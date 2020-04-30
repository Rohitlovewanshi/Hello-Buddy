package com.rohit.hellobuddy;


import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ebanx.swipebtn.OnStateChangeListener;
import com.ebanx.swipebtn.SwipeButton;
import com.sinch.android.rtc.AudioController;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallState;
import com.sinch.android.rtc.video.VideoCallListener;
import com.sinch.android.rtc.video.VideoController;

import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class VideoCallScreenActivity extends BaseActivity {

    static final String TAG = VideoCallScreenActivity.class.getSimpleName();
    static final String ADDED_LISTENER = "addedListener";
    static final String VIEWS_TOGGLED = "viewsToggled";

    private AudioPlayer mAudioPlayer;

    private String mCallId;
    private boolean mAddedListener = false;
    private boolean mLocalVideoViewAdded = false;
    private boolean mRemoteVideoViewAdded = false;

    ImageView endCallButton;

    boolean mToggleVideoViewPositions = false;


    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(ADDED_LISTENER, mAddedListener);
        savedInstanceState.putBoolean(VIEWS_TOGGLED, mToggleVideoViewPositions);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mAddedListener = savedInstanceState.getBoolean(ADDED_LISTENER);
        mToggleVideoViewPositions = savedInstanceState.getBoolean(VIEWS_TOGGLED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call_screen);

        mAudioPlayer = new AudioPlayer(this);

        endCallButton=findViewById(R.id.hangupButton);

        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });

        mCallId = getIntent().getStringExtra(SinchService.CALL_ID);
    }

    @Override
    public void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            if (!mAddedListener) {
                call.addCallListener(new SinchCallListener());
                mAddedListener = true;
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }

        updateUI();
    }

    private void updateUI() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }

        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            if (call.getDetails().isVideoOffered()) {
                if (call.getState() == CallState.ESTABLISHED) {
                    setVideoViewsVisibility(true, true);
                } else {
                    setVideoViewsVisibility(true, false);
                }
            }
        } else {
            setVideoViewsVisibility(false, false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        removeVideoViews();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUI();
    }

    @Override
    public void onBackPressed() {
        // User should exit activity by ending call, not by going back.
    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();
    }

    private ViewGroup getVideoView(boolean localView) {
        if (mToggleVideoViewPositions) {
            localView = !localView;
        }
        return (ViewGroup) (localView ? findViewById(R.id.localVideo) : findViewById(R.id.remoteVideo));
    }

    private void addLocalView() {
        if (mLocalVideoViewAdded || getSinchServiceInterface() == null) {
            return; //early
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(() -> {
                ViewGroup localView = getVideoView(true);
                localView.addView(vc.getLocalView());
                localView.setOnClickListener(v -> vc.toggleCaptureDevicePosition());
                mLocalVideoViewAdded = true;
                vc.setLocalVideoZOrder(!mToggleVideoViewPositions);
            });
        }
    }
    private void addRemoteView() {
        if (mRemoteVideoViewAdded || getSinchServiceInterface() == null) {
            return; //early
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(() -> {
                ViewGroup remoteView = getVideoView(false);
                remoteView.addView(vc.getRemoteView());
                remoteView.setOnClickListener((View v) -> {
                    removeVideoViews();
                    mToggleVideoViewPositions = !mToggleVideoViewPositions;
                    addRemoteView();
                    addLocalView();
                });
                mRemoteVideoViewAdded = true;
                vc.setLocalVideoZOrder(!mToggleVideoViewPositions);
            });
        }
    }


    private void removeVideoViews() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }

        VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(() -> {
                ((ViewGroup)(vc.getRemoteView().getParent())).removeView(vc.getRemoteView());
                ((ViewGroup)(vc.getLocalView().getParent())).removeView(vc.getLocalView());
                mLocalVideoViewAdded = false;
                mRemoteVideoViewAdded = false;
            });
        }
    }


    private void setVideoViewsVisibility(final boolean localVideoVisibile, final boolean remoteVideoVisible) {
        if (getSinchServiceInterface() == null)
            return;
        if (!mRemoteVideoViewAdded) {
            addRemoteView();
        }
        if (!mLocalVideoViewAdded) {
            addLocalView();
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(() -> {
                vc.getLocalView().setVisibility(localVideoVisibile ? View.VISIBLE : View.GONE);
                vc.getRemoteView().setVisibility(remoteVideoVisible ? View.VISIBLE : View.GONE);
            });
        }
    }

    private class SinchCallListener implements VideoCallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended. Reason: " + cause.toString());
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            String endMsg = "Call ended: " + call.getDetails().toString();
            Toast.makeText(VideoCallScreenActivity.this, endMsg, Toast.LENGTH_LONG).show();

            endCall();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            AudioController audioController = getSinchServiceInterface().getAudioController();
            audioController.enableSpeaker();
            if (call.getDetails().isVideoOffered()) {
                setVideoViewsVisibility(true, true);
            }
            Log.d(TAG, "Call offered video: " + call.getDetails().isVideoOffered());
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
            mAudioPlayer.playProgressTone();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // No need to implement if you use managed push
        }

        @Override
        public void onVideoTrackAdded(Call call) {

        }

        @Override
        public void onVideoTrackPaused(Call call) {

        }

        @Override
        public void onVideoTrackResumed(Call call) {

        }
    }
}
