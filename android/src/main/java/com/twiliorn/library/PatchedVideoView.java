/**
 * Component for patching black screen bug coming from Twilio VideoView
 * Authors:
 * Aaron Alaniz (@aaalaniz) <aaron.a.alaniz@gmail.com>
 */

package com.twiliorn.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;

import com.twilio.video.VideoView;

import tvi.webrtc.EglRenderer;
import tvi.webrtc.VideoFrame;

/*
 * VideoView that notifies Listener of the first frame rendered and the first frame after a reset
 * request.
 */
public class PatchedVideoView extends VideoView {

    private boolean notifyFrameRendered = false;
    private Listener listener;
    private FrameListener frameListener;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public interface VideoViewCaptureListener {
        public void onBitmapReady(Bitmap bitMap);
    }

    private VideoViewCaptureListener videoViewCaptureListener;

    public PatchedVideoView(Context context) {
        super(context);
        this.videoViewCaptureListener = null;
    }

    public PatchedVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.videoViewCaptureListener = null;
    }

    @Override
    public void onFrame(VideoFrame frame) {
        if (notifyFrameRendered) {
            notifyFrameRendered = false;
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onFirstFrame();
                }
            });
        }
        super.onFrame(frame);
    }

    /*
     * Set your listener
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setVideoViewCaptureListener(VideoViewCaptureListener listener) {
        this.videoViewCaptureListener = listener;
    }

    /*
     * Reset the listener so next frame rendered results in callback
     */
    public void resetListener() {
        notifyFrameRendered = true;
    }

    public interface Listener {
        void onFirstFrame();
    }

    public void takeScreenShot() {
        frameListener = new FrameListener();
        this.addFrameListener(frameListener, 1);
    }

    // ===== VideoView.Listener Implementation =====================================================

    class FrameListener implements EglRenderer.FrameListener {

        @Override
        public void onFrame(Bitmap bitmap) {
            if (bitmap != null) {
                resetFrameListener();
                videoViewCaptureListener.onBitmapReady(bitmap);
            }
        }
    }

    private void resetFrameListener() {
        if (frameListener == null) {
            return;
        } else {
            Runnable removeListenerRunnable = new Runnable() {
                @Override
                public void run() {
                    removeFrameListener(frameListener);
                    frameListener = null;
                }
            };
            new Thread(removeListenerRunnable).start();
        }
    }
}
