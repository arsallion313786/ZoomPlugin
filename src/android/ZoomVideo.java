package cordova.plugin.zoomvideo;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

// Note: us.zoom.sdk.ZoomVideoSDK import is removed as it's not used in this file,
// but it's fine if your original file needs it for other reasons.

public class ZoomVideo extends CordovaPlugin {
    // Callbacks for JS listeners
    private CallbackContext callbackContext;
    private static CallbackContext fileUploadCallbackContext;
    private static CallbackContext sendDocumentMetaDataContext;
    private static CallbackContext addDownloadFileListenerContext;
    private static CallbackContext sendFileDataContext;

    // Static instance for access from other classes (like ChatActivity)
    private static ZoomVideo instance;

    // Member variables for state
    private String jwtToken;
    private String sessionName;
    private String userName;
    private String domain;
    private String waitingMessage;
    private String primaryUserSpeciality;
    private String documentID;
    private String fileName;
    private String fileMimetype;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // Set the static instance when the plugin is initialized
        instance = this;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        // Your execute method remains largely the same, but we will use the static instance
        // for better consistency.
        if ("openSession".equals(action)) {
            this.callbackContext = callbackContext;
            this.openSession(args);
            return true;
        } else if ("addFileUploadListener".equals(action)) {
            fileUploadCallbackContext = callbackContext;
            return true;
        } else if ("sendDocumentMetaData".equals(action)) {
            sendDocumentMetaDataContext = callbackContext;
            this.sendFileURL(args);
            return true;
        } else if ("addDownloadFileListener".equals(action)) {
            addDownloadFileListenerContext = callbackContext;
            return true;
        } else if ("sendFileData".equals(action)) {
            sendFileDataContext = callbackContext;
            this.ShowDocument(args);
            return true;
        } else if ("handleSuccessErrorMessage".equals(action)) {
            this.HandleSuccessErrorMessage(args);
            return true;
        }
        return false; // Action not found
    }

    // --- Methods related to Chat and File Upload (UNCHANGED) ---

    private void sendFileURL(final JSONArray args) {
        try {
            this.documentID = args.getString(0);
            this.fileName = args.getString(1);
            this.fileMimetype = args.getString(2);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        String URL = "https://fileupload.bupa.com.sa/#" + this.documentID + "#" + this.fileName + "#" + this.fileMimetype;
        sendURL(URL);
    }

    public void sendURL(String URL) {
        // Get the application context from Cordova
        Context context = cordova.getActivity().getApplicationContext();

        // Create an Intent with a custom action string
        Intent intent = new Intent("custom-message-event");

        // Add the URL as an extra
        intent.putExtra("message", URL);

        // Send the broadcast
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void registerFileUploadListener(JSONObject fileData) {
        if (fileUploadCallbackContext != null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, fileData);
            pluginResult.setKeepCallback(true);
            fileUploadCallbackContext.sendPluginResult(pluginResult);
        }
    }

    // --- Methods for Document Preview (UPDATED) ---

    public void ShowDocument(final JSONArray args) {
        try {
            String downloadFileName = args.getString(0);
            String downloadFileMimeType = args.getString(1);
            String binaryData = args.getString(2);
            // boolean isBase64String = args.getBoolean(3); // This variable wasn't used

            // Call the new, robust method for showing the document
            showDocumentPreview(downloadFileName, downloadFileMimeType, binaryData);

        } catch (JSONException e) {
            if (sendFileDataContext != null) {
                sendFileDataContext.error("Invalid arguments for ShowDocument: " + e.getMessage());
            }
        }
    }

    /**
     * This is the static bridge method for any native class (e.g., ChatActivity) to call.
     * It creates a temporary file and uses the cordova-outsystems-file-viewer plugin
     * via the JavaScript bridge to display it.
     */
    public static void showDocumentPreview(String fileName, String mimeType, String binaryData) {
        SessionActivity activity = SessionActivity.getActiveInstance();

//        if (activity != null) {
//            // Run the UI operation on the main thread of that activity
//            activity.runOnUiThread(() -> {
//                activity.showAttachmentViewer(fileName, mimeType, binaryData);
//            });
//        } else {
//            Log.e("ZoomVideoPlugin", "SessionActivity is not active, cannot show document preview.");
//        }
    }

    // --- Other Plugin Methods (UNCHANGED) ---

    public void HandleSuccessErrorMessage(final JSONArray args) {
        try {
            String alertMessage = args.getString(1);
            cordova.getActivity().runOnUiThread(() -> Toast.makeText(cordova.getActivity(), alertMessage, Toast.LENGTH_LONG).show());
        } catch (JSONException e) {
            // It's better to log the error than to crash the app
            Log.e("ZoomVideo", "Error processing toast message arguments.", e);
        }
    }

    private void openSession(final JSONArray args) {
        try {
            this.jwtToken = args.getString(0);
            this.sessionName = args.getString(1);
            this.userName = args.getString(2);
            this.domain = args.getString(3);
            this.waitingMessage = args.getString(5);
            this.primaryUserSpeciality = args.getString(4);

            final CordovaPlugin that = this;
            cordova.getThreadPool().execute(() -> {
                Intent intentZoomVideo = new Intent(that.cordova.getActivity().getBaseContext(), SessionActivity.class);
                intentZoomVideo.putExtra("jwtToken", jwtToken);
                intentZoomVideo.putExtra("sessionName", sessionName);
                intentZoomVideo.putExtra("userName", userName);
                intentZoomVideo.putExtra("domain", domain);
                intentZoomVideo.putExtra("waitingMessage", waitingMessage);
                intentZoomVideo.putExtra("primaryUserSpeciality", primaryUserSpeciality);
                that.cordova.startActivityForResult(that, intentZoomVideo, 0);
            });
        } catch (JSONException e) {
            LOG.e("ZoomVideo", "Invalid JSON string for openSession: ", e);
            callbackContext.error("Invalid JSON arguments for openSession.");
        }
    }

    public static void registerDownloadFileListener(JSONObject fileData) {
        if (addDownloadFileListenerContext != null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, fileData);
            pluginResult.setKeepCallback(true);
            addDownloadFileListenerContext.sendPluginResult(pluginResult);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putString("jwtToken", this.jwtToken);
        state.putString("sessionName", this.sessionName);
        state.putString("userName", this.userName);
        state.putString("domain", this.domain);
        state.putString("waitingMessage", this.waitingMessage);
        return state;
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.jwtToken = state.getString("jwtToken");
        this.sessionName = state.getString("sessionName");
        this.userName = state.getString("userName");
        this.domain = state.getString("domain");
        this.waitingMessage = state.getString("waitingMessage");
        this.callbackContext = callbackContext;
    }
}
