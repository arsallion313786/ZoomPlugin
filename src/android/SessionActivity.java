package cordova.plugin.zoomvideo;

import static android.os.Build.VERSION.SDK_INT;


import android.Manifest;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.app.AlertDialog;


import android.os.Environment;
import android.os.Handler;

import android.os.Parcelable;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.util.Rational;
import android.view.Display;

import android.view.View;
import android.view.WindowManager;

import android.widget.ImageView;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


import us.zoom.sdk.IncomingLiveStreamStatus;
import us.zoom.sdk.RealTimeMediaStreamsFailReason;
import us.zoom.sdk.RealTimeMediaStreamsStatus;
import us.zoom.sdk.SubSessionKit;
import us.zoom.sdk.SubSessionUserHelpRequestHandler;
import us.zoom.sdk.UVCCameraStatus;
import us.zoom.sdk.ZoomVideoSDK;
import us.zoom.sdk.ZoomVideoSDKAnnotationHelper;
import us.zoom.sdk.ZoomVideoSDKAudioHelper;
import us.zoom.sdk.ZoomVideoSDKAudioOption;
import us.zoom.sdk.ZoomVideoSDKAudioRawData;
import us.zoom.sdk.ZoomVideoSDKAudioStatus;
import us.zoom.sdk.ZoomVideoSDKBroadcastControlStatus;
import us.zoom.sdk.ZoomVideoSDKCRCCallStatus;
import us.zoom.sdk.ZoomVideoSDKCameraControlRequestHandler;
import us.zoom.sdk.ZoomVideoSDKCameraControlRequestType;
import us.zoom.sdk.ZoomVideoSDKChatHelper;
import us.zoom.sdk.ZoomVideoSDKChatMessage;
import us.zoom.sdk.ZoomVideoSDKChatMessageDeleteType;
import us.zoom.sdk.ZoomVideoSDKChatPrivilegeType;
import us.zoom.sdk.ZoomVideoSDKDataType;
import us.zoom.sdk.ZoomVideoSDKErrors;
import us.zoom.sdk.ZoomVideoSDKExportFormat;
import us.zoom.sdk.ZoomVideoSDKFileTransferStatus;
import us.zoom.sdk.ZoomVideoSDKInitParams;
import us.zoom.sdk.ZoomVideoSDKLiveStreamHelper;
import us.zoom.sdk.ZoomVideoSDKLiveStreamStatus;
import us.zoom.sdk.ZoomVideoSDKLiveTranscriptionHelper;
import us.zoom.sdk.ZoomVideoSDKMultiCameraStreamStatus;
import us.zoom.sdk.ZoomVideoSDKNetworkStatus;
import us.zoom.sdk.ZoomVideoSDKPasswordHandler;
import us.zoom.sdk.ZoomVideoSDKPhoneFailedReason;
import us.zoom.sdk.ZoomVideoSDKPhoneStatus;
import us.zoom.sdk.ZoomVideoSDKProxySettingHandler;
import us.zoom.sdk.ZoomVideoSDKRawDataPipe;
import us.zoom.sdk.ZoomVideoSDKReceiveFile;
import us.zoom.sdk.ZoomVideoSDKRecordingConsentHandler;
import us.zoom.sdk.ZoomVideoSDKRecordingStatus;
import us.zoom.sdk.ZoomVideoSDKSSLCertificateInfo;
import us.zoom.sdk.ZoomVideoSDKSendFile;
import us.zoom.sdk.ZoomVideoSDKSessionContext;
import us.zoom.sdk.ZoomVideoSDKSessionLeaveReason;
import us.zoom.sdk.ZoomVideoSDKShareAction;
import us.zoom.sdk.ZoomVideoSDKShareHelper;
import us.zoom.sdk.ZoomVideoSDKShareSetting;
import us.zoom.sdk.ZoomVideoSDKShareStatus;
import us.zoom.sdk.ZoomVideoSDKStreamingJoinStatus;
import us.zoom.sdk.ZoomVideoSDKSubSessionManager;
import us.zoom.sdk.ZoomVideoSDKSubSessionParticipant;
import us.zoom.sdk.ZoomVideoSDKSubSessionStatus;
import us.zoom.sdk.ZoomVideoSDKTestMicStatus;
import us.zoom.sdk.ZoomVideoSDKUser;
import us.zoom.sdk.ZoomVideoSDKUserHelpRequestResult;
import us.zoom.sdk.ZoomVideoSDKUserHelper;
import us.zoom.sdk.ZoomVideoSDKVideoCanvas;
import us.zoom.sdk.ZoomVideoSDKVideoOption;
import us.zoom.sdk.ZoomVideoSDKVideoResolution;
import us.zoom.sdk.ZoomVideoSDKVideoSubscribeFailReason;
import us.zoom.sdk.ZoomVideoSDKVideoView;
import us.zoom.sdk.ZoomVideoSDKVideoHelper;
import us.zoom.sdk.ZoomVideoSDKDelegate;
import us.zoom.sdk.ZoomVideoSDKVideoAspect;
import us.zoom.sdk.ZoomVideoSDKWhiteboardHelper;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class SessionActivity extends AppCompatActivity implements ZoomVideoSDKDelegate  {
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;

    /*
     * Necessary parameters to create or join the Zoom Video session.
     */
    private String jwtToken;
    private String sessionName;
    private String userName;
    private String domain;
    private String startingWaitingMessage;

    /*
     * Video views.
     */
    private ZoomVideoSDKVideoView primaryVideoView;
    private ZoomVideoSDKVideoView thumbnailVideoView;
    private ZoomVideoSDKVideoView secondaryThumbnailVideoView;
    private ZoomVideoSDKUser primaryUser;
    private String primaryUserSpeciality = "Doctor Speciality";
    private ZoomVideoSDKUser thumbnailUser;
    private ZoomVideoSDKUser secondaryThumbnailUser;

    /*
     * Android application UI elements
     */
    private ProgressBar progressBar;
    private TextView waitingMessageTextView;
    private TextView videoStatusTextView;
    private TextView identityTextView;
    private FloatingActionButton disconnectActionFab;
    private ImageView switchCameraActionFab;
    private  ImageView localVideoActionFab;
    private ImageView  muteActionFab;
    private ImageView  speakerActionFab;
    private ImageView  CallEndFabAction;
    private ImageView chatActionFab;
    private AlertDialog alertDialog;
    private AudioManager audioManager;

    private boolean shouldVideoBeOn = false;

    private Context context;
    final String LAYOUT = "layout";
    final String STRING = "string";
    final String DRAWABLE = "drawable";
    final String ID = "id";

    View videoControls;
// popup window


    //chat adapter

    private TextView timerTextView;
    private TextView NameDoctorTextView;
    private TextView specialityDoctorTextView;
    private Handler handler;
    private Runnable runnable;
    private long startTime;
    boolean isPipSupported;


    // Initialize your chat adapter and set it to the RecyclerView
    public static BottomSheetChat chatBottomSheetFragment;
    public  static  boolean isOpenedGalleryForImage = false;
    private List<ChatMessage> chatMessages = new ArrayList<>();

    private static SessionActivity instance;



    public static int getResourceId(Context context, String group, String key) {
        return context.getResources().getIdentifier(key, group, context.getPackageName());
    }

    public  static  SessionActivity getInstance(){
        return instance;
    }

//    private String getJWT(){
//        long iat = (System.currentTimeMillis()/1000) - 30;
//        long exp = iat + 60 * 60 * 2;
//
//        String header = "{\"alg\": \"HS256\", \"typ\": \"JWT\"}";
//        String payload = "{\"app_key\": \"" + this.sdkKey + "\"" +
//                ", \"tpc\": \"" + this.sessionName + "\"" +
//                ", \"role_type\": " + this.roleType +
//                ", \"session_key\": \"" + this.sessionKey + "\"" +
//                ", \"user_identity\": \"" + this.userIdentity + "\"" +
//                ", \"version\": 1" +
//                ", \"iat\": " + String.valueOf(iat) +
//                ", \"exp\": " + String.valueOf(exp) + "}";
//
//        try {
//            String headerBase64Str = Base64.encodeToString(header.getBytes(StandardCharsets.UTF_8),
//                    Base64.NO_WRAP| Base64.NO_PADDING | Base64.URL_SAFE);
//            String payloadBase64Str = Base64.encodeToString(payload.getBytes(StandardCharsets.UTF_8),
//                    Base64.NO_WRAP| Base64.NO_PADDING | Base64.URL_SAFE);
//
//            final Mac mac = Mac.getInstance("HmacSHA256");
//            SecretKeySpec secretKeySpec = new SecretKeySpec(this.sdkSecret.getBytes(), "HmacSHA256");
//            mac.init(secretKeySpec);
//
//            byte[] digest = mac.doFinal((headerBase64Str + "." + payloadBase64Str).getBytes());
//
//            return headerBase64Str + "." + payloadBase64Str + "." + Base64.encodeToString(digest,
//                    Base64.NO_WRAP| Base64.NO_PADDING | Base64.URL_SAFE);
//
//        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    private final BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", 0);
                if (state == 1) {
                    audioManager.setSpeakerphoneOn(false);
                    speakerActionFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                            getResourceId(context,DRAWABLE,("ic_microphone_icon"))));
                    // Headphones connected
                    // ... your logic for handling headphone connection ...
                } else if (state == 0) {
                    audioManager.setSpeakerphoneOn(true);
                    speakerActionFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                            getResourceId(context,DRAWABLE,("ic_volume_on"))));

                    // Headphones disconnected
                    // ... your logic for handling headphone disconnection ...
                }
            }
        }
    };

    private boolean isHeadsetConnected() {
        //AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn();
    }

    private boolean checkPermissionForCameraAndMicrophone(){
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForCameraAndMicrophone(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this,
                    getResourceId(context,STRING,("permissions_needed")),
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }

    private void initializeSDK() {
        ZoomVideoSDKInitParams params = new ZoomVideoSDKInitParams();

        if (this.domain == null || this.domain.equals("")) {
            params.domain = "https://zoom.us";
        } else {
            params.domain = domain;
        }



        ZoomVideoSDK sdk = ZoomVideoSDK.getInstance();

        int initResult = sdk.initialize(this, params);
        if (initResult == ZoomVideoSDKErrors.Errors_Success) {
            /* The ZoomVideoSDKDelegate allows you to subscribe to callback events that provide
            status updates on the operations performed in your app that are related to
            the Video SDK. For example, you might want to be notified when a user has successfully
            joined or left a session.  */
            sdk.addListener(this);
        } else {
            // Something went wrong, see error code documentation
           // Log.e("ZoomSDK", "Initialization result: errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
            Log.e("SessionActivity", "Initialize SDK error: " + initResult	);
        }
    }

    private void joinSession(Bundle savedInstanceState) {
        // Setup audio options
        ZoomVideoSDKAudioOption audioOptions = new ZoomVideoSDKAudioOption();
        audioOptions.connect = true; // Auto connect to audio upon joining
        audioOptions.mute = true; // Auto mute audio upon joining

        // Setup video options
        ZoomVideoSDKVideoOption videoOptions = new ZoomVideoSDKVideoOption();
        videoOptions.localVideoOn = false; // Turn on local/self video upon joining



        ZoomVideoSDKSessionContext sessionContext = new ZoomVideoSDKSessionContext();
        sessionContext.sessionName = this.sessionName;
        sessionContext.userName = this.userName;
        sessionContext.token = this.jwtToken;
        sessionContext.audioOption = audioOptions;
        sessionContext.videoOption = videoOptions;

        ZoomVideoSDK.getInstance().joinSession(sessionContext);

        // Start the timer when the session starts
        if (savedInstanceState != null) {
            startTime = savedInstanceState.getLong("startTime");
        } else {
            startTime = System.currentTimeMillis();
        }

        // Start the timer
        startTimer();
    }

    private void switchPrimaryAndSecondaryView() {
        ZoomVideoSDKUser auxUser = this.secondaryThumbnailUser;
        this.secondaryThumbnailUser = this.primaryUser;
        this.primaryUser = auxUser;
        // Subscribe to screen share instead of video if it on.
        if (this.primaryUser.getVideoCanvas().getShareStatus() != ZoomVideoSDKShareStatus.ZoomVideoSDKShareStatus_None) {
            this.primaryUser.getVideoCanvas().subscribe(this.primaryVideoView,
                    ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                    ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
        } else {
            this.primaryUser.getVideoCanvas().subscribe(this.primaryVideoView,
                    ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                    ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
        }
        // Subscribe to screen share instead of video if it on.
        if (this.secondaryThumbnailUser.getVideoCanvas().getShareStatus() != ZoomVideoSDKShareStatus.ZoomVideoSDKShareStatus_None) {
            this.secondaryThumbnailUser.getVideoCanvas().subscribe(this.secondaryThumbnailVideoView,
                    ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                    ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
        } else {
            this.secondaryThumbnailUser.getVideoCanvas().subscribe(this.secondaryThumbnailVideoView,
                    ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                    ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
        }
    }

    private View.OnClickListener setSecondaryAsPrimaryView() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (primaryUser != null && secondaryThumbnailUser != null) {
                    switchPrimaryAndSecondaryView();
                }
            }
        };
    }

    private View.OnClickListener disconnectClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * The session will be left OnDestroy.
                 */
                finish();
            }
        };
    }

    private View.OnClickListener switchCameraClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZoomVideoSDKVideoHelper videoHelper = ZoomVideoSDK.getInstance().getVideoHelper();
                videoHelper.switchCamera();
                videoHelper.mirrorMyVideo(false);
            }
        };
    }

    private View.OnClickListener localVideoClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Enable/disable the local video
                 */
                int icon;
                ZoomVideoSDKUser myUser = ZoomVideoSDK.getInstance().getSession().getMySelf();
                ZoomVideoSDKVideoHelper videoHelper = ZoomVideoSDK.getInstance().getVideoHelper();

                if (myUser.getVideoCanvas().getVideoStatus().isOn()) {

                    videoHelper.stopVideo();
                    shouldVideoBeOn = false;

                    icon = getResourceId(context,DRAWABLE,("icon_cross_camera"));
                    switchCameraActionFab.setVisibility(View.GONE);
                } else {

                    videoHelper.startVideo();
                    shouldVideoBeOn = true;

                    icon = getResourceId(context,DRAWABLE,("icon_camera"));
                    switchCameraActionFab.setVisibility(View.VISIBLE);
                }
                localVideoActionFab.setImageDrawable(ContextCompat.getDrawable(SessionActivity.this, icon));
            }
        };
    }

    private View.OnClickListener muteClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Mute/unmute the local audio.
                 */
                ZoomVideoSDK sdk = ZoomVideoSDK.getInstance();
                ZoomVideoSDKUser myUser = sdk.getSession().getMySelf();
                ZoomVideoSDKAudioHelper audioHelper = sdk.getAudioHelper();

                int icon;
                if(myUser.getAudioStatus().isMuted()) {
                    audioHelper.unMuteAudio(myUser);
                    icon = getResourceId(context,DRAWABLE,("icon_microphone"));;
                } else {
                    audioHelper.muteAudio(myUser);
                    icon = getResourceId(context,DRAWABLE,("icon_mute_cross"));
                }
                muteActionFab.setImageDrawable(ContextCompat.getDrawable(SessionActivity.this, icon));
            }
        };
    }

    private View.OnClickListener speakerClickListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioManager.isSpeakerphoneOn()) {
                    audioManager.setSpeakerphoneOn(false);
                    if(isHeadsetConnected()){
                        speakerActionFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                                getResourceId(context,DRAWABLE,("ic_microphone_icon"))));
                    }
                    else{

                        speakerActionFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                                getResourceId(context,DRAWABLE,("ic_volume_off"))));
                    }

                } else {
                    audioManager.setSpeakerphoneOn(true);
                    speakerActionFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                            getResourceId(context,DRAWABLE,("ic_volume_on"))));
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        instance = this; // This line is crucial
        Log.d("oncreate", "onCreate:trigger ");
        setContentView(getResourceId(context, LAYOUT, "activity_video"));

        // Initialize your views

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        isPipSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
        if (savedInstanceState != null) {
            // Restore your state here

            this.jwtToken = savedInstanceState.getString("jwtToken");
            this.sessionName = savedInstanceState.getString("sessionName");
            this.userName = savedInstanceState.getString("userName");
            this.domain = savedInstanceState.getString("domain");
            this.startingWaitingMessage = savedInstanceState.getString("startingWaitingMessage");
            this.primaryUserSpeciality = savedInstanceState.getString("primaryUserSpeciality");
            this.shouldVideoBeOn = savedInstanceState.getBoolean("shouldVideoBeOn");
            this.chatMessages =  (List<ChatMessage>) savedInstanceState.getSerializable("chatMessages");

            chatBottomSheetFragment = new BottomSheetChat(this.chatMessages,this);

            // Restore other necessary states
            Log.d("oncreate", "onCreate:SavedState ");
        } else {
            this.primaryUser = null;
            this.thumbnailUser = null;
            this.secondaryThumbnailUser = null;
            /*
             * Enable changing the volume using the up/down keys during a conversation
             */
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

            /*
             * Needed for setting/abandoning audio focus during call
             */
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            // Handle the case where there is no saved state
            Intent intent = getIntent();
            this.jwtToken = intent.getStringExtra("jwtToken");
            this.sessionName = intent.getStringExtra("sessionName");
            this.userName = intent.getStringExtra("userName");
            this.domain = intent.getStringExtra("domain");
            this.startingWaitingMessage = intent.getStringExtra("waitingMessage");
            this.primaryUserSpeciality = intent.getStringExtra("primaryUserSpeciality");
            this.primaryUserSpeciality = "--";
           //this.jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfa2V5IjoiellGM2QyQVoyRTZudTlTaXc0UVlRMTVRZW1VU0M0dDlxTmE2Iiwicm9sZV90eXBlIjoxLCJ0cGMiOiJCdXBhMTIzIiwidmVyc2lvbiI6MSwiaWF0IjoxNzMyMTkyNjM5LCJleHAiOjE3MzIxOTYyMzl9.zt0sij0I_eT6Y_Pw1Zuh7SuTSKQ5FFaAheI5iB_V4Cs";//intent.getStringExtra("jwtToken");
           // this.sessionName = "Bupa123";//intent.getStringExtra("sessionName");
           // this.userName = "Hasnain";//intent.getStringExtra("userName");
           // this.domain = "zoom.us";//intent.getStringExtra("domain");
           // this.startingWaitingMessage = "wait participant will join call";//intent.getStringExtra("waitingMessage");
          //  this.primaryUserSpeciality = "Family Doctor";//intent.getStringExtra("primaryUserSpeciality");

            chatBottomSheetFragment = new BottomSheetChat(this.chatMessages,this);
            // Check camera and microphone permissions

            Log.d("oncreate", "onCreate:WithoutSavedState ");
        }


            initializeViews();
            initializeSDK();
            if (!checkPermissionForCameraAndMicrophone()) {
                requestPermissionForCameraAndMicrophone();
            } else {
                joinSession(savedInstanceState);
            }

            waitingMessageTextView.setText(this.startingWaitingMessage);

//            Rect sourceRectHint = new Rect();
//            primaryVideoView.getGlobalVisibleRect(sourceRectHint);
//            PictureInPictureParams.Builder pipBuilder = null;
//            if (SDK_INT >= Build.VERSION_CODES.O) {
//                pipBuilder = new PictureInPictureParams.Builder().setSourceRectHint(sourceRectHint);
//            }
//            if (SDK_INT >= Build.VERSION_CODES.S) {
//                pipBuilder.setAutoEnterEnabled(true);
//                setPictureInPictureParams(pipBuilder.build());
//            }

        if (audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(false);
            speakerActionFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                    getResourceId(context,DRAWABLE,("ic_volume_off"))));
        } else {
            audioManager.setSpeakerphoneOn(true);
            speakerActionFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                    getResourceId(context,DRAWABLE,("ic_volume_on"))));
        }

        if (isHeadsetConnected()) {

            speakerActionFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                    getResourceId(context,DRAWABLE,("ic_microphone_icon"))));
        } else {
            speakerActionFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),
                    getResourceId(context,DRAWABLE,("ic_volume_on"))));
        }

        // Register the BroadcastReceiver for headphone connection changes
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetReceiver, filter);
    }



    private void initializeViews() {
        progressBar = findViewById(getResourceId(context, ID, "progressBar"));
        primaryVideoView = findViewById(getResourceId(context, ID, "primary_video_view"));
        thumbnailVideoView = findViewById(getResourceId(context, ID, "thumbnail_video_view"));
        secondaryThumbnailVideoView = findViewById(getResourceId(context, ID, "secondary_thumbnail_video_view"));
        secondaryThumbnailVideoView.setOnClickListener(setSecondaryAsPrimaryView());
        waitingMessageTextView = findViewById(getResourceId(context, ID, "waiting_message_textview"));
        videoStatusTextView = findViewById(getResourceId(context, ID, "video_status_textview"));
        identityTextView = findViewById(getResourceId(context, ID, "identity_textview"));
        videoControls = findViewById(getResourceId(context, ID, "video_control"));
        videoControls.setVisibility(View.GONE);
        disconnectActionFab = findViewById(getResourceId(context, ID, "disconnect_action_fab"));
        switchCameraActionFab = findViewById(getResourceId(context, ID, "switch_camera_action_fab"));
        localVideoActionFab = findViewById(getResourceId(context, ID, "local_video_action_fab"));
        muteActionFab = findViewById(getResourceId(context, ID, "mute_action_fab"));
        speakerActionFab = findViewById(getResourceId(context, ID, "speaker_action_fab"));
        chatActionFab = findViewById(getResourceId(context, ID, "icon_chat"));
        CallEndFabAction = findViewById(getResourceId(context, ID, "icon_call_end"));
        chatActionFab.setOnClickListener(v -> showChatPopup());
        CallEndFabAction.setOnClickListener(disconnectClickListener());
        disconnectActionFab.setOnClickListener(disconnectClickListener());
        switchCameraActionFab.setVisibility(View.GONE);
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        muteActionFab.setOnClickListener(muteClickListener());
        speakerActionFab.setOnClickListener(speakerClickListener());
        timerTextView = findViewById(getResourceId(context, ID, "timerTextView"));
        specialityDoctorTextView = findViewById(getResourceId(context, ID, "specialityDoctor"));
        NameDoctorTextView = findViewById(getResourceId(context, ID, "NameDoctor"));
    }

    @Override
    protected void onResume() {
        Log.d("Onresume", "onResume:Trigger ");
        super.onResume();
        ZoomVideoSDK sdk = ZoomVideoSDK.getInstance();
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        sdk.getVideoHelper().rotateMyVideo(display.getRotation());

        /*
         * If the video was stopped when the app was put in the background, start again.
         */
        if(sdk.isInSession() && shouldVideoBeOn) {
            sdk.getVideoHelper().startVideo();
        }
        startTimer();
    }


//    @Override
//    public void onUserLeaveHint () {
//
//
////        if(ZoomVideoSDK.getInstance().isInSession() && shouldVideoBeOn) {
////            ZoomVideoSDK.getInstance().getVideoHelper().stopVideo();
////        }
//
//        handler.removeCallbacks(runnable);
//        super.onUserLeaveHint();
//    }

    @Override
    protected void onPause() {

        if (chatBottomSheetFragment != null
                && chatBottomSheetFragment.getDialog() != null
                && chatBottomSheetFragment.getDialog().isShowing()
                && !chatBottomSheetFragment.isRemoving()
                && !isOpenedGalleryForImage
        ) {
            chatBottomSheetFragment.dismiss();
            //dialog is showing so do something
        }


        /*
         * Stop video before going in the background. This ensures that the
         * camera can be used by other applications while this app is in the background.
         */

        enterPipMode();

//        if(ZoomVideoSDK.getInstance().isInSession() && shouldVideoBeOn) {
//            ZoomVideoSDK.getInstance().getVideoHelper().stopVideo();
//        }

        handler.removeCallbacks(runnable);
        super.onPause();
    }


    @Override
    protected void onStop() {


        super.onStop();
    }
    @Override
    protected void onDestroy() {
        /*
         * Always leave the session before leaving the Activity.
         */
        if (this.secondaryThumbnailUser != null) {
            this.secondaryThumbnailUser.getVideoCanvas().unSubscribe(this.secondaryThumbnailVideoView);
        }
        if (this.thumbnailUser != null) {
            this.thumbnailUser.getVideoCanvas().unSubscribe(this.thumbnailVideoView);
        }
        if (this.primaryUser != null) {
            this.primaryUser.getVideoCanvas().unSubscribe(this.primaryVideoView);
        }

        ZoomVideoSDK sdk = ZoomVideoSDK.getInstance();
        ZoomVideoSDKVideoHelper videoHelper = sdk.getVideoHelper();
        ZoomVideoSDKAudioHelper audioHelper = sdk.getAudioHelper();
        videoHelper.stopVideo();
        audioHelper.stopAudio();

        shouldVideoBeOn = false;

        sdk.leaveSession(false);
        sdk.cleanup();
        handler.removeCallbacks(runnable);

        unregisterReceiver(headsetReceiver);
        super.onDestroy();


    }

    @Override
    public void onConfigurationChanged (@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // We want to rotate the local video based on the phone rotation.
        ZoomVideoSDK sdk = ZoomVideoSDK.getInstance();
        if(sdk.isInSession()) {
            Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
            sdk.getVideoHelper().rotateMyVideo(display.getRotation());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            boolean cameraAndMicPermissionGranted = true;

            for (int grantResult : grantResults) {
                cameraAndMicPermissionGranted &= grantResult == PackageManager.PERMISSION_GRANTED;
            }

            if (cameraAndMicPermissionGranted) {
                joinSession(null);
            } else {
                Toast.makeText(this, getResourceId(context,STRING,("permissions_needed")), Toast.LENGTH_LONG).show();
            }
        }
    }

    /* SDK callback listeners */
    @Override
    public void onSessionJoin() {

        this.progressBar.setVisibility(View.GONE);
        this.videoControls.setVisibility(View.VISIBLE);

       this.disconnectActionFab.setVisibility(View.GONE);
//        this.switchCameraActionFab.setVisibility(View.VISIBLE);
//        this.localVideoActionFab.setVisibility(View.VISIBLE);
//        this.muteActionFab.setVisibility(View.VISIBLE);
//        this.speakerActionFab.setVisibility(View.VISIBLE);

        this.waitingMessageTextView.setVisibility(View.VISIBLE);

        ZoomVideoSDK sdk = ZoomVideoSDK.getInstance();
        ZoomVideoSDKUser myUser = sdk.getSession().getMySelf();
        ZoomVideoSDKVideoCanvas myCanvas = myUser.getVideoCanvas();

        /* Start Video */
        ZoomVideoSDKVideoHelper videoHelper = sdk.getVideoHelper();
//        if (!myCanvas.getVideoStatus().isOn()) {
//            videoHelper.startVideo();
//            shouldVideoBeOn = true;
//        } else {
//            Log.i("SessionActivity", "Video was already started onSessionJoin");
//        }

        this.thumbnailVideoView.setVisibility(View.VISIBLE);
        this.thumbnailUser = myUser;
        myCanvas.subscribe(this.thumbnailVideoView,
                ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);

        /* Start Audio */
        ZoomVideoSDKAudioStatus audioStatus = myUser.getAudioStatus();
        ZoomVideoSDKAudioStatus.ZoomVideoSDKAudioType audioType = audioStatus.getAudioType();
        ZoomVideoSDKAudioHelper audioHelper = sdk.getAudioHelper();
        if (audioType == ZoomVideoSDKAudioStatus.ZoomVideoSDKAudioType.ZoomVideoSDKAudioType_None) {
            audioHelper.startAudio();
        } else {
            Log.i("SessionActivity", "Audio was already started onSessionJoin");
        }
    }

    @Override
    public void onSessionLeave() {
        Log.i("SessionActivity", "on session leave called");
    }

    @Override
    public void onSessionLeave(ZoomVideoSDKSessionLeaveReason reason) {

    }

    @Override
    public void onError(int errorCode) {
        Log.i("SessionActivity", "on session leave called");
    }

    @Override
    public void onUserJoin(ZoomVideoSDKUserHelper userHelper, List<ZoomVideoSDKUser> userList) {
        for (ZoomVideoSDKUser user : userList) {

            ZoomVideoSDKVideoCanvas userCanvas = user.getVideoCanvas();

            // Place the user in the primary view if it is available.
            if(this.primaryUser == null){

                this.primaryUserSpeciality = user.isHost()? "Practitioner/Doctor":"";
                this.waitingMessageTextView.setVisibility(View.GONE);

                userCanvas.subscribe(this.primaryVideoView,
                        ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                        ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
                this.primaryUser = user;
                this.NameDoctorTextView.setText(user.getUserName());
                specialityDoctorTextView.setText(this.primaryUserSpeciality);
            } else if (this.secondaryThumbnailUser == null) {
                // In this case, the user will be in the secondary thumbnail if it is available.
                this.secondaryThumbnailVideoView.setVisibility(View.VISIBLE);
                this.secondaryThumbnailUser = user;
                userCanvas.subscribe(this.secondaryThumbnailVideoView,
                        ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                        ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
            }
        }
    }

    @Override
    public void onUserLeave(ZoomVideoSDKUserHelper userHelper, List<ZoomVideoSDKUser> userList) {
        for (ZoomVideoSDKUser user : userList) {
            if (this.primaryUser != null && this.primaryUser.getUserID().equals(user.getUserID())) {
                // Remove the user from primary view.
                user.getVideoCanvas().unSubscribe(this.primaryVideoView);
                this.primaryUser = null;

                if (this.secondaryThumbnailUser != null) {
                    // Move the secondary thumbnail user to the primary view.
                    this.secondaryThumbnailUser.getVideoCanvas().unSubscribe(this.secondaryThumbnailVideoView);
                    this.secondaryThumbnailVideoView.setVisibility(View.GONE);

                    this.primaryUser = this.secondaryThumbnailUser;
                    this.secondaryThumbnailUser = null;

                    this.primaryUser.getVideoCanvas().subscribe(this.primaryVideoView,
                            ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                            ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
                } else {
                    //waitingMessageTextView.setText(String.format("%sLeft session", this.primaryUser.getUserName()));
                    this.waitingMessageTextView.setVisibility(View.VISIBLE);
                }

            } else if (this.secondaryThumbnailUser != null && this.secondaryThumbnailUser.getUserID().equals(user.getUserID())) {
                this.secondaryThumbnailUser.getVideoCanvas().unSubscribe(this.secondaryThumbnailVideoView);
                this.secondaryThumbnailVideoView.setVisibility(View.GONE);
                this.secondaryThumbnailUser = null;
            }
        }
    }

    @Override
    public void onUserVideoStatusChanged(ZoomVideoSDKVideoHelper videoHelper, List<ZoomVideoSDKUser> userList) {

    }

    @Override
    public void onShareNetworkStatusChanged(ZoomVideoSDKNetworkStatus shareNetworkStatus, boolean isSendingShare) {

    }

    @Override
    public void onUserAudioStatusChanged(ZoomVideoSDKAudioHelper audioHelper, List<ZoomVideoSDKUser> userList) {

    }

    @Override
    public void onUserShareStatusChanged(ZoomVideoSDKShareHelper shareHelper, ZoomVideoSDKUser userInfo, ZoomVideoSDKShareStatus status) {
        switch (status) {
            case ZoomVideoSDKShareStatus_Resume:
            case ZoomVideoSDKShareStatus_Start:
                // The user with the corresponding userInfo is now sharing
                ZoomVideoSDKVideoCanvas shareCanvas = userInfo.getVideoCanvas();
                if (this.primaryUser != null && this.primaryUser.getUserID().equals(userInfo.getUserID())) {
                    shareCanvas.subscribe(this.primaryVideoView,
                            ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                            ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
                } else if (this.secondaryThumbnailUser != null && this.secondaryThumbnailUser.getUserID().equals(userInfo.getUserID())) {
                    shareCanvas.subscribe(this.secondaryThumbnailVideoView,
                            ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                            ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
                }
                break;
            case ZoomVideoSDKShareStatus_Pause:
            case ZoomVideoSDKShareStatus_Stop:
                // The user with the corresponding userInfo is not sharing
                ZoomVideoSDKVideoCanvas videoCanvas = userInfo.getVideoCanvas();
                if (this.primaryUser != null && this.primaryUser.getUserID().equals(userInfo.getUserID())) {
                    videoCanvas.subscribe(this.primaryVideoView,
                            ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                            ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
                } else if (this.secondaryThumbnailUser != null && this.secondaryThumbnailUser.getUserID().equals(userInfo.getUserID())) {
                    videoCanvas.subscribe(this.secondaryThumbnailVideoView,
                            ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan,
                            ZoomVideoSDKVideoResolution.ZoomVideoSDKResolution_Auto);
                }
                break;
        }
    }

    @Override
    public void onUserShareStatusChanged(ZoomVideoSDKShareHelper shareHelper, ZoomVideoSDKUser userInfo, ZoomVideoSDKShareAction shareAction) {

    }

    @Override
    public void onShareContentChanged(ZoomVideoSDKShareHelper shareHelper, ZoomVideoSDKUser userInfo, ZoomVideoSDKShareAction shareAction) {

    }

    @Override
    public void onLiveStreamStatusChanged(ZoomVideoSDKLiveStreamHelper liveStreamHelper, ZoomVideoSDKLiveStreamStatus status) {

    }

    @Override
    public void onChatNewMessageNotify(ZoomVideoSDKChatHelper chatHelper, ZoomVideoSDKChatMessage messageItem) {
        //Handle the received chat message
        String content = messageItem.getContent();
        String senderName = messageItem.getSenderUser().getUserName();
        //if (chatBottomSheetFragment != null && chatBottomSheetFragment.isVisible()) {
       //     chatBottomSheetFragment.updateChatMessages(chatMessages);
       //     Log.d("chatMessage", "onChatNewMessageNotify: "+chatMessages.size());
      //  }
        runOnUiThread(() -> {
            // Update your UI with the new message
            chatBottomSheetFragment.addMessage(new ChatMessage(senderName,content));
        });
    }

    @Override
    public void onChatDeleteMessageNotify(ZoomVideoSDKChatHelper chatHelper, String msgID, ZoomVideoSDKChatMessageDeleteType deleteBy) {

    }

    @Override
    public void onChatPrivilegeChanged(ZoomVideoSDKChatHelper chatHelper, ZoomVideoSDKChatPrivilegeType currentPrivilege) {

    }

    @Override
    public void onUserHostChanged(ZoomVideoSDKUserHelper userHelper, ZoomVideoSDKUser userInfo) {

    }

    @Override
    public void onUserManagerChanged(ZoomVideoSDKUser user) {

    }

    @Override
    public void onUserNameChanged(ZoomVideoSDKUser user) {

    }

    @Override
    public void onUserActiveAudioChanged(ZoomVideoSDKAudioHelper audioHelper, List<ZoomVideoSDKUser> list) {
//        for (ZoomVideoSDKUser user : list) {
//            // Check if the current user is talking
//            if (this.secondaryThumbnailUser != null
//                    && this.secondaryThumbnailUser.getUserID().equals(user.getUserID())
//                    && this.primaryUser != null
//                    && !this.primaryUser.getAudioStatus().isTalking()
//                    && user.getAudioStatus().isTalking()) {
//
//                switchPrimaryAndSecondaryView();
//            }
//        }
    }

    @Override
    public void onSessionNeedPassword(ZoomVideoSDKPasswordHandler handler) {

    }

    @Override
    public void onSessionPasswordWrong(ZoomVideoSDKPasswordHandler handler) {

    }

    @Override
    public void onMixedAudioRawDataReceived(ZoomVideoSDKAudioRawData rawData) {

    }

    @Override
    public void onOneWayAudioRawDataReceived(ZoomVideoSDKAudioRawData rawData, ZoomVideoSDKUser user) {

    }

    @Override
    public void onShareAudioRawDataReceived(ZoomVideoSDKAudioRawData rawData) {

    }

    @Override
    public void onCommandReceived(ZoomVideoSDKUser sender, String strCmd) {

    }

    @Override
    public void onCommandChannelConnectResult(boolean isSuccess) {

    }

    @Override
    public void onCloudRecordingStatus(ZoomVideoSDKRecordingStatus status, ZoomVideoSDKRecordingConsentHandler handler) {

    }

    @Override
    public void onHostAskUnmute() {

    }

    @Override
    public void onInviteByPhoneStatus(ZoomVideoSDKPhoneStatus status, ZoomVideoSDKPhoneFailedReason reason) {

    }

    @Override
    public void onMultiCameraStreamStatusChanged(ZoomVideoSDKMultiCameraStreamStatus status, ZoomVideoSDKUser user, ZoomVideoSDKRawDataPipe videoPipe) {

    }

    @Override
    public void onMultiCameraStreamStatusChanged(ZoomVideoSDKMultiCameraStreamStatus status, ZoomVideoSDKUser user, ZoomVideoSDKVideoCanvas canvas) {

    }

    @Override
    public void onLiveTranscriptionStatus(ZoomVideoSDKLiveTranscriptionHelper.ZoomVideoSDKLiveTranscriptionStatus status) {

    }


    public void onLiveTranscriptionMsgReceived(String ltMsg, ZoomVideoSDKUser pUser, ZoomVideoSDKLiveTranscriptionHelper.ZoomVideoSDKLiveTranscriptionOperationType type) {

    }

    @Override
    public void onOriginalLanguageMsgReceived(ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionMessageInfo messageInfo) {

    }

    @Override
    public void onLiveTranscriptionMsgInfoReceived(ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionMessageInfo messageInfo) {

    }

    @Override
    public void onLiveTranscriptionMsgError(ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage spokenLanguage, ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage transcriptLanguage) {

    }

    @Override
    public void onSpokenLanguageChanged(ZoomVideoSDKLiveTranscriptionHelper.ILiveTranscriptionLanguage spokenLanguage) {

    }

    @Override
    public void onProxySettingNotification(ZoomVideoSDKProxySettingHandler handler) {

    }

    @Override
    public void onSSLCertVerifiedFailNotification(ZoomVideoSDKSSLCertificateInfo info) {

    }

    @Override
    public void onCameraControlRequestResult(ZoomVideoSDKUser user, boolean isApproved) {

    }

    @Override
    public void onCameraControlRequestReceived(ZoomVideoSDKUser user, ZoomVideoSDKCameraControlRequestType requestType, ZoomVideoSDKCameraControlRequestHandler requestHandler) {

    }

    @Override
    public void onUserVideoNetworkStatusChanged(ZoomVideoSDKNetworkStatus status, ZoomVideoSDKUser user) {

    }

    @Override
    public void onUserRecordingConsent(ZoomVideoSDKUser user) {

    }

    @Override
    public void onCallCRCDeviceStatusChanged(ZoomVideoSDKCRCCallStatus status) {

    }

    @Override
    public void onVideoCanvasSubscribeFail(ZoomVideoSDKVideoSubscribeFailReason fail_reason, ZoomVideoSDKUser pUser, ZoomVideoSDKVideoView view) {

    }

    @Override
    public void onShareCanvasSubscribeFail(ZoomVideoSDKVideoSubscribeFailReason fail_reason, ZoomVideoSDKUser pUser, ZoomVideoSDKVideoView view) {

    }

    @Override
    public void onShareCanvasSubscribeFail(ZoomVideoSDKUser pUser, ZoomVideoSDKVideoView view, ZoomVideoSDKShareAction shareAction) {

    }

    @Override
    public void onAnnotationHelperCleanUp(ZoomVideoSDKAnnotationHelper helper) {

    }

    @Override
    public void onAnnotationPrivilegeChange(ZoomVideoSDKUser shareOwner, ZoomVideoSDKShareAction shareAction) {

    }


    public void onAnnotationPrivilegeChange(boolean enable, ZoomVideoSDKUser shareOwner) {

    }

    @Override
    public void onTestMicStatusChanged(ZoomVideoSDKTestMicStatus status) {

    }

    @Override
    public void onMicSpeakerVolumeChanged(int micVolume, int speakerVolume) {

    }

    @Override
    public void onCalloutJoinSuccess(ZoomVideoSDKUser user, String phoneNumber) {

    }

    @Override
    public void onSendFileStatus(ZoomVideoSDKSendFile file, ZoomVideoSDKFileTransferStatus status) {

    }

    @Override
    public void onReceiveFileStatus(ZoomVideoSDKReceiveFile file, ZoomVideoSDKFileTransferStatus status) {

    }

    @Override
    public void onUVCCameraStatusChange(String cameraId, UVCCameraStatus status) {

    }

    @Override
    public void onVideoAlphaChannelStatusChanged(boolean isAlphaModeOn) {

    }

    @Override
    public void onSpotlightVideoChanged(ZoomVideoSDKVideoHelper videoHelper, List<ZoomVideoSDKUser> userList) {

    }

    @Override
    public void onFailedToStartShare(ZoomVideoSDKShareHelper shareHelper, ZoomVideoSDKUser user) {

    }

    @Override
    public void onBindIncomingLiveStreamResponse(boolean bSuccess, String streamKeyID) {

    }

    @Override
    public void onUnbindIncomingLiveStreamResponse(boolean bSuccess, String streamKeyID) {

    }

    @Override
    public void onIncomingLiveStreamStatusResponse(boolean bSuccess, List<IncomingLiveStreamStatus> streamsStatusList) {

    }

    @Override
    public void onStartIncomingLiveStreamResponse(boolean bSuccess, String streamKeyID) {

    }

    @Override
    public void onStopIncomingLiveStreamResponse(boolean bSuccess, String streamKeyID) {

    }

    @Override
    public void onShareContentSizeChanged(ZoomVideoSDKShareHelper shareHelper, ZoomVideoSDKUser user, ZoomVideoSDKShareAction shareAction) {

    }

    @Override
    public void onSubSessionStatusChanged(ZoomVideoSDKSubSessionStatus status, List<SubSessionKit> subSessionKitList) {

    }

    @Override
    public void onSubSessionManagerHandle(ZoomVideoSDKSubSessionManager manager) {

    }

    @Override
    public void onSubSessionParticipantHandle(ZoomVideoSDKSubSessionParticipant participant) {

    }

    @Override
    public void onSubSessionUsersUpdate(SubSessionKit subSessionKit) {

    }

    @Override
    public void onBroadcastMessageFromMainSession(String message, String userName) {

    }

    @Override
    public void onSubSessionUserHelpRequest(SubSessionUserHelpRequestHandler handler) {

    }

    @Override
    public void onSubSessionUserHelpRequestResult(ZoomVideoSDKUserHelpRequestResult eResult) {

    }

    @Override
    public void onShareSettingChanged(ZoomVideoSDKShareSetting setting) {

    }

    @Override
    public void onStartBroadcastResponse(boolean bSuccess, String channelID) {

    }

    @Override
    public void onStopBroadcastResponse(boolean bSuccess) {

    }

    @Override
    public void onGetBroadcastControlStatus(boolean bSuccess, ZoomVideoSDKBroadcastControlStatus status) {

    }

    @Override
    public void onStreamingJoinStatusChanged(ZoomVideoSDKStreamingJoinStatus status) {

    }

    @Override
    public void onUserWhiteboardShareStatusChanged(ZoomVideoSDKUser user, ZoomVideoSDKWhiteboardHelper helper) {

    }

    @Override
    public void onWhiteboardExported(ZoomVideoSDKExportFormat format, byte[] data) {

    }

    @Override
    public void onMyAudioSourceTypeChanged(ZoomVideoSDKAudioHelper.ZoomVideoSDKAudioDevice device) {

    }

    @Override
    public void onUserNetworkStatusChanged(ZoomVideoSDKDataType type, ZoomVideoSDKNetworkStatus level, ZoomVideoSDKUser user) {

    }

    @Override
    public void onUserOverallNetworkStatusChanged(ZoomVideoSDKNetworkStatus level, ZoomVideoSDKUser user) {

    }

    @Override
    public void onAudioLevelChanged(int level, boolean audioSharing, ZoomVideoSDKUser user) {

    }

    @Override
    public void onRealTimeMediaStreamsStatus(RealTimeMediaStreamsStatus status) {

    }

    @Override
    public void onRealTimeMediaStreamsFail(RealTimeMediaStreamsFailReason failReason) {

    }

    private void showChatPopup() {
//        if (SDK_INT >= Build.VERSION_CODES.R) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
//                            Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 1);
//
//            if (Environment.isExternalStorageManager()) {
//
//            } else {
//                Intent intent = new Intent();
//                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//                Uri uri = Uri.fromParts("package", getPackageName(), null);
//                intent.setData(uri);
//                startActivity(intent);
//            }
//
//
//        } else {
//
//            if (SDK_INT >= 23) {
//                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                        == PackageManager.PERMISSION_GRANTED) {
//                    Log.v("Permission", "Storage Permission is granted");
//
//                } else {
//                    Log.v("Permission", "Storage Permission is revoked");
//                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//
//                }
//            } else { //permission is automatically granted on sdk<23 upon installation
//                Log.v("Permission", "Storage Permission is granted");
//
//            }
//        }
        chatBottomSheetFragment.show(getSupportFragmentManager(), "chat_bottom_sheet");

       // chatBottomSheetFragment.updateChatMessages(chatMessages);
      //  Log.d("chatMessage", "onChatNewMessageNotify: "+chatMessages.size());
    }

    public void showAttachmentViewer(String fileName, String mimeType, String binaryData) {
        try {
            // 1. Create a temporary file
            File tempFile = new File(getCacheDir(), fileName);
            byte[] fileData = Base64.decode(binaryData, Base64.DEFAULT);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileData);
            }

            // 2. Create and show the DialogFragment
            AttachmentViewerDialogFragment viewerFragment = AttachmentViewerDialogFragment.newInstance(
                    mimeType,
                    tempFile.getAbsolutePath()
            );

            // *** CRITICAL FIX: Use getSupportFragmentManager() for an Activity ***
            viewerFragment.show(getSupportFragmentManager(), "AttachmentViewer");

        } catch (IOException e) {
            Log.e("SessionActivity", "Failed to create or write to temporary file", e);
        }
    }

    private void startTimer() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedTime / 1000) % 60;
                int minutes = (int) ((elapsedTime / (1000 * 60)) % 60);
                int hours = (int) ((elapsedTime / (1000 * 60 * 60)) % 24);
                String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                timerTextView.setText(time);
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(16, 9);
//            Rect sourceRectHint = new Rect();
//            primaryVideoView.getGlobalVisibleRect(sourceRectHint);
//            PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder().setSourceRectHint(sourceRectHint);
//            if (SDK_INT >= Build.VERSION_CODES.S) {
//                pipBuilder.setAutoEnterEnabled(true);
//                setPictureInPictureParams(pipBuilder.build());
//            }

            PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
            pipBuilder.setAspectRatio(aspectRatio);
            ArrayList<RemoteAction> actions = new ArrayList<>();





            // Example: Add a custom action button
            Intent intent = new Intent(this, SessionActivity.class);
            intent.setAction("PIP_ACTION");
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            RemoteAction action = new RemoteAction(
                    Icon.createWithResource(this, getResourceId(this,DRAWABLE,"icon_call_end")),
                    "End Call",
                    "End Call",
                    pendingIntent
            );
            actions.add(action);
            pipBuilder.setActions(actions);
            enterPictureInPictureMode(pipBuilder.build());
            if (isPipSupported && ZoomVideoSDK.getInstance().isInSession()) {
                Log.d("PIP", "*** Entering Picture-in-Picture ***");
                enterPictureInPictureMode(pipBuilder.build());
            } else {
                Log.d("PIP", "*** No support for Picture-in-Picture ***");
            }
        }
    }

    @Override 
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (isInPictureInPictureMode) {
            // Hide UI elements that are not needed in PiP mode
            Log.d("PIP", "*** Picture-in-Picture Mode ***");
            if (videoControls != null) videoControls.setVisibility(View.GONE);
            if (disconnectActionFab != null) disconnectActionFab.setVisibility(View.VISIBLE);
        } else {
            // Restore UI elements when exiting PiP mode
            Log.d("PIP", "*** Exiting Picture-in-Picture Mode ***");
            if (videoControls != null) videoControls.setVisibility(View.VISIBLE);
            if (disconnectActionFab != null) disconnectActionFab.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("jwtToken", jwtToken);
        outState.putString("sessionName", sessionName);
        outState.putString("userName", userName);
        outState.putString("domain", domain);
        outState.putString("startingWaitingMessage", startingWaitingMessage);
        outState.putString("primaryUserSpeciality", primaryUserSpeciality);
        outState.putBoolean("shouldVideoBeOn", shouldVideoBeOn);
        outState.putSerializable("chatMessages", (Serializable) chatMessages);
        outState.putLong("startTime", startTime);
        outState.putLong("elapsedTime", System.currentTimeMillis() - startTime);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            this.chatMessages =  (List<ChatMessage>) savedInstanceState.getSerializable("chatMessages");
            startTime = savedInstanceState.getLong("startTime");
            long elapsedTime = savedInstanceState.getLong("elapsedTime");
            startTime = System.currentTimeMillis() - elapsedTime;
            startTimer();
        }
    }



}
