package cordova.plugin.zoomvideo;



import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.app.AlertDialog;



import android.os.Handler;


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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


import us.zoom.sdk.*; // Using wildcard import for cleaner code with many SDK classes

public class SessionActivity extends AppCompatActivity implements ZoomVideoSDKDelegate {

    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;

    // Session parameters
    private String jwtToken;
    private String sessionName;
    private String userName;
    private String domain;
    private String startingWaitingMessage;
    private String primaryUserSpeciality = "Doctor Speciality";

    // Video views and users
    private ZoomVideoSDKVideoView primaryVideoView;
    private ZoomVideoSDKVideoView thumbnailVideoView;
    private ZoomVideoSDKVideoView secondaryThumbnailVideoView;
    private ZoomVideoSDKUser primaryUser;
    private ZoomVideoSDKUser thumbnailUser;
    private ZoomVideoSDKUser secondaryThumbnailUser;

    // UI Elements
    private ProgressBar progressBar;
    private TextView waitingMessageTextView;
    private TextView timerTextView;
    private TextView NameDoctorTextView;
    private TextView specialityDoctorTextView;
    private FloatingActionButton disconnectActionFab;
    private ImageView switchCameraActionFab;
    private ImageView localVideoActionFab;
    private ImageView muteActionFab;
    private ImageView speakerActionFab;
    private ImageView CallEndFabAction;
    private ImageView chatActionFab;
    private View videoControls;

    // State management
    private boolean shouldVideoBeOn = false;

    // Timer logic
    private Handler handler;
    private Runnable runnable;
    private long startTime;

    // Master list for chat messages. This is the "single source of truth".
    private List<ChatMessage> chatMessages = new ArrayList<>();

    // Audio management
    private AudioManager audioManager;

    // Resource ID helper
    final String LAYOUT = "layout";
    final String STRING = "string";
    final String DRAWABLE = "drawable";
    final String ID = "id";

    private static SessionActivity instance;

    private String pendingAttachmentPath = null;
    private String pendingAttachmentMimeType = null;
    private final AtomicBoolean isActivityInForeground = new AtomicBoolean(false);


    // Add this static getter method inside the SessionActivity class
    public static SessionActivity getActiveInstance() {
        return instance;
    }



    public static int getResourceId(Context context, String group, String key) {
        return context.getResources().getIdentifier(key, group, context.getPackageName());
    }


    private final BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), Intent.ACTION_HEADSET_PLUG)) {
                updateSpeakerUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(getResourceId(this, LAYOUT, "activity_video"));


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        initializeViews();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            initializeWithIntent();
        }

        setupActionFabListeners();
        initializeSDK();
        updateSpeakerUI();

        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            joinSession(savedInstanceState);
        }
    }

    private void restoreState(Bundle savedInstanceState) {
        this.jwtToken = savedInstanceState.getString("jwtToken");
        this.sessionName = savedInstanceState.getString("sessionName");
        this.userName = savedInstanceState.getString("userName");
        this.domain = savedInstanceState.getString("domain");
        this.startingWaitingMessage = savedInstanceState.getString("startingWaitingMessage");
        this.primaryUserSpeciality = savedInstanceState.getString("primaryUserSpeciality");
        this.shouldVideoBeOn = savedInstanceState.getBoolean("shouldVideoBeOn");
        this.chatMessages = (List<ChatMessage>) savedInstanceState.getSerializable("chatMessages");
        if (savedInstanceState.containsKey("elapsedTime")) {
            this.startTime = System.currentTimeMillis() - savedInstanceState.getLong("elapsedTime");
        }
        waitingMessageTextView.setText(this.startingWaitingMessage);
    }

    private void initializeWithIntent() {
        Intent intent = getIntent();
        this.jwtToken = intent.getStringExtra("jwtToken");
        this.sessionName = intent.getStringExtra("sessionName");
        this.userName = intent.getStringExtra("userName");
        this.domain = intent.getStringExtra("domain");
        this.startingWaitingMessage = intent.getStringExtra("waitingMessage");
        this.primaryUserSpeciality = intent.getStringExtra("primaryUserSpeciality");
        waitingMessageTextView.setText(this.startingWaitingMessage);
    }

    private void initializeViews() {
        progressBar = findViewById(getResourceId(this, ID, "progressBar"));
        primaryVideoView = findViewById(getResourceId(this, ID, "primary_video_view"));
        thumbnailVideoView = findViewById(getResourceId(this, ID, "thumbnail_video_view"));
        secondaryThumbnailVideoView = findViewById(getResourceId(this, ID, "secondary_thumbnail_video_view"));
        waitingMessageTextView = findViewById(getResourceId(this, ID, "waiting_message_textview"));
        videoControls = findViewById(getResourceId(this, ID, "video_control"));
        disconnectActionFab = findViewById(getResourceId(this, ID, "disconnect_action_fab"));
        switchCameraActionFab = findViewById(getResourceId(this, ID, "switch_camera_action_fab"));
        localVideoActionFab = findViewById(getResourceId(this, ID, "local_video_action_fab"));
        muteActionFab = findViewById(getResourceId(this, ID, "mute_action_fab"));
        speakerActionFab = findViewById(getResourceId(this, ID, "speaker_action_fab"));
        chatActionFab = findViewById(getResourceId(this, ID, "icon_chat"));
        CallEndFabAction = findViewById(getResourceId(this, ID, "icon_call_end"));
        timerTextView = findViewById(getResourceId(this, ID, "timerTextView"));
        specialityDoctorTextView = findViewById(getResourceId(this, ID, "specialityDoctor"));
        NameDoctorTextView = findViewById(getResourceId(this, ID, "NameDoctor"));

        videoControls.setVisibility(View.GONE);
        switchCameraActionFab.setVisibility(View.GONE);
    }

    private void setupActionFabListeners() {
        chatActionFab.setOnClickListener(v -> showChatActivity());
        CallEndFabAction.setOnClickListener(v -> finish());
        disconnectActionFab.setOnClickListener(v -> finish());
        switchCameraActionFab.setOnClickListener(v -> ZoomVideoSDK.getInstance().getVideoHelper().switchCamera());

        localVideoActionFab.setOnClickListener(v -> {
            if (ZoomVideoSDK.getInstance().isInSession()) {
                if (shouldVideoBeOn) {
                    shouldVideoBeOn = false;
                    ZoomVideoSDK.getInstance().getVideoHelper().stopVideo();
                } else {
                    shouldVideoBeOn = true;
                    ZoomVideoSDK.getInstance().getVideoHelper().startVideo();
                }
            }
        });

        muteActionFab.setOnClickListener(v -> {
            ZoomVideoSDKUser myUser = ZoomVideoSDK.getInstance().getSession().getMySelf();
            if (myUser != null && myUser.getAudioStatus() != null) {
                if (myUser.getAudioStatus().isMuted()) {
                    ZoomVideoSDK.getInstance().getAudioHelper().unMuteAudio(myUser);
                } else {
                    ZoomVideoSDK.getInstance().getAudioHelper().muteAudio(myUser);
                }
            }
        });

        speakerActionFab.setOnClickListener(v -> {
            audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
            updateSpeakerUI();
        });

        secondaryThumbnailVideoView.setOnClickListener(v -> {
            if (primaryUser != null && secondaryThumbnailUser != null) {
                switchPrimaryAndSecondaryView();
            }
        });
    }

    private void updateSpeakerUI() {
        if (audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn()) {
            speakerActionFab.setImageDrawable(ContextCompat.getDrawable(this, getResourceId(this, DRAWABLE, "ic_microphone_icon")));
        } else if (audioManager.isSpeakerphoneOn()) {
            speakerActionFab.setImageDrawable(ContextCompat.getDrawable(this, getResourceId(this, DRAWABLE, "ic_volume_on")));
        } else {
            speakerActionFab.setImageDrawable(ContextCompat.getDrawable(this, getResourceId(this, DRAWABLE, "ic_volume_off")));
        }
    }

    private void showChatActivity() {
        Intent intent = new Intent(SessionActivity.this, ChatActivity.class);
        intent.putExtra("chat_history", (Serializable) chatMessages);
        startActivity(intent);
    }

    // ----------------- LIFECYCLE & PERMISSION METHODS -----------------

    @Override
    protected void onStart() {
        super.onStart();
        // *** THIS IS THE FIX: Add the delegate back here. ***
        ZoomVideoSDK.getInstance().addListener(this);

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetReceiver, filter);
        refreshLocalVideo(); // Force video refresh on start
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLocalVideo(); // Also refresh on resume for max reliability
        if (ZoomVideoSDK.getInstance().isInSession()) {
            Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
            ZoomVideoSDK.getInstance().getVideoHelper().rotateMyVideo(display.getRotation());
        }
        startTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityInForeground.set(false);
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        enterPipMode();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // *** THIS IS THE FIX: Remove the delegate here. ***

        unregisterReceiver(headsetReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ZoomVideoSDK.getInstance().isInSession()) {
            if (this.secondaryThumbnailUser != null) this.secondaryThumbnailUser.getVideoCanvas().unSubscribe(this.secondaryThumbnailVideoView);
            if (this.thumbnailUser != null) this.thumbnailUser.getVideoCanvas().unSubscribe(this.thumbnailVideoView);
            if (this.primaryUser != null) this.primaryUser.getVideoCanvas().unSubscribe(this.primaryVideoView);
            ZoomVideoSDK.getInstance().leaveSession(false);
        }
        ZoomVideoSDK.getInstance().cleanup();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("jwtToken", jwtToken);
        outState.putString("sessionName", sessionName);
        outState.putString("userName", userName);
        outState.putString("domain", domain);
        outState.putString("startingWaitingMessage", startingWaitingMessage);
        outState.putString("primaryUserSpeciality", primaryUserSpeciality);
        outState.putBoolean("shouldVideoBeOn", shouldVideoBeOn);
        outState.putSerializable("chatMessages", (Serializable) chatMessages);
        if (startTime > 0) {
            outState.putLong("elapsedTime", System.currentTimeMillis() - startTime);
        }
    }

    // ----------------- ZOOM SDK INITIALIZATION & SESSION JOIN -----------------

    private void initializeSDK() {
        ZoomVideoSDKInitParams params = new ZoomVideoSDKInitParams();
        params.domain = (this.domain == null || this.domain.isEmpty()) ? "zoom.us" : this.domain;
        int initResult = ZoomVideoSDK.getInstance().initialize(this, params);
        if (initResult != ZoomVideoSDKErrors.Errors_Success) {
            Log.e("SessionActivity", "Initialize SDK error: " + initResult);
        }
    }

    private void joinSession(Bundle savedInstanceState) {
        ZoomVideoSDKSessionContext sessionContext = new ZoomVideoSDKSessionContext();
        sessionContext.sessionName = this.sessionName;
        sessionContext.userName = this.userName;
        sessionContext.token = this.jwtToken;
        sessionContext.audioOption = new ZoomVideoSDKAudioOption();
        sessionContext.audioOption.connect = true;
        sessionContext.audioOption.mute = true;
        sessionContext.videoOption = new ZoomVideoSDKVideoOption();
        sessionContext.videoOption.localVideoOn = false;

        ZoomVideoSDK.getInstance().joinSession(sessionContext);

        if (savedInstanceState != null) {
            startTime = savedInstanceState.getLong("startTime", System.currentTimeMillis());
        } else {
            startTime = System.currentTimeMillis();
        }
        startTimer();
    }

    // ----------------- ZOOM SDK DELEGATE CALLBACKS -----------------

    @Override
    public void onSessionJoin() {
        this.progressBar.setVisibility(View.GONE);
        this.videoControls.setVisibility(View.VISIBLE);
        this.waitingMessageTextView.setVisibility(View.VISIBLE);
        this.disconnectActionFab.setVisibility(View.GONE);
        ZoomVideoSDKUser myUser = ZoomVideoSDK.getInstance().getSession().getMySelf();
        if (myUser == null) return;
        this.thumbnailVideoView.setVisibility(View.VISIBLE);
        this.thumbnailUser = myUser;
        myUser.getVideoCanvas().subscribe(this.thumbnailVideoView, ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan);
        if (myUser.getAudioStatus().getAudioType() == ZoomVideoSDKAudioStatus.ZoomVideoSDKAudioType.ZoomVideoSDKAudioType_None) {
            ZoomVideoSDK.getInstance().getAudioHelper().startAudio();
        }
    }

    @Override
    public void onUserJoin(ZoomVideoSDKUserHelper userHelper, List<ZoomVideoSDKUser> userList) {
        for (ZoomVideoSDKUser user : userList) {
            if (this.primaryUser == null) {
                this.primaryUser = user;
                this.primaryUserSpeciality = user.isHost() ? "Practitioner/Doctor" : "";
                this.waitingMessageTextView.setVisibility(View.GONE);
                this.NameDoctorTextView.setText(user.getUserName());
                this.specialityDoctorTextView.setText(this.primaryUserSpeciality);
                user.getVideoCanvas().subscribe(this.primaryVideoView, ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan);
            } else if (this.secondaryThumbnailUser == null) {
                this.secondaryThumbnailVideoView.setVisibility(View.VISIBLE);
                this.secondaryThumbnailUser = user;
                user.getVideoCanvas().subscribe(this.secondaryThumbnailVideoView, ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan);
            }
        }
    }

    @Override
    public void onUserLeave(ZoomVideoSDKUserHelper userHelper, List<ZoomVideoSDKUser> userList) {
        for (ZoomVideoSDKUser user : userList) {
            if (this.primaryUser != null && this.primaryUser.getUserID().equals(user.getUserID())) {
                user.getVideoCanvas().unSubscribe(this.primaryVideoView);
                this.primaryUser = null;
                if (this.secondaryThumbnailUser != null) {
                    switchPrimaryAndSecondaryView();
                } else {
                    this.waitingMessageTextView.setVisibility(View.VISIBLE);
                }
            } else if (this.secondaryThumbnailUser != null && this.secondaryThumbnailUser.getUserID().equals(user.getUserID())) {
                user.getVideoCanvas().unSubscribe(this.secondaryThumbnailVideoView);
                this.secondaryThumbnailVideoView.setVisibility(View.GONE);
                this.secondaryThumbnailUser = null;
            }
        }
    }

    @Override
    public void onUserVideoStatusChanged(ZoomVideoSDKVideoHelper videoHelper, List<ZoomVideoSDKUser> userList) {
        ZoomVideoSDKUser myUser = ZoomVideoSDK.getInstance().getSession().getMySelf();
        if (myUser == null) return;
        for (ZoomVideoSDKUser user : userList) {
            if (myUser.getUserID().equals(user.getUserID())) {
                int icon = shouldVideoBeOn ? getResourceId(this, DRAWABLE, "icon_camera") : getResourceId(this, DRAWABLE, "icon_cross_camera");
                localVideoActionFab.setImageDrawable(ContextCompat.getDrawable(this, icon));
                switchCameraActionFab.setVisibility(shouldVideoBeOn ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onShareNetworkStatusChanged(ZoomVideoSDKNetworkStatus shareNetworkStatus, boolean isSendingShare) {

    }

    @Override
    public void onUserAudioStatusChanged(ZoomVideoSDKAudioHelper audioHelper, List<ZoomVideoSDKUser> userList) {
        ZoomVideoSDKUser myUser = ZoomVideoSDK.getInstance().getSession().getMySelf();
        if (myUser == null) return;
        for (ZoomVideoSDKUser user : userList) {
            if (myUser.getUserID().equals(user.getUserID())) {
                boolean isMuted = user.getAudioStatus().isMuted();
                int icon = isMuted ? getResourceId(this, DRAWABLE, "icon_mute_cross") : getResourceId(this, DRAWABLE, "icon_microphone");
                muteActionFab.setImageDrawable(ContextCompat.getDrawable(this, icon));
            }
        }
    }

    @Override
    public void onUserShareStatusChanged(ZoomVideoSDKShareHelper shareHelper, ZoomVideoSDKUser userInfo, ZoomVideoSDKShareStatus status) {

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
        String content = messageItem.getContent();
        String senderName = messageItem.getSenderUser().getUserName();
        String fileName;
        String mimType;
        String documentId;
        boolean isAttachmentMessage;
        if (content.contains("https://fileupload.bupa.com.sa")) {
            content = content.replace(" ", "").replace("\n", "");
            String[] parts = content.split("#");
            isAttachmentMessage = true;
            documentId = parts[1];
            fileName = parts[2];
            mimType = parts[3];

            JSONObject filedata = new JSONObject();
            try {
                filedata.put("documentId", parts[1]);
                filedata.put("fileName", parts[2]);
                filedata.put("fileMimetype", parts[3]);
            } catch (JSONException e) {
                Log.e("ChatAdapter", "Error parsing file URL", e);
            }
        } else {
            fileName = null;
            mimType = null;
            documentId = null;
            isAttachmentMessage = false;
        }


        String finalContent = content;
        runOnUiThread(() -> chatMessages.add(new ChatMessage(senderName, finalContent, isAttachmentMessage, documentId, mimType, fileName, false )));
        Intent intent = new Intent("new-chat-message");
        intent.putExtra("senderName", senderName);
        intent.putExtra("content", content);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onChatDeleteMessageNotify(ZoomVideoSDKChatHelper chatHelper, String msgID, ZoomVideoSDKChatMessageDeleteType deleteBy) {

    }

    @Override
    public void onChatPrivilegeChanged(ZoomVideoSDKChatHelper chatHelper, ZoomVideoSDKChatPrivilegeType currentPrivilege) {

    }

    // ----------------- UTILITY METHODS -----------------

    private void refreshLocalVideo() {
        new Handler().postDelayed(() -> {
            if (!ZoomVideoSDK.getInstance().isInSession()) return;
            ZoomVideoSDKUser myUser = ZoomVideoSDK.getInstance().getSession().getMySelf();
            if (myUser == null) return;
            Log.d("VideoFix", "Refreshing video. Should be on: " + shouldVideoBeOn);
            if (shouldVideoBeOn) {
                myUser.getVideoCanvas().unSubscribe(thumbnailVideoView);
                myUser.getVideoCanvas().subscribe(thumbnailVideoView, ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan);
            } else {
                ZoomVideoSDK.getInstance().getVideoHelper().stopVideo();
            }
        }, 500);
    }

    private void switchPrimaryAndSecondaryView() {
        if (primaryUser == null || secondaryThumbnailUser == null) return;
        ZoomVideoSDKUser tempUser = this.primaryUser;
        this.primaryUser = this.secondaryThumbnailUser;
        this.secondaryThumbnailUser = tempUser;
        this.primaryUser.getVideoCanvas().subscribe(this.primaryVideoView, ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan);
        this.secondaryThumbnailUser.getVideoCanvas().subscribe(this.secondaryThumbnailVideoView, ZoomVideoSDKVideoAspect.ZoomVideoSDKVideoAspect_PanAndScan);
    }

    private void startTimer() {
        if (handler != null) handler.removeCallbacks(runnable);
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (startTime == 0) return;
                long elapsedTime = System.currentTimeMillis() - startTime;
                int seconds = (int) (elapsedTime / 1000) % 60;
                int minutes = (int) ((elapsedTime / (1000 * 60)) % 60);
                int hours = (int) ((elapsedTime / (1000 * 60 * 60)) % 24);
                timerTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private boolean checkPermissionForCameraAndMicrophone() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForCameraAndMicrophone() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, CAMERA_MIC_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                joinSession(null); // Pass null as we are joining fresh
            } else {
                Toast.makeText(this, "Permissions for camera and microphone are required.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void enterPipMode() {
        if (SDK_INT >= Build.VERSION_CODES.O && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && ZoomVideoSDK.getInstance().isInSession()) {
            Rational aspectRatio = new Rational(9, 16);
            PictureInPictureParams.Builder pipBuilder = new PictureInPictureParams.Builder();
            pipBuilder.setAspectRatio(aspectRatio);
            enterPictureInPictureMode(pipBuilder.build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        videoControls.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
    }

    // --- All other unused but required delegate overrides can be left empty ---
    @Override public void onSessionLeave() {}

    @Override
    public void onSessionLeave(ZoomVideoSDKSessionLeaveReason reason) {

    }

    @Override public void onError(int errorCode) {}
    @Override public void onUserHostChanged(ZoomVideoSDKUserHelper userHelper, ZoomVideoSDKUser userInfo) {}

    @Override
    public void onUserManagerChanged(ZoomVideoSDKUser user) {

    }

    @Override
    public void onUserNameChanged(ZoomVideoSDKUser user) {

    }

    @Override
    public void onUserActiveAudioChanged(ZoomVideoSDKAudioHelper audioHelper, List<ZoomVideoSDKUser> list) {

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
    // ... many more empty overrides
}
