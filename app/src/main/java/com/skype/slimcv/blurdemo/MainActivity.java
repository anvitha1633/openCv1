package com.skype.slimcv.blurdemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String     TAG = "BlurDemo";

    private Display                 mDisplay;
    private Mat                     mRgba;
    private Mat                     mRgbaT;
    private AndroidCameraView       mOpenCvCameraView;
    static private int              mCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        Bundle extras = getIntent().getExtras();
        String args = "";
        if(extras != null && extras.getString("args") != null)
        {
            args = extras.getString("args");
            Log.v(TAG, "Input arguments: " + args);
        }

        int result = nativeMain(args);
        Log.v(TAG, "Native blurdemo initialization returned " + result);

        mOpenCvCameraView = findViewById(R.id.surface_view);
        mOpenCvCameraView.setCameraIndex(mCameraIndex);
        try {
            // method not available in OpenCV 3
            // mOpenCvCameraView.setCameraPermissionGranted();
            mOpenCvCameraView.getClass().getMethod("setCameraPermissionGranted", null).invoke(mOpenCvCameraView, null);
        } catch (Exception e) {
        }
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mDisplay = ((WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mRgbaT.release();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "called onTouch");
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mCameraIndex = mCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK
                    ? CameraBridgeViewBase.CAMERA_ID_FRONT : CameraBridgeViewBase.CAMERA_ID_BACK;
            mOpenCvCameraView.setCameraIndex(mCameraIndex);
            mOpenCvCameraView.enableView();
        }
        return false; // don't need subsequent touch events
    }

    private Mat rotateFrame(Mat m, int rotation) {
        // Handle orientation
        if(mCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK) { // back-facing camera
            switch(rotation) {
                case Surface.ROTATION_0:
                default:
                    Core.flip(m, m, -1);
                    Core.transpose(m, mRgbaT);
                    return mRgbaT;
                case Surface.ROTATION_90:
                    Core.flip(m, m, 1);
                    return m;
                case Surface.ROTATION_180:
                    Core.transpose(m, mRgbaT);
                    return mRgbaT;
                case Surface.ROTATION_270:
                    Core.flip(m, m, 0);
                    return m;
            }
        } else {    // front-facing camera
            switch(rotation) {
                case Surface.ROTATION_0:
                default:
                    Core.flip(m, m, 0);
                    Core.transpose(m, mRgbaT);
                    return mRgbaT;
                case Surface.ROTATION_90:
                    return m;
                case Surface.ROTATION_180:
                    Core.flip(m, m, 1);
                    Core.transpose(m, mRgbaT);
                    return mRgbaT;
                case Surface.ROTATION_270:
                    Core.flip(m, m, -1);
                    return m;
            }
        }
    }

    private int gravity(int rotation) {
        switch(rotation) {
            case Surface.ROTATION_0:
            default:
                return 3;   // RIGHT
            case Surface.ROTATION_90:
                return 0;   // DOWN
            case Surface.ROTATION_180:
                return 1;   // LEFT
            case Surface.ROTATION_270:
                return 2;   // UP
        }
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        final int rotation = (mDisplay == null) ? 0 : mDisplay.getRotation();

        // Do background blur through JNI
        int r = processFrame(mRgba.getNativeObjAddr(), gravity(rotation));
        if (r != 0)
            Log.v(TAG, "Frame processing failed with code " + r);

        Mat modified = rotateFrame(mRgba, rotation);

        return modified;
    }

    private native int nativeMain(String args);
    private native int processFrame(long pMat, int gravity);

    static void loadPackagedLibrary(String name)
    {
        try {
            System.loadLibrary(name);
            Log.v(TAG, "Loaded " + name);
        } catch (UnsatisfiedLinkError e) {
            Log.v(TAG, "Load library failed " + name);
        }
    }

    static {
        // Explictly loading all native libraries to avoid any corner cases
        loadPackagedLibrary("gnustl_shared");
        loadPackagedLibrary("c++_shared");
        loadPackagedLibrary("skypert");
        loadPackagedLibrary("opencv_java3");
        loadPackagedLibrary("opencv_java4");
        loadPackagedLibrary("blurdemo");
    }
}
