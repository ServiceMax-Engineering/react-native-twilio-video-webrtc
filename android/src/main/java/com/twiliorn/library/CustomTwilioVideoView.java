/**
 * Component to orchestrate the Twilio Video connection and the various video
 * views.
 * <p>
 * Authors:
 * Ralph Pina <ralph.pina@gmail.com>
 * Jonathan Chang <slycoder@gmail.com>
 */
package com.twiliorn.library;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.annotation.StringDef;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.twilio.video.AudioTrackPublication;
import com.twilio.video.BaseTrackStats;
import com.twilio.video.CameraCapturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalAudioTrackPublication;
import com.twilio.video.LocalAudioTrackStats;
import com.twilio.video.LocalDataTrackPublication;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalTrackStats;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.LocalVideoTrackPublication;
import com.twilio.video.LocalVideoTrackStats;
import com.twilio.video.NetworkQualityConfiguration;
import com.twilio.video.NetworkQualityLevel;
import com.twilio.video.NetworkQualityVerbosity;
import com.twilio.video.Participant;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteAudioTrackStats;
import com.twilio.video.LocalDataTrack;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteTrackStats;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.RemoteVideoTrackStats;
import com.twilio.video.Room;
import com.twilio.video.Room.State;
import com.twilio.video.StatsListener;
import com.twilio.video.StatsReport;
import com.twilio.video.TrackPublication;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoDimensions;
import com.twilio.video.VideoFormat;
import com.twilio.video.VideoCodec;

import tvi.webrtc.voiceengine.WebRtcAudioManager;

import tvi.webrtc.Camera1Enumerator;
import tvi.webrtc.HardwareVideoEncoderFactory;
import tvi.webrtc.HardwareVideoDecoderFactory;
import tvi.webrtc.VideoCodecInfo;
import com.twilio.video.H264Codec;
import com.twilio.video.Vp8Codec;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;

import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_AUDIO_CHANGED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_CAMERA_SWITCHED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_CONNECTED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_CONNECT_FAILURE;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_DISCONNECTED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_DATATRACK_MESSAGE_RECEIVED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_NETWORK_QUALITY_LEVELS_CHANGED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_ADDED_DATA_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_ADDED_AUDIO_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_ADDED_VIDEO_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_CONNECTED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_DISABLED_AUDIO_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_DISABLED_VIDEO_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_DISCONNECTED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_ENABLED_AUDIO_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_ENABLED_VIDEO_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_REMOVED_DATA_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_REMOVED_AUDIO_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_PARTICIPANT_REMOVED_VIDEO_TRACK;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_STATS_RECEIVED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_VIDEO_CHANGED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_DOMINANT_SPEAKER_CHANGED;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_LOCAL_PARTICIPANT_SUPPORTED_CODECS;
import static com.twiliorn.library.CustomTwilioVideoView.Events.ON_VIDEO_FRAME_CAPTURED;

class StoreData {
  public static boolean enableRemoteAudio = false;
  public static boolean enableNetworkQualityReporting = false;
  public static boolean isVideoEnabled = false;
  public static boolean dominantSpeakerEnabled = false;
  public static boolean maintainVideoTrackInBackground = false;
  public static String cameraType = "";
  public static boolean enableH264Codec = false;
  public static AudioFocusRequest audioFocusRequest;

  /*
   * A Room represents communication between the client and one or more participants.
   */
  public static Room room;
  public static String roomName = null;
  public static String accessToken = null;
  public static LocalParticipant localParticipant;

  public static LocalVideoTrack localVideoTrack;
  public static LocalDataTrack localDataTrack;

  public static CameraCapturer cameraCapturer;
  public static LocalAudioTrack localAudioTrack;
  /*
   * A VideoView receives frames from a local or remote video track and renders them
   * to an associated view.
   */
  public static PatchedVideoView thumbnailVideoView;
  public static PatchedVideoView remoteThumbnailVideoView;
  public static AudioManager audioManager;
  public static int previousAudioMode;
  public static boolean disconnectedFromOnDestroy;

  // Map used to map remote data tracks to remote participants
  public static final Map<RemoteDataTrack, RemoteParticipant> dataTrackRemoteParticipantMap =
    new HashMap<>();
}

public class CustomTwilioVideoView extends View implements LifecycleEventListener, AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "CustomTwilioVideoView";
    private static final String DATA_TRACK_MESSAGE_THREAD_NAME = "DataTrackMessages";
    private static final String FRONT_CAMERA_TYPE = "front";
    private static final String BACK_CAMERA_TYPE = "back";
    private static String frontFacingDevice;
    private static String backFacingDevice;
    private boolean maintainVideoTrackInBackground = false;
    private String cameraType = "";

    private String currentDeviceOrientation = "UNKNOWN";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({Events.ON_CAMERA_SWITCHED,
            Events.ON_VIDEO_CHANGED,
            Events.ON_AUDIO_CHANGED,
            Events.ON_CONNECTED,
            Events.ON_CONNECT_FAILURE,
            Events.ON_DISCONNECTED,
            Events.ON_PARTICIPANT_CONNECTED,
            Events.ON_PARTICIPANT_DISCONNECTED,
            Events.ON_PARTICIPANT_ADDED_VIDEO_TRACK,
            Events.ON_DATATRACK_MESSAGE_RECEIVED,
            Events.ON_PARTICIPANT_ADDED_DATA_TRACK,
            Events.ON_PARTICIPANT_REMOVED_DATA_TRACK,
            Events.ON_PARTICIPANT_REMOVED_VIDEO_TRACK,
            Events.ON_PARTICIPANT_ADDED_AUDIO_TRACK,
            Events.ON_PARTICIPANT_REMOVED_AUDIO_TRACK,
            Events.ON_PARTICIPANT_ENABLED_VIDEO_TRACK,
            Events.ON_PARTICIPANT_DISABLED_VIDEO_TRACK,
            Events.ON_PARTICIPANT_ENABLED_AUDIO_TRACK,
            Events.ON_PARTICIPANT_DISABLED_AUDIO_TRACK,
            Events.ON_STATS_RECEIVED,
            Events.ON_NETWORK_QUALITY_LEVELS_CHANGED,
            Events.ON_DOMINANT_SPEAKER_CHANGED,
            Events.ON_LOCAL_PARTICIPANT_SUPPORTED_CODECS,
            Events.ON_VIDEO_FRAME_CAPTURED,
    })
    public @interface Events {
        String ON_CAMERA_SWITCHED = "onCameraSwitched";
        String ON_VIDEO_CHANGED = "onVideoChanged";
        String ON_AUDIO_CHANGED = "onAudioChanged";
        String ON_CONNECTED = "onRoomDidConnect";
        String ON_CONNECT_FAILURE = "onRoomDidFailToConnect";
        String ON_DISCONNECTED = "onRoomDidDisconnect";
        String ON_PARTICIPANT_CONNECTED = "onRoomParticipantDidConnect";
        String ON_PARTICIPANT_DISCONNECTED = "onRoomParticipantDidDisconnect";
        String ON_DATATRACK_MESSAGE_RECEIVED = "onDataTrackMessageReceived";
        String ON_PARTICIPANT_ADDED_DATA_TRACK = "onParticipantAddedDataTrack";
        String ON_PARTICIPANT_REMOVED_DATA_TRACK = "onParticipantRemovedDataTrack";
        String ON_PARTICIPANT_ADDED_VIDEO_TRACK = "onParticipantAddedVideoTrack";
        String ON_PARTICIPANT_REMOVED_VIDEO_TRACK = "onParticipantRemovedVideoTrack";
        String ON_PARTICIPANT_ADDED_AUDIO_TRACK = "onParticipantAddedAudioTrack";
        String ON_PARTICIPANT_REMOVED_AUDIO_TRACK = "onParticipantRemovedAudioTrack";
        String ON_PARTICIPANT_ENABLED_VIDEO_TRACK = "onParticipantEnabledVideoTrack";
        String ON_PARTICIPANT_DISABLED_VIDEO_TRACK = "onParticipantDisabledVideoTrack";
        String ON_PARTICIPANT_ENABLED_AUDIO_TRACK = "onParticipantEnabledAudioTrack";
        String ON_PARTICIPANT_DISABLED_AUDIO_TRACK = "onParticipantDisabledAudioTrack";
        String ON_STATS_RECEIVED = "onStatsReceived";
        String ON_NETWORK_QUALITY_LEVELS_CHANGED = "onNetworkQualityLevelsChanged";
        String ON_DOMINANT_SPEAKER_CHANGED = "onDominantSpeakerDidChange";
        String ON_LOCAL_PARTICIPANT_SUPPORTED_CODECS = "onLocalParticipantSupportedCodecs";
        String ON_VIDEO_FRAME_CAPTURED = "onVideoFrameCaptured";
    }

    private final ThemedReactContext themedReactContext;
    private final RCTEventEmitter eventEmitter;

    private AudioAttributes playbackAttributes;
    private Handler handler = new Handler();

    private IntentFilter intentFilter;
    private BecomingNoisyReceiver myNoisyAudioStreamReceiver;

    // Dedicated thread and handler for messages received from a RemoteDataTrack
    private final HandlerThread dataTrackMessageThread =
            new HandlerThread(DATA_TRACK_MESSAGE_THREAD_NAME);
    private Handler dataTrackMessageThreadHandler;


    public CustomTwilioVideoView(ThemedReactContext context) {
        super(context);
        this.themedReactContext = context;
        Log.d("CustomTwilioVideoView", "constructor ->" + StoreData.roomName);

        this.eventEmitter = themedReactContext.getJSModule(RCTEventEmitter.class);

        // add lifecycle for onResume and on onPause
        themedReactContext.addLifecycleEventListener(this);

        /*
         * Needed for setting/abandoning audio focus during call
         */
        StoreData.audioManager = (AudioManager) themedReactContext.getSystemService(Context.AUDIO_SERVICE);
        myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
        intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

        // Create the local data track
        // localDataTrack = LocalDataTrack.create(this);
        StoreData.localDataTrack = LocalDataTrack.create(getContext());


        // Start the thread where data messages are received
        dataTrackMessageThread.start();
        dataTrackMessageThreadHandler = new Handler(dataTrackMessageThread.getLooper());

    }

    // ===== SETUP =================================================================================

    private VideoFormat buildVideoFormat() {
        return new VideoFormat(VideoDimensions.VGA_VIDEO_DIMENSIONS, 15);
    }

    private CameraCapturer createCameraCaputer(Context context, String cameraId) {
        CameraCapturer newCameraCapturer = null;
        try {
            newCameraCapturer = new CameraCapturer(
                    context,
                    cameraId,
                    new CameraCapturer.Listener() {
                        @Override
                        public void onFirstFrameAvailable() {
                        }

                        @Override
                        public void onCameraSwitched(String newCameraId) {
                            setThumbnailMirror();
                            WritableMap event = new WritableNativeMap();
                            if (isCurrentCameraSourceBackFacing()) {
                                event.putString("cameraType",CustomTwilioVideoView.BACK_CAMERA_TYPE);
                            } else {
                                event.putString("cameraType",CustomTwilioVideoView.FRONT_CAMERA_TYPE);
                            }
                            pushEvent(CustomTwilioVideoView.this, ON_CAMERA_SWITCHED, event);
                        }

                        @Override
                        public void onError(int i) {
                            Log.i("CustomTwilioVideoView", "Error getting camera");
                        }
                    }
            );
            return newCameraCapturer;
        } catch (Exception e) {
            return null;
        }
    }

    private void buildDeviceInfo() {
        Camera1Enumerator enumerator = new Camera1Enumerator();
        String[] deviceNames = enumerator.getDeviceNames();
        backFacingDevice = null;
        frontFacingDevice = null;
        for (String deviceName : deviceNames) {
            if (enumerator.isBackFacing(deviceName) && enumerator.getSupportedFormats(deviceName).size() > 0) {
                backFacingDevice = deviceName;
            } else if (enumerator.isFrontFacing(deviceName) && enumerator.getSupportedFormats(deviceName).size() > 0) {
                frontFacingDevice = deviceName;
            }
        }
    }

    private boolean createLocalVideo(boolean enableVideo, String cameraType) {
        StoreData.isVideoEnabled = enableVideo;
        // Share your camera
        buildDeviceInfo();

        if (cameraType.equals(CustomTwilioVideoView.FRONT_CAMERA_TYPE)) {
          if (frontFacingDevice != null) {
            StoreData.cameraCapturer = this.createCameraCaputer(getContext(), frontFacingDevice);
          } else {
            // IF the camera is unavailable try the other camera
            StoreData.cameraCapturer = this.createCameraCaputer(getContext(), backFacingDevice);
          }
        } else {
          if (backFacingDevice != null) {
            StoreData.cameraCapturer = this.createCameraCaputer(getContext(), backFacingDevice);
          } else {
            // IF the camera is unavailable try the other camera
            StoreData.cameraCapturer = this.createCameraCaputer(getContext(), frontFacingDevice);
          }
        }

        // If no camera is available let the caller know
        if (StoreData.cameraCapturer == null) {
          WritableMap event = new WritableNativeMap();
          event.putString("error", "No camera is supported on this device");
          pushEvent(CustomTwilioVideoView.this, ON_CONNECT_FAILURE, event);
          return false;
        }

        StoreData.localVideoTrack = LocalVideoTrack.create(getContext(), enableVideo, StoreData.cameraCapturer, buildVideoFormat());

        if (StoreData.thumbnailVideoView != null && StoreData.localVideoTrack != null) {
            StoreData.localVideoTrack.addSink(StoreData.thumbnailVideoView);
        }
        setThumbnailMirror();
        return true;
    }

    // ===== LIFECYCLE EVENTS ======================================================================


    @Override
    public void onHostResume() {
        /*
         * In case it wasn't set.
         */
        if (themedReactContext.getCurrentActivity() != null) {
            /*
             * If the local video track was released when the app was put in the background, recreate.
             */
            if (StoreData.cameraCapturer != null && StoreData.localVideoTrack == null) {
              StoreData.localVideoTrack = LocalVideoTrack.create(getContext(), StoreData.isVideoEnabled, StoreData.cameraCapturer, buildVideoFormat());
            }

            if (StoreData.localVideoTrack != null) {
                if (StoreData.thumbnailVideoView != null) {
                  StoreData.localVideoTrack.addSink(StoreData.thumbnailVideoView);
                }

                /*
                 * If connected to a Room then share the local video track.
                 */
                if (StoreData.localParticipant != null) {
                  StoreData.localParticipant.publishTrack(StoreData.localVideoTrack);
                }
            }

            if (StoreData.room != null) {
                themedReactContext.getCurrentActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            }

            this.specificOrientationListener.enable();
        }
    }

    @Override
    public void onHostPause() {
        /*
         * Release the local video track before going in the background. This ensures that the
         * camera can be used by other applications while this app is in the background.
         */
        if (StoreData.localVideoTrack != null && !maintainVideoTrackInBackground) {
            /*
             * If this local video track is being shared in a Room, remove from local
             * participant before releasing the video track. Participants will be notified that
             * the track has been removed.
             */
            if (StoreData.localParticipant != null) {
              StoreData.localParticipant.unpublishTrack(StoreData.localVideoTrack);
            }

          StoreData.localVideoTrack.release();
          StoreData.localVideoTrack = null;
        }

        this.specificOrientationListener.disable();
    }

    @Override
    public void onHostDestroy() {
        /*
         * Remove stream voice control
         */
        if (themedReactContext.getCurrentActivity() != null) {
            themedReactContext.getCurrentActivity().setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        }
        /*
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        if (StoreData.room != null && StoreData.room.getState() != Room.State.DISCONNECTED) {
            StoreData.room.disconnect();
            StoreData.disconnectedFromOnDestroy = true;
        }

        /*
         * Release the local media ensuring any memory allocated to audio or video is freed.
         */
        if (StoreData.localVideoTrack != null) {
            StoreData.localVideoTrack.release();
            StoreData.localVideoTrack = null;
        }

        if (StoreData.localAudioTrack != null) {
            StoreData.localAudioTrack.release();
            StoreData.audioManager.stopBluetoothSco();
            StoreData.localAudioTrack = null;
        }

        // Quit the data track message thread
        dataTrackMessageThread.quit();

        this.specificOrientationListener.disable();
    }

    public void releaseResource() {
        themedReactContext.removeLifecycleEventListener(this);
        StoreData.room = null;
        StoreData.localVideoTrack = null;
        StoreData.thumbnailVideoView = null;
        StoreData.cameraCapturer = null;
    }

    // ====== CONNECTING ===========================================================================

    public void connectToRoomWrapper(
            String roomName,
            String accessToken,
            boolean enableAudio,
            boolean enableVideo,
            boolean enableRemoteAudio,
            boolean enableNetworkQualityReporting,
            boolean dominantSpeakerEnabled,
            boolean maintainVideoTrackInBackground,
            String cameraType,
            boolean enableH264Codec
    ) {
        StoreData.roomName = roomName;
        StoreData.accessToken = accessToken;
        StoreData.enableRemoteAudio = enableRemoteAudio;
        StoreData.enableNetworkQualityReporting = enableNetworkQualityReporting;
        StoreData.dominantSpeakerEnabled = dominantSpeakerEnabled;
        StoreData.maintainVideoTrackInBackground = maintainVideoTrackInBackground;
        StoreData.cameraType = cameraType;
        StoreData.enableH264Codec = enableH264Codec;

        // Share your microphone
        StoreData.localAudioTrack = LocalAudioTrack.create(getContext(), enableAudio);

        if (StoreData.cameraCapturer == null && enableVideo) {
            boolean createVideoStatus = createLocalVideo(enableVideo, cameraType);
            if (!createVideoStatus) {
                Log.d("RNTwilioVideo", "Failed to create local video");
                // No need to connect to room if video creation failed
                return;
            }
        } else {
          StoreData.isVideoEnabled = false;
        }

        setAudioFocus(enableAudio);
        connectToRoom();
    }

    public void connectToRoom() {
        /*
         * Create a VideoClient allowing you to connect to a Room
         */
        ConnectOptions.Builder connectOptionsBuilder = new ConnectOptions.Builder(StoreData.accessToken);
        Log.d("CustomTwilioVideoView", "constructor --> connectToRoom");
        if (StoreData.roomName != null) {
            connectOptionsBuilder.roomName(StoreData.roomName);
        }

        if (StoreData.localAudioTrack != null) {
            connectOptionsBuilder.audioTracks(Collections.singletonList(StoreData.localAudioTrack));
        }

        if (StoreData.localVideoTrack != null) {
            connectOptionsBuilder.videoTracks(Collections.singletonList(StoreData.localVideoTrack));
        }

        //LocalDataTrack localDataTrack = LocalDataTrack.create(getContext());

        if (StoreData.localDataTrack != null) {
            connectOptionsBuilder.dataTracks(Collections.singletonList(StoreData.localDataTrack));
        }

        // H264 Codec Support Detection: https://www.twilio.com/docs/video/managing-codecs
        HardwareVideoEncoderFactory hardwareVideoEncoderFactory = new HardwareVideoEncoderFactory(null, true, true);
        HardwareVideoDecoderFactory hardwareVideoDecoderFactory = new HardwareVideoDecoderFactory(null);

        boolean h264EncoderSupported = false;
        for (VideoCodecInfo videoCodecInfo : hardwareVideoEncoderFactory.getSupportedCodecs()) {
            if (videoCodecInfo.name.equalsIgnoreCase("h264")) {
                h264EncoderSupported = true;
                break;
            }
        }
        boolean h264DecoderSupported = false;
        for (VideoCodecInfo videoCodecInfo : hardwareVideoDecoderFactory.getSupportedCodecs()) {
            if (videoCodecInfo.name.equalsIgnoreCase("h264")) {
                h264DecoderSupported = true;
                break;
            }
        }

        boolean isH264Supported = h264EncoderSupported && h264DecoderSupported;

        Log.d("RNTwilioVideo", "H264 supported by hardware: " + isH264Supported);

        WritableArray supportedCodecs = new WritableNativeArray();

        VideoCodec videoCodec =  new Vp8Codec();
        // VP8 is supported on all android devices by default
        supportedCodecs.pushString(videoCodec.toString());

        if (isH264Supported && StoreData.enableH264Codec) {
            videoCodec = new H264Codec();
            supportedCodecs.pushString(videoCodec.toString());
        }

        WritableMap event = new WritableNativeMap();

        event.putArray("supportedCodecs", supportedCodecs);

        pushEvent(CustomTwilioVideoView.this, ON_LOCAL_PARTICIPANT_SUPPORTED_CODECS, event);

        connectOptionsBuilder.preferVideoCodecs(Collections.singletonList(videoCodec));

        connectOptionsBuilder.enableDominantSpeaker(StoreData.dominantSpeakerEnabled);

        if (StoreData.enableNetworkQualityReporting) {
            connectOptionsBuilder.enableNetworkQuality(true);
            connectOptionsBuilder.networkQualityConfiguration(new NetworkQualityConfiguration(
                    NetworkQualityVerbosity.NETWORK_QUALITY_VERBOSITY_MINIMAL,
                    NetworkQualityVerbosity.NETWORK_QUALITY_VERBOSITY_MINIMAL));
        }

        StoreData.room = Video.connect(getContext(), connectOptionsBuilder.build(), roomListener());
    }

    public void setAudioType() {
        AudioDeviceInfo[] devicesInfo = StoreData.audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        boolean hasNonSpeakerphoneDevice = false;
        for (int i = 0; i < devicesInfo.length; i++) {
            int deviceType = devicesInfo[i].getType();
            if (
                deviceType == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                deviceType == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            ) {
                hasNonSpeakerphoneDevice = true;
            }
            if (
                deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            ) {
              StoreData.audioManager.startBluetoothSco();
              StoreData.audioManager.setBluetoothScoOn(true);
              hasNonSpeakerphoneDevice = true;
            }
        }
        StoreData.audioManager.setSpeakerphoneOn(!hasNonSpeakerphoneDevice);
    }

    private void setAudioFocus(boolean focus) {
        if (focus) {
            StoreData.previousAudioMode = StoreData.audioManager.getMode();
            // Request audio focus before making any device switch.
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                StoreData.audioManager.requestAudioFocus(this,
                  AudioManager.STREAM_VOICE_CALL,
                  AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            } else {
                playbackAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
              StoreData.audioFocusRequest = new AudioFocusRequest
                        .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(this, handler)
                        .build();
              StoreData.audioManager.requestAudioFocus(StoreData.audioFocusRequest);
            }
            /*
             * Use MODE_IN_COMMUNICATION as the default audio mode. It is required
             * to be in this mode when playout and/or recording starts for the best
             * possible VoIP performance. Some devices have difficulties with
             * speaker mode if this is not set.
             */
            StoreData.audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            setAudioType();
            getContext().registerReceiver(myNoisyAudioStreamReceiver, intentFilter);

        } else {
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
               StoreData.audioManager.abandonAudioFocus(this);
            } else if (StoreData.audioFocusRequest != null) {
               StoreData.audioManager.abandonAudioFocusRequest(StoreData.audioFocusRequest);
            }

            StoreData.audioManager.setSpeakerphoneOn(false);
            StoreData.audioManager.setMode(StoreData.previousAudioMode);
            try {
                if (myNoisyAudioStreamReceiver != null) {
                    getContext().unregisterReceiver(myNoisyAudioStreamReceiver);
                }
                myNoisyAudioStreamReceiver = null;
            } catch (Exception e) {
                // already registered
                e.printStackTrace();
            }
        }
    }

    private class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            StoreData.audioManager.setSpeakerphoneOn(true);
            if (Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                setAudioType();
            }
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.e(TAG, "onAudioFocusChange: focuschange: " + focusChange);
    }

    // ====== DISCONNECTING ========================================================================

    public void disconnect() {
        if (StoreData.room != null) {
          StoreData.room.disconnect();
        }
        if (StoreData.localAudioTrack != null) {
          StoreData.localAudioTrack.release();
          StoreData.localAudioTrack = null;
          StoreData.audioManager.stopBluetoothSco();
        }
        if (StoreData.localVideoTrack != null) {
          StoreData.localVideoTrack.release();
          StoreData.localVideoTrack = null;
          StoreData.audioManager.stopBluetoothSco();
        }
        setAudioFocus(false);
        if (StoreData.cameraCapturer != null) {
          StoreData.cameraCapturer.stopCapture();
          StoreData.cameraCapturer = null;
        }
    }

    // ===== SEND STRING ON DATA TRACK ======================================================================
    public void sendString(String message) {
        if (StoreData.localDataTrack != null) {
          StoreData.localDataTrack.send(message);
        }
    }

    private static boolean isCurrentCameraSourceBackFacing() {
        return StoreData.cameraCapturer != null && StoreData.cameraCapturer.getCameraId() == backFacingDevice;
    }

    // ===== BUTTON LISTENERS ======================================================================
    private static void setThumbnailMirror() {
        if (StoreData.cameraCapturer != null) {
            final boolean isBackCamera = isCurrentCameraSourceBackFacing();
            if (StoreData.thumbnailVideoView != null && StoreData.thumbnailVideoView.getVisibility() == View.VISIBLE) {
              StoreData.thumbnailVideoView.setMirror(!isBackCamera);
            }
        }
    }

    public void captureVideoFrame() {
        PatchedVideoView videoView = StoreData.isVideoEnabled ? StoreData.thumbnailVideoView : StoreData.remoteThumbnailVideoView;
        if (videoView != null) {
            videoView.setVideoViewCaptureListener(new PatchedVideoView.VideoViewCaptureListener() {
                @Override
                public void onBitmapReady(Bitmap bitMap) {
                    String path = BitmapImageHelper.saveImage(bitMap, getContext(), currentDeviceOrientation);
                    WritableMap event = new WritableNativeMap();
                    event.putString("path", path);
                    pushEvent(CustomTwilioVideoView.this, ON_VIDEO_FRAME_CAPTURED, event);
                }
            });

            Context context = getContext();
            videoView.takeScreenShot();
        }
    }

    public void switchCamera() {
        if (StoreData.cameraCapturer != null) {
            final boolean isBackCamera = isCurrentCameraSourceBackFacing();
            if (frontFacingDevice != null && (isBackCamera || backFacingDevice == null)) {
              StoreData.cameraCapturer.switchCamera(frontFacingDevice);
                cameraType = CustomTwilioVideoView.FRONT_CAMERA_TYPE;
            } else {
              StoreData.cameraCapturer.switchCamera(backFacingDevice);
                cameraType = CustomTwilioVideoView.BACK_CAMERA_TYPE;
            }
        }
    }

    public void toggleVideo(boolean enabled) {
        StoreData.isVideoEnabled = enabled;

        if (StoreData.cameraCapturer == null && enabled) {
            String fallbackCameraType = CustomTwilioVideoView.FRONT_CAMERA_TYPE;
            boolean createVideoStatus = createLocalVideo(true, fallbackCameraType);
            if (!createVideoStatus) {
                Log.d("RNTwilioVideo", "Failed to create local video");
                return;
            }
        }

        if (StoreData.localVideoTrack != null) {
            StoreData.localVideoTrack.enable(enabled);
            publishLocalVideo(enabled);

            WritableMap event = new WritableNativeMap();
            event.putBoolean("videoEnabled", enabled);
            pushEvent(CustomTwilioVideoView.this, ON_VIDEO_CHANGED, event);
        }
    }

    public void toggleSoundSetup(boolean speaker) {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (speaker) {
            audioManager.setSpeakerphoneOn(true);
        } else {
            audioManager.setSpeakerphoneOn(false);
        }
    }

    public void toggleAudio(boolean enabled) {
        if (StoreData.localAudioTrack != null) {
            StoreData.localAudioTrack.enable(enabled);

            WritableMap event = new WritableNativeMap();
            event.putBoolean("audioEnabled", enabled);
            pushEvent(CustomTwilioVideoView.this, ON_AUDIO_CHANGED, event);
        }
    }

    public void toggleBluetoothHeadset(boolean enabled) {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (enabled) {
            audioManager.startBluetoothSco();
            audioManager.setSpeakerphoneOn(false);
        } else {
            audioManager.stopBluetoothSco();
            audioManager.setSpeakerphoneOn(true);
        }
    }

    public void toggleRemoteAudio(boolean enabled) {
        if (StoreData.room != null) {
            for (RemoteParticipant rp : StoreData.room.getRemoteParticipants()) {
                for (AudioTrackPublication at : rp.getAudioTracks()) {
                    if (at.getAudioTrack() != null) {
                        ((RemoteAudioTrack) at.getAudioTrack()).enablePlayback(enabled);
                    }
                }
            }
        }
    }

    public void publishLocalVideo(boolean enabled) {
        if (StoreData.localParticipant != null && StoreData.localVideoTrack != null) {
            if (enabled) {
              StoreData.localParticipant.publishTrack(StoreData.localVideoTrack);
            } else {
              StoreData.localParticipant.unpublishTrack(StoreData.localVideoTrack);
            }
        }
    }

    public void publishLocalAudio(boolean enabled) {
        if (StoreData.localParticipant != null && StoreData.localAudioTrack != null) {
            if (enabled) {
              StoreData.localParticipant.publishTrack(StoreData.localAudioTrack);
            } else {
              StoreData.localParticipant.unpublishTrack(StoreData.localAudioTrack);
            }
        }
    }


    private void convertBaseTrackStats(BaseTrackStats bs, WritableMap result) {
        result.putString("codec", bs.codec);
        result.putInt("packetsLost", bs.packetsLost);
        result.putString("ssrc", bs.ssrc);
        result.putDouble("timestamp", bs.timestamp);
        result.putString("trackSid", bs.trackSid);
    }

    private void convertLocalTrackStats(LocalTrackStats ts, WritableMap result) {
        result.putDouble("bytesSent", ts.bytesSent);
        result.putInt("packetsSent", ts.packetsSent);
        result.putDouble("roundTripTime", ts.roundTripTime);
    }

    private void convertRemoteTrackStats(RemoteTrackStats ts, WritableMap result) {
        result.putDouble("bytesReceived", ts.bytesReceived);
        result.putInt("packetsReceived", ts.packetsReceived);
    }

    private WritableMap convertAudioTrackStats(RemoteAudioTrackStats as) {
        WritableMap result = new WritableNativeMap();
        result.putInt("audioLevel", as.audioLevel);
        result.putInt("jitter", as.jitter);
        convertBaseTrackStats(as, result);
        convertRemoteTrackStats(as, result);
        return result;
    }

    private WritableMap convertLocalAudioTrackStats(LocalAudioTrackStats as) {
        WritableMap result = new WritableNativeMap();
        result.putInt("audioLevel", as.audioLevel);
        result.putInt("jitter", as.jitter);
        convertBaseTrackStats(as, result);
        convertLocalTrackStats(as, result);
        return result;
    }

    private WritableMap convertVideoTrackStats(RemoteVideoTrackStats vs) {
        WritableMap result = new WritableNativeMap();
        WritableMap dimensions = new WritableNativeMap();
        dimensions.putInt("height", vs.dimensions.height);
        dimensions.putInt("width", vs.dimensions.width);
        result.putMap("dimensions", dimensions);
        result.putInt("frameRate", vs.frameRate);
        convertBaseTrackStats(vs, result);
        convertRemoteTrackStats(vs, result);
        return result;
    }

    private WritableMap convertLocalVideoTrackStats(LocalVideoTrackStats vs) {
        WritableMap result = new WritableNativeMap();
        WritableMap dimensions = new WritableNativeMap();
        dimensions.putInt("height", vs.dimensions.height);
        dimensions.putInt("width", vs.dimensions.width);
        result.putMap("dimensions", dimensions);
        result.putInt("frameRate", vs.frameRate);
        convertBaseTrackStats(vs, result);
        convertLocalTrackStats(vs, result);
        return result;
    }

    public void getStats() {
        if (StoreData.room != null) {
          StoreData.room.getStats(new StatsListener() {
                @Override
                public void onStats(List<StatsReport> statsReports) {
                    WritableMap event = new WritableNativeMap();
                    for (StatsReport sr : statsReports) {
                        WritableMap connectionStats = new WritableNativeMap();
                        WritableArray as = new WritableNativeArray();
                        for (RemoteAudioTrackStats s : sr.getRemoteAudioTrackStats()) {
                            as.pushMap(convertAudioTrackStats(s));
                        }
                        connectionStats.putArray("remoteAudioTrackStats", as);

                        WritableArray vs = new WritableNativeArray();
                        for (RemoteVideoTrackStats s : sr.getRemoteVideoTrackStats()) {
                            vs.pushMap(convertVideoTrackStats(s));
                        }
                        connectionStats.putArray("remoteVideoTrackStats", vs);

                        WritableArray las = new WritableNativeArray();
                        for (LocalAudioTrackStats s : sr.getLocalAudioTrackStats()) {
                            las.pushMap(convertLocalAudioTrackStats(s));
                        }
                        connectionStats.putArray("localAudioTrackStats", las);

                        WritableArray lvs = new WritableNativeArray();
                        for (LocalVideoTrackStats s : sr.getLocalVideoTrackStats()) {
                            lvs.pushMap(convertLocalVideoTrackStats(s));
                        }
                        connectionStats.putArray("localVideoTrackStats", lvs);
                        event.putMap(sr.getPeerConnectionId(), connectionStats);
                    }
                    pushEvent(CustomTwilioVideoView.this, ON_STATS_RECEIVED, event);
                }
            });
        }
    }

    public void disableOpenSLES() {
        WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true);
    }

    // ====== ROOM LISTENER ========================================================================

    /*
     * Room events listener
     */
    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                /*
                 * Enable changing the volume using the up/down keys during a conversation
                 */
                if (themedReactContext.getCurrentActivity() != null) {
                    themedReactContext.getCurrentActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                }

                StoreData.localParticipant = room.getLocalParticipant();
                StoreData.localParticipant.setListener(localListener());

                WritableMap event = new WritableNativeMap();
                event.putString("roomName",room.getName());
                event.putString("roomSid", room.getSid());
                List<RemoteParticipant> participants = room.getRemoteParticipants();

                WritableArray participantsArray = new WritableNativeArray();
                for (RemoteParticipant participant : participants) {
                    participantsArray.pushMap(buildParticipant(participant));
                }
                participantsArray.pushMap(buildParticipant(StoreData.localParticipant));
                event.putArray("participants", participantsArray);
                event.putMap("localParticipant", buildParticipant(StoreData.localParticipant));

                pushEvent(CustomTwilioVideoView.this, ON_CONNECTED, event);


                //There is not .publish it's publishTrack
                StoreData.localParticipant.publishTrack(StoreData.localDataTrack);
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                WritableMap event = new WritableNativeMap();
                event.putString("roomName", room.getName());
                event.putString("roomSid", room.getSid());
                event.putString("error", e.getMessage());
                pushEvent(CustomTwilioVideoView.this, ON_CONNECT_FAILURE, event);
            }

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {

            }

            @Override
            public void onReconnected(@NonNull Room room) {

            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                WritableMap event = new WritableNativeMap();

                /*
                 * Remove stream voice control
                 */
                if (themedReactContext.getCurrentActivity() != null) {
                    themedReactContext.getCurrentActivity().setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
                }
                if (StoreData.localParticipant != null) {
                    event.putString("participant", StoreData.localParticipant.getIdentity());
                }
                event.putString("roomName", room.getName());
                event.putString("roomSid", room.getSid());
                if (e != null) {
                    event.putString("error", e.getMessage());
                }
                pushEvent(CustomTwilioVideoView.this, ON_DISCONNECTED, event);

                StoreData.localParticipant = null;
                StoreData.roomName = null;
                StoreData.accessToken = null;


                StoreData.room = null;
                // Only reinitialize the UI if disconnect was not called from onDestroy()
                if (!StoreData.disconnectedFromOnDestroy) {
                    setAudioFocus(false);
                }
            }

            @Override
            public void onParticipantConnected(Room room, RemoteParticipant participant) {
                addParticipant(room, participant);

            }

            @Override
            public void onParticipantDisconnected(Room room, RemoteParticipant participant) {
                removeParticipant(room, participant);
            }

            @Override
            public void onRecordingStarted(Room room) {
            }

            @Override
            public void onRecordingStopped(Room room) {
            }

            @Override
            public void onDominantSpeakerChanged(Room room, RemoteParticipant remoteParticipant) {
                WritableMap event = new WritableNativeMap();

                event.putString("roomName", room.getName());
                event.putString("roomSid", room.getSid());

                if (remoteParticipant == null) {
                    event.putString("participant", "");
                } else {
                    event.putMap("participant", buildParticipant(remoteParticipant));
                }

                pushEvent(CustomTwilioVideoView.this, ON_DOMINANT_SPEAKER_CHANGED, event);
            }
        };
    }

    /*
     * Called when participant joins the room
     */
    private void addParticipant(Room room, RemoteParticipant remoteParticipant) {

        WritableMap event = new WritableNativeMap();
        event.putString("roomName", room.getName());
        event.putString("roomSid", room.getSid());
        event.putMap("participant", buildParticipant(remoteParticipant));

        pushEvent(this, ON_PARTICIPANT_CONNECTED, event);

        /*
         * Start listening for participant media events
         */
        remoteParticipant.setListener(mediaListener());

        for (final RemoteDataTrackPublication remoteDataTrackPublication :
                remoteParticipant.getRemoteDataTracks()) {
            /*
             * Data track messages are received on the thread that calls setListener. Post the
             * invocation of setting the listener onto our dedicated data track message thread.
             */
            if (remoteDataTrackPublication.isTrackSubscribed()) {
                dataTrackMessageThreadHandler.post(() -> addRemoteDataTrack(remoteParticipant,
                        remoteDataTrackPublication.getRemoteDataTrack()));
            }
        }
    }

    /*
     * Called when participant leaves the room
     */
    private void removeParticipant(Room room, RemoteParticipant participant) {
        WritableMap event = new WritableNativeMap();
        event.putString("roomName", room.getName());
        event.putString("roomSid", room.getSid());
        event.putMap("participant", buildParticipant(participant));
        pushEvent(this, ON_PARTICIPANT_DISCONNECTED, event);
        //something about this breaking.
        //participant.setListener(null);
    }

    private void addRemoteDataTrack(RemoteParticipant remoteParticipant, RemoteDataTrack remoteDataTrack) {
        StoreData.dataTrackRemoteParticipantMap.put(remoteDataTrack, remoteParticipant);
        remoteDataTrack.setListener(remoteDataTrackListener());
    }

    // ====== MEDIA LISTENER =======================================================================

    private RemoteParticipant.Listener mediaListener() {
        return new RemoteParticipant.Listener() {
            @Override
            public void onAudioTrackSubscribed(RemoteParticipant participant, RemoteAudioTrackPublication publication, RemoteAudioTrack audioTrack) {
                audioTrack.enablePlayback(StoreData.enableRemoteAudio);
                WritableMap event = buildParticipantVideoEvent(participant, publication);
                pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_ADDED_AUDIO_TRACK, event);
            }

            @Override
            public void onAudioTrackUnsubscribed(RemoteParticipant participant, RemoteAudioTrackPublication publication, RemoteAudioTrack audioTrack) {
                WritableMap event = buildParticipantVideoEvent(participant, publication);
                pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_REMOVED_AUDIO_TRACK, event);
            }

            @Override
            public void onAudioTrackSubscriptionFailed(RemoteParticipant participant, RemoteAudioTrackPublication publication, TwilioException twilioException) {

            }

            @Override
            public void onAudioTrackPublished(RemoteParticipant participant, RemoteAudioTrackPublication publication) {
            }

            @Override
            public void onAudioTrackUnpublished(RemoteParticipant participant, RemoteAudioTrackPublication publication) {

            }

            @Override
            public void onDataTrackSubscribed(RemoteParticipant remoteParticipant, RemoteDataTrackPublication remoteDataTrackPublication, RemoteDataTrack remoteDataTrack) {
                WritableMap event = buildParticipantDataEvent(remoteParticipant, remoteDataTrackPublication);
                pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_ADDED_DATA_TRACK, event);
                dataTrackMessageThreadHandler.post(() -> addRemoteDataTrack(remoteParticipant, remoteDataTrack));
            }

            @Override
            public void onDataTrackUnsubscribed(RemoteParticipant remoteParticipant, RemoteDataTrackPublication remoteDataTrackPublication, RemoteDataTrack remoteDataTrack) {
                WritableMap event = buildParticipantDataEvent(remoteParticipant, remoteDataTrackPublication);
                pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_REMOVED_DATA_TRACK, event);
            }

            @Override
            public void onDataTrackSubscriptionFailed(RemoteParticipant participant, RemoteDataTrackPublication publication, TwilioException twilioException) {

            }

            @Override
            public void onDataTrackPublished(RemoteParticipant participant, RemoteDataTrackPublication publication) {

            }

            @Override
            public void onDataTrackUnpublished(RemoteParticipant participant, RemoteDataTrackPublication publication) {

            }

            @Override
            public void onVideoTrackSubscribed(RemoteParticipant participant, RemoteVideoTrackPublication publication, RemoteVideoTrack videoTrack) {
                addParticipantVideo(participant, publication);
            }

            @Override
            public void onVideoTrackUnsubscribed(RemoteParticipant participant, RemoteVideoTrackPublication publication, RemoteVideoTrack videoTrack) {
                removeParticipantVideo(participant, publication);
                StoreData.remoteThumbnailVideoView = null;
            }

            @Override
            public void onVideoTrackSubscriptionFailed(RemoteParticipant participant, RemoteVideoTrackPublication publication, TwilioException twilioException) {
            }

            @Override
            public void onVideoTrackPublished(RemoteParticipant participant, RemoteVideoTrackPublication publication) {

            }

            @Override
            public void onVideoTrackUnpublished(RemoteParticipant participant, RemoteVideoTrackPublication publication) {
                StoreData.remoteThumbnailVideoView = null;
            }

            @Override
            public void onAudioTrackEnabled(RemoteParticipant participant, RemoteAudioTrackPublication publication) {//                Log.i(TAG, "onAudioTrackEnabled");
//                publication.getRemoteAudioTrack().enablePlayback(false);
                WritableMap event = buildParticipantVideoEvent(participant, publication);
                pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_ENABLED_AUDIO_TRACK, event);
            }

            @Override
            public void onAudioTrackDisabled(RemoteParticipant participant, RemoteAudioTrackPublication publication) {
                WritableMap event = buildParticipantVideoEvent(participant, publication);
                pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_DISABLED_AUDIO_TRACK, event);
            }

            @Override
            public void onVideoTrackEnabled(RemoteParticipant participant, RemoteVideoTrackPublication publication) {
                WritableMap event = buildParticipantVideoEvent(participant, publication);
                pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_ENABLED_VIDEO_TRACK, event);
            }

            @Override
            public void onVideoTrackDisabled(RemoteParticipant participant, RemoteVideoTrackPublication publication) {
                WritableMap event = buildParticipantVideoEvent(participant, publication);
                pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_DISABLED_VIDEO_TRACK, event);
            }

            @Override
            public void onNetworkQualityLevelChanged(RemoteParticipant remoteParticipant, NetworkQualityLevel networkQualityLevel) {
                WritableMap event = new WritableNativeMap();
                event.putMap("participant", buildParticipant(remoteParticipant));
                event.putBoolean("isLocalUser", false);

                // Twilio SDK defines Enum 0 as UNKNOWN and 1 as Quality ZERO, so we subtract one to get the correct quality level as an integer
                event.putInt("quality", networkQualityLevel.ordinal() - 1);

                pushEvent(CustomTwilioVideoView.this, ON_NETWORK_QUALITY_LEVELS_CHANGED, event);
            }
        };
    }

    // ====== LOCAL LISTENER =======================================================================
    private LocalParticipant.Listener localListener() {
        return new LocalParticipant.Listener() {

            @Override
            public void onAudioTrackPublished(LocalParticipant localParticipant, LocalAudioTrackPublication localAudioTrackPublication) {

            }

            @Override
            public void onAudioTrackPublicationFailed(LocalParticipant localParticipant, LocalAudioTrack localAudioTrack, TwilioException twilioException) {

            }

            @Override
            public void onVideoTrackPublished(LocalParticipant localParticipant, LocalVideoTrackPublication localVideoTrackPublication) {

            }

            @Override
            public void onVideoTrackPublicationFailed(LocalParticipant localParticipant, LocalVideoTrack localVideoTrack, TwilioException twilioException) {

            }

            @Override
            public void onDataTrackPublished(LocalParticipant localParticipant, LocalDataTrackPublication localDataTrackPublication) {

            }

            @Override
            public void onDataTrackPublicationFailed(LocalParticipant localParticipant, LocalDataTrack localDataTrack, TwilioException twilioException) {

            }

            @Override
            public void onNetworkQualityLevelChanged(LocalParticipant localParticipant, NetworkQualityLevel networkQualityLevel) {
                WritableMap event = new WritableNativeMap();
                event.putMap("participant", buildParticipant(localParticipant));
                event.putBoolean("isLocalUser", true);

                // Twilio SDK defines Enum 0 as UNKNOWN and 1 as Quality ZERO, so we subtract one to get the correct quality level as an integer
                event.putInt("quality", networkQualityLevel.ordinal() - 1);

                pushEvent(CustomTwilioVideoView.this, ON_NETWORK_QUALITY_LEVELS_CHANGED, event);
            }
        };
    }

    private WritableMap buildParticipant(Participant participant) {
        WritableMap participantMap = new WritableNativeMap();
        participantMap.putString("identity", participant.getIdentity());
        participantMap.putString("sid", participant.getSid());
        return participantMap;
    }

    private WritableMap buildTrack(TrackPublication publication) {
        WritableMap trackMap = new WritableNativeMap();
        trackMap.putString("trackSid", publication.getTrackSid());
        trackMap.putString("trackName", publication.getTrackName());
        trackMap.putBoolean("enabled", publication.isTrackEnabled());
        return trackMap;
    }

    private WritableMap buildParticipantDataEvent(Participant participant, TrackPublication publication) {
        WritableMap participantMap = buildParticipant(participant);
        WritableMap trackMap = buildTrack(publication);

        WritableMap event = new WritableNativeMap();
        event.putMap("participant", participantMap);
        event.putMap("track", trackMap);
        return event;
    }

    private WritableMap buildParticipantVideoEvent(Participant participant, TrackPublication publication) {
        WritableMap participantMap = buildParticipant(participant);
        WritableMap trackMap = buildTrack(publication);

        WritableMap event = new WritableNativeMap();
        event.putMap("participant", participantMap);
        event.putMap("track", trackMap);
        return event;
    }

    private WritableMap buildDataTrackEvent(RemoteDataTrack remoteDataTrack, String message) {
        WritableMap event = new WritableNativeMap();
        event.putString("message", message);
        event.putString("trackSid", remoteDataTrack.getSid());
        return event;
    }

    private void addParticipantVideo(Participant participant, RemoteVideoTrackPublication publication) {
        WritableMap event = this.buildParticipantVideoEvent(participant, publication);
        pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_ADDED_VIDEO_TRACK, event);
    }

    private void removeParticipantVideo(Participant participant, RemoteVideoTrackPublication deleteVideoTrack) {
        WritableMap event = this.buildParticipantVideoEvent(participant, deleteVideoTrack);
        pushEvent(CustomTwilioVideoView.this, ON_PARTICIPANT_REMOVED_VIDEO_TRACK, event);
    }
    // ===== EVENTS TO RN ==========================================================================

    void pushEvent(View view, String name, WritableMap data) {
        eventEmitter.receiveEvent(view.getId(), name, data);
    }

    public static void registerPrimaryVideoView(PatchedVideoView v, String trackSid) {
        if (StoreData.room != null) {

            for (RemoteParticipant participant : StoreData.room.getRemoteParticipants()) {
                for (RemoteVideoTrackPublication publication : participant.getRemoteVideoTracks()) {
                    RemoteVideoTrack track = publication.getRemoteVideoTrack();
                    if (track == null) {
                        continue;
                    }
                    if (publication.getTrackSid().equals(trackSid)) {
                        track.addSink(v);
                        StoreData.remoteThumbnailVideoView = v;
                    } else {
                        track.removeSink(v);
                    }
                }
            }
        }
    }

    public static void registerThumbnailVideoView(PatchedVideoView v) {
        StoreData.thumbnailVideoView = v;
        if (StoreData.localVideoTrack != null) {
          StoreData.localVideoTrack.addSink(v);
        }
        setThumbnailMirror();
    }

    private RemoteDataTrack.Listener remoteDataTrackListener() {
        return new RemoteDataTrack.Listener() {

            @Override
            public void onMessage(RemoteDataTrack remoteDataTrack, ByteBuffer byteBuffer) {

            }


            @Override
            public void onMessage(RemoteDataTrack remoteDataTrack, String message) {
                WritableMap event = buildDataTrackEvent(remoteDataTrack, message);
                pushEvent(CustomTwilioVideoView.this, ON_DATATRACK_MESSAGE_RECEIVED, event);
            }
        };
    }

// ===== Orientation =================================================================================

    private OrientationEventListener specificOrientationListener = new OrientationEventListener(getContext()) {
        @Override
        public void onOrientationChanged(int orientation) {

            if (orientation >= 0 && orientation <= 359) {
                String specificOrientation = "UNKNOWN";

                if (orientation >= 0 && orientation < 45) {
                    specificOrientation = "PORTRAIT";
                } else if (orientation >= 45 && orientation < 135) {
                    specificOrientation = "LANDSCAPE_RIGHT";
                } else if (orientation >= 135 && orientation < 225) {
                    specificOrientation = "PORTRAIT_UPSIDE_DOWN";
                } else if (orientation >= 225 && orientation < 315) {
                    specificOrientation = "LANDSCAPE_LEFT";
                } else if (orientation >= 315 && orientation < 360) {
                    specificOrientation = "PORTRAIT";
                }
                currentDeviceOrientation = specificOrientation;
            }
        }
    };

}
