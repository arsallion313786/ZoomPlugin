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

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import us.zoom.sdk.ZoomVideoSDK;

public class ZoomVideo extends CordovaPlugin {
    private CallbackContext callbackContext;
    private static CallbackContext fileUploadCallbackContext;
    private static CallbackContext sendDocumentMetaDataContext;
    private static CallbackContext addDownloadFileListenerContext;
    private static CallbackContext sendFileDataContext;
    private CordovaInterface cordova;

    private String jwtToken;
    private String sessionName;
    private String userName;
    private String domain;
    private String waitingMessage;
    private String primaryUserSpeciality;
    private String documentID;
    private String fileName;
    private String fileMimetype;
    private String BinaryData;
    private boolean isBase64String;
    private String DownloadFileName;
    private String DownloadFileMimeType;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("openSession")) {
            this.callbackContext = callbackContext;
            this.openSession(args);
        }
        else if (action.equals("addFileUploadListener")) {
            fileUploadCallbackContext = callbackContext;
        }
        else if(action.equals("sendDocumentMetaData")){
            sendDocumentMetaDataContext = callbackContext;
            this.sendFileURL(args);
        }else if (action.equals("addDownloadFileListener")) {
            addDownloadFileListenerContext = callbackContext;
            } else if(action.equals("sendFileData")){
            sendFileDataContext = callbackContext;
            this.ShowDocument(args);
        }
 	    else if (action.equals("handleSuccessErrorMessage")) {
            this.HandleSuccessErrorMessage(args);
        }
        return true;
    }

    private void sendFileURL(final JSONArray args) {
        try {
            this.documentID = args.getString(0);
            this.fileName = args.getString(1);
            this.fileMimetype = args.getString(2);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        String URL = "https://fileupload.bupa.com.sa/#"+this.documentID+"#"+this.fileName+"#"+this.fileMimetype;
        sendURL(URL);

    }

    public void sendURL(String URL){
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BottomSheetChat.sendMessage(URL);
            }
        });
    }
    public void ShowDocument(final JSONArray args) {
        try {
            this.DownloadFileName = args.getString(0);
            this.DownloadFileMimeType = args.getString(1);
            this.BinaryData = args.getString(2);
            this.isBase64String = args.getBoolean(3);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        //SessionActivity.chatBottomSheetFragment

        showDoc(DownloadFileName,DownloadFileMimeType,BinaryData, isBase64String);
        //BottomSheetChat.showDocument(this.DownloadFileName,this.DownloadFileMimeType,this.BinaryData);
    }

    public void HandleSuccessErrorMessage(final JSONArray args) {
        try {
            String alertType = args.getString(0);
            String alertMessage = args.getString(1);

            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(cordova.getActivity(), alertMessage, Toast.LENGTH_LONG).show();
                }
            });
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public  void showDoc(String downloadFileName, String downloadFileMimeType, String binaryData, boolean isBase64String){
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BottomSheetChat.showDocument(downloadFileName, downloadFileMimeType, binaryData,isBase64String);
            }
        });
    }


    private void openSession(final JSONArray args) {

        try {
            this.jwtToken = args.getString(0);
            this.sessionName = args.getString(1);
            this.userName = args.getString(2);
            this.domain = args.getString(3);
            this.waitingMessage = args.getString(5);
            this.primaryUserSpeciality = args.getString(4);

            final String jwtToken = this.jwtToken;
            final String sessionName = this.sessionName;
            final String userName = this.userName;
            final String domain = this.domain;
            final String waitingMessage = this.waitingMessage;
            final String primaryUserSpeciality = this.primaryUserSpeciality;

            final CordovaPlugin that = this;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    Intent intentZoomVideo = new Intent(that.cordova.getActivity().getBaseContext(), SessionActivity.class);
                    intentZoomVideo.putExtra("jwtToken", jwtToken);
                    intentZoomVideo.putExtra("sessionName", sessionName);
                    intentZoomVideo.putExtra("userName", userName);
                    intentZoomVideo.putExtra("domain", domain);
                    intentZoomVideo.putExtra("waitingMessage", waitingMessage);
                    intentZoomVideo.putExtra("primaryUserSpeciality", primaryUserSpeciality);

                    that.cordova.startActivityForResult(that, intentZoomVideo, 0);
                }
            });
        } catch (JSONException e) {
            LOG.e("ROOM", "Invalid JSON string: ", e);
        }
    }

    public static void registerDownloadFileListener(JSONObject fileData) {
        // Example of registering a listener for file upload events

        Log.v("", "----- file json data " + fileData);
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, fileData);
        pluginResult.setKeepCallback(true);
        if (addDownloadFileListenerContext != null) {
            addDownloadFileListenerContext.sendPluginResult(pluginResult);
        }

    }

    public static void registerFileUploadListener(JSONObject fileData) {
        // Example of registering a listener for file upload events

        Log.v("", "----- file json data " + fileData);
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, fileData);
        pluginResult.setKeepCallback(true);
        if (fileUploadCallbackContext != null) {
            fileUploadCallbackContext.sendPluginResult(pluginResult);
        }

    }

    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putString("jwtToken", this.jwtToken);
        state.putString("sessionName", this.sessionName);
        state.putString("userName", this.userName);
        state.putString("domain", this.domain);
        state.putString("waitingMessage", this.waitingMessage);
        return state;
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.jwtToken = state.getString("jwtToken");
        this.sessionName = state.getString("sessionName");
        this.userName = state.getString("userName");
        this.domain = state.getString("domain");
        this.waitingMessage = state.getString("waitingMessage");
        this.callbackContext = callbackContext;
    }
}
