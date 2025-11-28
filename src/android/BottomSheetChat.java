package cordova.plugin.zoomvideo;

import static android.app.Activity.RESULT_OK;

import static com.zipow.cmmlib.AppUtil.getApplicationContext;
import static cordova.plugin.zoomvideo.SessionActivity.getResourceId;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


import us.zoom.sdk.ZoomVideoSDK;


public class BottomSheetChat extends BottomSheetDialogFragment {

    RecyclerView recyclerViewChat;
    static EditText editTextMessage;
    static Button buttonSend;
    static Button buttonClose;
    static WebView webView;
    Button btnUpload;
    List<ChatMessage> chatMessages = new ArrayList<>();
    ChatAdapter chatAdapter = new ChatAdapter(chatMessages);
    private static Context context;
    final String LAYOUT = "layout";
    final String STRING = "string";
    final String DRAWABLE = "drawable";
    final String ID = "id";
    private Activity sessionActivity;
    private static final int FILE_SELECT_CODE = 0;

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    public BottomSheetChat (List<ChatMessage> chatMessages,Activity sessionActivity) {
        this.chatMessages = chatMessages;
        this.sessionActivity = sessionActivity;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getActivity();
        View view = inflater.inflate(getResourceId(context,LAYOUT,("chat_popup_window")), container,
                false);
        recyclerViewChat = view.findViewById(getResourceId(context,ID,("recyclerViewChat")));
        editTextMessage = view.findViewById(getResourceId(context,ID,("editTextMessage")));
        buttonSend = view.findViewById(getResourceId(context,ID,("buttonSend")));
        buttonClose = view.findViewById(getResourceId(context,ID,("close_button")));
        webView = view.findViewById(getResourceId(context,ID,("webview")));
        btnUpload = view.findViewById(getResourceId(context,ID,("uploadButton")));
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(getContext()));
        //chatAdapter = new ChatAdapter();
        recyclerViewChat.setAdapter(chatAdapter);

        return view;


    }
    /*public void updateChatMessages(List<ChatMessage> newMessages) {
        chatMessages.clear();

        chatMessages.addAll(newMessages);
        chatAdapter.notifyDataSetChanged();
    }
*/
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showChatPopup();
    }

    public void addMessage(ChatMessage message) {
        chatAdapter.addMessage(message);
        chatAdapter.notifyDataSetChanged();

    }

    public boolean isBottomSheetVisible() {
        View bottomSheet = getDialog().findViewById(getResourceId(context,LAYOUT,("chat_popup_window")));
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        int state = bottomSheetBehavior.getState();
        return (state == BottomSheetBehavior.STATE_EXPANDED || state == BottomSheetBehavior.STATE_COLLAPSED);
    }

    public static void sendMessage(String message){
        //message = editTextMessage.getText().toString();

        if (!message.isEmpty()) {
            editTextMessage.setText(message);
            // Send the message using Zoom Video SDK

            buttonSend.performClick();


            //ZoomVideoSDK.getInstance().getChatHelper().sendChatToAll(message);
            //editTextMessage.setText("");
        }
    }
    private void showChatPopup() {


        // Set up the RecyclerView and EditText


        // Initialize your chat adapter and set it to the RecyclerView
        //List<ChatMessage> chatMessages = new ArrayList<>();
        // ChatAdapter chatAdapter = new ChatAdapter(chatMessages);
        recyclerViewChat.setAdapter(chatAdapter);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(getContext()));

        // Handle sending messages
        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString();
            if (!message.isEmpty()) {
                // Send the message using Zoom Video SDK
                ZoomVideoSDK.getInstance().getChatHelper().sendChatToAll(message);
                editTextMessage.setText("");
            }
        });

        // Upload button click event
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionActivity.isOpenedGalleryForImage = true;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] mimeTypesss = {"image/jpeg", "image/png", "application/pdf"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypesss);
                startActivityForResult(Intent.createChooser(intent,"ChooseFile"), FILE_SELECT_CODE);
            }
        });
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.setVisibility(View.GONE);
                buttonClose.setVisibility(View.GONE);
            }
        });
        // Handle receiving messages
        /*ZoomVideoSDK.getInstance().getChatHelper().addListener(new ZoomVideoSDKChatHelperListener() {
            @Override
            public void onChatMessageReceived(ZoomVideoSDKChatMessage message) {
                runOnUiThread(() -> {
                    String content = message.getContent();
                    String senderName = message.getSenderUser().getUserName();
                    chatAdapter.addMessage(message);
                    chatAdapter.notifyDataSetChanged();
                    recyclerViewChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                });
            }
        });*/
    }


    public String getStringFile(File f) {
        InputStream inputStream = null;
        String encodedFile= "", lastVal;
        try {
            inputStream = new FileInputStream(f.getAbsolutePath());

            byte[] buffer = new byte[(int) f.length()];//specify the size to allow
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output64.write(buffer, 0, bytesRead);
            }
            output64.close();
            encodedFile =  output.toString();
        }
        catch (FileNotFoundException e1 ) {
            e1.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        lastVal = encodedFile;
        return lastVal;
    }

    private File getFileFromUri(Uri uri) {
        assert getApplicationContext() != null;
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (displayNameIndex != -1) {
                String displayName = cursor.getString(displayNameIndex);
                // Create a temporary file with the display name
                File file = new File(getApplicationContext().getCacheDir(), displayName);
                try {
                    InputStream inputStream = contentResolver.openInputStream(uri);
                    if (inputStream != null) {
                        OutputStream outputStream = new FileOutputStream(file);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                        outputStream.close();
                        inputStream.close();
                        return file;
                    }
                } catch (IOException e) {
                    e.printStackTrace(); // Handle exceptions
                }
            }
            cursor.close();
        }
        return null;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SessionActivity.isOpenedGalleryForImage = false;
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            if (data != null) {


                Uri uri = data.getData();
                if (uri != null) {

                    String filePath = getPathFromUri(uri);
                    Log.d("fileuploadURI", ":"+uri);
                    // Handle file upload to your server here
                    //File file = new File(filePath);
                    File file =  this.getFileFromUri(uri);
                    //new File(data.getData().getPath() + "/" + df.getName());
                    assert file != null;
                    if(file.exists()){
                        Log.d("file exists", "true");
                    }
                    if(file.canRead()){
                        Log.d("file can read", "true");
                    }

                    //Log.d("file canonicalpath", file.getCanonicalPath());
//                    FileInputStream fileInputStream = null;
//                    byte[] bytes = new byte[(int) file.length()];
//                    try {
//                        fileInputStream = new FileInputStream(file);
//                        fileInputStream.read(bytes);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }

                    // *** FILE SIZE CHECK ADDED HERE ***
                    if (file.length() > MAX_FILE_SIZE_BYTES) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("File Too Large")
                                .setMessage("The selected file exceeds the 5MB size limit. Please choose a smaller file.")
                                .setPositiveButton(android.R.string.ok, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                        // Stop further processing
                        return;
                    }

                    String base64 =  this.getStringFile(file);
                    //Base64.encodeToString(bytes, Base64.DEFAULT);
                    /* addobject */

                    String extension = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
                    extension = extension.replace(".", "");
                    JSONObject filedata = new JSONObject();
                    try {
                        filedata.put("base64", base64);
                        filedata.put("fileName", file.getName());
                        filedata.put("fileMimetype", MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }


                    Log.v("", "----- file json data " + filedata);
                    /* end object  */
                    //DocumentFile df = DocumentFile.fromSingleUri(this.context, data.getData());
                    // assert df != null;
                    //Log.d("file type", Objects.requireNonNull(df.getType()));
                    //ZoomVideoSDK.getInstance().getChatHelper().sendChatToAll("data:"+df.getType() + ";base64,"  +base64);
                    ZoomVideo.registerFileUploadListener(filedata);
                }
            }
        }
    }
    private String getPathFromUri(Uri uri) {
        // Implement method to get file path from URI
        return DocumentsContract.getDocumentId(uri);
    }


    public static void showDocument(String DownloadFileName, String DownloadFileMimeType, String BinaryData,boolean isbase64){
        // Show the bottom sheet

        //webView.getSettings().setJavaScriptEnabled(true);
        String base64Image = "data:"+DownloadFileMimeType+";base64," + BinaryData; // Ensure BinaryData is your base64 string without the data URL prefix
        String html = "<html><body style='margin:0;padding:0;'><img src='" + base64Image + "' style='width:100%;height:auto;'/></body></html>";
        webView.loadData(html, "text/html", "UTF-8");

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true); // Enable zoom controls
        webView.getSettings().setDisplayZoomControls(false); // Hide the zoom controls
        webView.getSettings().setSupportZoom(true); // Enable zoom support
        //webView.loadData(BinaryData, DownloadFileMimeType, "base64");
        if(webView != null){
            webView.setVisibility(View.VISIBLE);
            buttonClose.setVisibility(View.VISIBLE);
        }

    }

    private static Activity unwrap(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

}

