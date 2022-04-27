package com.skype.slimcv.blurdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.List;

public final class AndroidCameraView extends JavaCameraView {
    private static final String     TAG = "AndroidCameraView";

    // Have to create personal member variables here, because OpenCV's are private
    private Bitmap mMyCacheBitmap;
    private CvCameraViewListener2 mMyListener;

    public AndroidCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public AndroidCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setCvCameraViewListener(CvCameraViewListener2 listener) {
        super.setCvCameraViewListener(listener);
        mMyListener = listener;
    }

    @Override
    public void disableView() {
        super.disableView();
        if (mMyCacheBitmap != null) {
            mMyCacheBitmap.recycle();
            mMyCacheBitmap = null;
        }
    }

    @Override
    protected Size calculateCameraFrameSize(List<?> supportedSizes, ListItemAccessor accessor, int surfaceWidth, int surfaceHeight) {
        return new Size(1280, 720);
    }

    @Override
    protected void deliverAndDrawFrame(CvCameraViewFrame frame) {
        Mat modified;

        if (mMyListener != null) {
            modified = mMyListener.onCameraFrame(frame);
        } else {
            modified = frame.rgba();
        }

        if (mMyCacheBitmap == null || mMyCacheBitmap.getWidth() != modified.width() || mMyCacheBitmap.getHeight() != modified.height())
        {
            if (mMyCacheBitmap != null)
                mMyCacheBitmap.recycle();
            mMyCacheBitmap = Bitmap.createBitmap(modified.width(), modified.height(), Bitmap.Config.ARGB_8888);
            mScale = Math.min((float)getWidth() / modified.width(), (float)getHeight() / modified.height());
        }

        boolean bmpValid = true;
        if (modified != null) {
            try {
                Utils.matToBitmap(modified, mMyCacheBitmap);
            } catch(Exception e) {
                Log.e(TAG, "Mat type: " + modified);
                Log.e(TAG, "Bitmap type: " + mMyCacheBitmap.getWidth() + "*" + mMyCacheBitmap.getHeight());
                Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
                bmpValid = false;
            }
        }

        if (bmpValid && mMyCacheBitmap != null) {
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

                canvas.drawBitmap(mMyCacheBitmap, new Rect(0,0,mMyCacheBitmap.getWidth(), mMyCacheBitmap.getHeight()),
                        new Rect((int)((canvas.getWidth() - mScale*mMyCacheBitmap.getWidth()) / 2),
                                (int)((canvas.getHeight() - mScale*mMyCacheBitmap.getHeight()) / 2),
                                (int)((canvas.getWidth() - mScale*mMyCacheBitmap.getWidth()) / 2 + mScale*mMyCacheBitmap.getWidth()),
                                (int)((canvas.getHeight() - mScale*mMyCacheBitmap.getHeight()) / 2 + mScale*mMyCacheBitmap.getHeight())), null);

                if (mFpsMeter != null) {
                    mFpsMeter.measure();
                    mFpsMeter.draw(canvas, 20, 30);
                }
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
}
