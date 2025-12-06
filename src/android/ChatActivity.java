package cordova.plugin.zoomvideo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.RelativeLayout;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import us.zoom.sdk.ZoomVideoSDK;

public class ChatActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 101;
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    // Declare all UI elements from your new layout
    private ImageButton closeButton;
    private Button uploadButton;
    private Button sendButton;
    private EditText messageEditText;
    private RecyclerView chatRecyclerView;

    List<ChatMessage> chatMessages = new ArrayList<>();
    ChatAdapter chatAdapter = new ChatAdapter(chatMessages);

    private RelativeLayout rootLayout; // The root layout of your activity


    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if ("custom-message-event".equals(action)) {
                // This is the existing logic for receiving a URL
                String message = intent.getStringExtra("message");
                if (message != null && messageEditText != null) {
                    messageEditText.setText(message);
                    if (sendButton != null) {
                        sendButton.performClick();
                    }
                }
            } else if ("new-chat-message".equals(action)) {
                // This is the NEW logic for receiving a chat message
                String senderName = intent.getStringExtra("senderName");
                String content = intent.getStringExtra("content");

                String fileName;
                String mimType;
                String documentId;
                boolean isAttachmentMessage;
                assert content != null;
                if (content.contains("https://fileupload.bupa.com.sa")) {
                    content = content.replace(" ", "").replace("\n", "");
                    String[] parts = content.split("#");
                    isAttachmentMessage = true;
                    documentId = parts[1];
                    fileName = parts[2];
                    mimType = parts[3];

                    boolean isDataFind = false;


                    for (int i = 0; i < chatMessages.size(); i++) {
                        ChatMessage msg = chatMessages.get(i);
                        if (msg.getIsAttachment() && msg.getIsUploading() && fileName.contains(msg.getFileName()) && msg.getAttachmentId() == null) {
                            // Found the placeholder message, now update it.
                            msg.updateAttachmentDetails(documentId, mimType);
                            isDataFind = true;
                            // Notify the adapter that this specific item has changed.
                            final int finalI = i;
                            runOnUiThread(() -> chatAdapter.notifyItemChanged(finalI));

                            return; // Exit the loop once updated
                        }
                    }

                    if (!isDataFind && senderName != null) {
                        addMessageToChat(new ChatMessage(senderName, content, true, documentId, mimType, fileName, false));
                    }


                } else {
                    fileName = null;
                    mimType = null;
                    documentId = null;
                    isAttachmentMessage = false;
                    if (senderName != null) {
                        addMessageToChat(new ChatMessage(senderName, content, isAttachmentMessage, documentId, mimType, fileName, false ));
                    }
                }


            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the new layout file
        setContentView(SessionActivity.getResourceId(this, "layout", "activity_chat"));


        List<ChatMessage> history = (List<ChatMessage>) getIntent().getSerializableExtra("chat_history");
        // As a fallback, create a new empty list if no history was passed
        this.chatMessages = Objects.requireNonNullElseGet(history, ArrayList::new);

        chatRecyclerView = findViewById(SessionActivity.getResourceId(this, "id", "recyclerViewChat"));
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initialize all views from the new layout
        closeButton = findViewById(SessionActivity.getResourceId(this, "id", "close_chat_button"));
        uploadButton = findViewById(SessionActivity.getResourceId(this, "id", "uploadButton"));
        sendButton = findViewById(SessionActivity.getResourceId(this, "id", "buttonSend"));
        messageEditText = findViewById(SessionActivity.getResourceId(this, "id", "editTextMessage"));



        // Set listeners for the buttons
        closeButton.setOnClickListener(v -> finish()); // Simply close the activity
        uploadButton.setOnClickListener(v -> openFilePicker());

        sendButton.setOnClickListener(v -> {
            // Logic to send a text message would go here
            String message = messageEditText.getText().toString();
            if (!message.isEmpty()) {
                // TODO: Implement your message sending logic
                ZoomVideoSDK.getInstance().getChatHelper().sendChatToAll(message);
                messageEditText.setText("");
            }
        });

        // TODO: Set up your RecyclerView adapter here
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("custom-message-event"); // For the URL
        filter.addAction("new-chat-message");   // For new chat messages
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, filter);
    }


    // 3. Unregister the receiver when the activity stops to prevent memory leaks
    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }

    private void addMessageToChat(ChatMessage chatMessage) {
        if (chatAdapter != null && chatRecyclerView != null) {
            chatAdapter.addMessage(chatMessage);
            chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File"), FILE_SELECT_CODE);
        } catch (ActivityNotFoundException ex) {
            // Handle case where no file picker is available
            Log.e("ChatActivity", "No file picker found on device.", ex);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                handleFileSelection(data.getData());
            }
        }
    }

    private void handleFileSelection(Uri uri) {
        File file = getFileFromUri(uri);
        if (file == null) {
            Log.e("ChatActivity", "Failed to get file from URI");
            return;
        }

        if (file.length() > MAX_FILE_SIZE_BYTES) {
            new AlertDialog.Builder(this)
                    .setTitle("File Too Large")
                    .setMessage("The selected file exceeds the 5MB size limit.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        String base64 = getStringFile(file);
        // Corrected to handle file names with no extension
        String fileName = file.getName();
        String extension = "";
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot >= 0) {
            extension = fileName.substring(lastDot + 1);
        }
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        if (mimeType == null) {
            mimeType = "*/*"; // A generic fallback
        }

        // We call back to the main Cordova plugin to handle the file viewer.
        // This logic remains correct.
        // ZoomVideo.showDocumentPreview(fileName, mimeType, base64);



        // This is where you send the file for upload
        try {
            JSONObject filedata = new JSONObject();
            filedata.put("base64", base64);
            filedata.put("fileName", fileName.replace(" ", ""));
            filedata.put("fileMimetype", mimeType);
            ZoomVideo.registerFileUploadListener(filedata);
            addMessageToChat(new ChatMessage("Me", "View Attachment", true, null, mimeType, fileName, true ));

        } catch (JSONException e) {
            Log.e("ChatActivity", "Error creating JSON for file upload", e);
        }
    }

    // --- All your helper methods for file handling can be moved here ---
    // (getStringFile, getFileFromUri)

    public String getStringFile(File f) {
        // ... (this method code is unchanged)
        InputStream inputStream;
        String encodedFile= "";
        try {
            inputStream = new FileInputStream(f.getAbsolutePath());
            byte[] buffer = new byte[(int) f.length()];
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output64.write(buffer, 0, bytesRead);
            }
            output64.close();
            encodedFile =  output.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encodedFile;
    }

    private File getFileFromUri(Uri uri) {
        // ... (this method code is unchanged)
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (displayNameIndex != -1) {
                String displayName = cursor.getString(displayNameIndex);
                File file = new File(getCacheDir(), displayName);
                try (InputStream inputStream = contentResolver.openInputStream(uri);
                     OutputStream outputStream = new FileOutputStream(file)) {
                    if (inputStream != null) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                        cursor.close();
                        return file;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return null;
    }
}
