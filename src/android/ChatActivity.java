package cordova.plugin.zoomvideo;

import android.app.Activity;
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
import androidx.core.content.FileProvider;
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


    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
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

                if (senderName != null && content != null) {
                    addMessageToChat(new ChatMessage(senderName, content));
                }
            }
        }
    };

    private BroadcastReceiver closeChatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check if the received action is the one we are expecting.
            if ("com.zoom.plugin.CLOSE_CHAT_ACTIVITY".equals(intent.getAction())) {
                // If it is, simply finish the ChatActivity.
                Log.d("ChatActivity", "Received close command. Finishing activity.");
                finish();
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
        closeButton.setOnClickListener(v -> {
            // --- THIS IS THE FIX ---

            // 1. Create an intent with a custom action.
            // This is the message we will send back to the SessionActivity.
            Intent intent = new Intent("com.zoom.plugin.RETURN_TO_FULL_SCREEN");

            // 2. Send the broadcast. Any part of the app listening for this action will receive it.
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            // 3. Now, finish the activity as before.
            finish();
        });
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

        IntentFilter filter2 = new IntentFilter("com.zoom.plugin.CLOSE_CHAT_ACTIVITY");
        LocalBroadcastManager.getInstance(this).registerReceiver(closeChatReceiver, filter2);
    }


    // 3. Unregister the receiver when the activity stops to prevent memory leaks
    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeChatReceiver);
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
        } catch (android.content.ActivityNotFoundException ex) {
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
        //ZoomVideo.showDocumentPreview(fileName, mimeType, base64);

        // This is where you send the file for upload
        //ZoomVideo.showDocumentPreview(fileName, mimeType, base64);

        try {
            JSONObject filedata = new JSONObject();
            filedata.put("base64", base64);
            filedata.put("fileName", fileName);
            filedata.put("fileMimetype", mimeType);
            ZoomVideo.registerFileUploadListener(filedata);
        } catch (JSONException e) {
            Log.e("ChatActivity", "Error creating JSON for file upload", e);
        }
    }

    // --- All your helper methods for file handling can be moved here ---
    // (getStringFile, getFileFromUri)

    public void openDocumentFromBase64(String base64Data, String fileName, String mimeType) {
        if (base64Data == null || base64Data.isEmpty() || fileName == null || mimeType == null) {
            Log.e("FileViewer", "Invalid arguments provided to openDocumentFromBase64.");
            // Optionally show an error Toast to the user
            // Toast.makeText(this, "Cannot open file: Invalid data.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 1. Decode the Base64 string into a byte array.
            byte[] fileAsBytes = Base64.decode(base64Data, Base64.DEFAULT);

            // 2. Create a temporary file in the app's cache directory.
            File tempFile = new File(getCacheDir(), fileName);

            // 3. Write the byte array to the temporary file.
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileAsBytes);
            }

            // 4. Get the secure content URI for the file using the FileProvider.
            //    The authority must match exactly what you defined in AndroidManifest.xml.
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".zoom.provider",
                    tempFile
            );

            // 5. Create a new Intent to view the content.
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setDataAndType(contentUri, mimeType);

            // 6. Grant temporary read permission to the app that will handle the intent.
            //    This is the most critical part for security and functionality.
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //    Optional: This flag prevents the viewer app from being in the back stack history.
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            // 7. Start the activity. The Android system will find an app to open the file.
            startActivity(viewIntent);

        } catch (Exception e) {
            Log.e("FileViewer", "Error opening file from Base64 string.", e);
            // This catch block will handle errors like:
            // - No app installed that can view the file type.
            // - FileProvider not configured correctly.
            // - Issues writing the temporary file.
            // Toast.makeText(this, "Error: Could not open file.", Toast.LENGTH_SHORT).show();
        }
    }

    // In ChatActivity.java

    public String getStringFile(File f) {
        try (InputStream inputStream = new FileInputStream(f);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            // 1. Use a small, efficient buffer to read the file in chunks.
            byte[] buffer = new byte[8192]; // 8KB buffer is a good standard size
            int bytesRead;

            // 2. Read the file chunk by chunk until the end is reached.
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            // 3. Get the complete byte array from the output stream.
            byte[] fileBytes = byteArrayOutputStream.toByteArray();

            // 4. CRITICAL FIX: Use Base64.encodeToString() to correctly encode the raw bytes.
            return Base64.encodeToString(fileBytes, Base64.DEFAULT);

        } catch (IOException e) {
            Log.e("FileHandling", "Failed to read file and encode to Base64", e);
            e.printStackTrace();
            return null; // Return null on error
        }
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
