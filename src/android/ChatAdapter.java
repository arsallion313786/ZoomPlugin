package cordova.plugin.zoomvideo;

import static cordova.plugin.zoomvideo.SessionActivity.getResourceId;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private Context context;
    final String LAYOUT = "layout";
    final String ID = "id";
    private List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(getResourceId(context, LAYOUT, "item_chat_message"), parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);

        // Always set the sender's name
        holder.senderTextView.setText(chatMessage.getSender());

        // --- LOGIC TO HANDLE DIFFERENT MESSAGE TYPES ---

        if (chatMessage.getIsAttachment()) {
            // --- THIS IS AN ATTACHMENT MESSAGE ---

            // 1. Show the attachment layout and hide the regular text view
            holder.messageTextView.setVisibility(View.GONE);
            holder.attachmentLayout.setVisibility(View.VISIBLE);

            // 2. Set the file name on the "View Attachment" link and underline it
            SpannableString fileNameSpannable = new SpannableString(chatMessage.getMessage());
            fileNameSpannable.setSpan(new UnderlineSpan(), 0, fileNameSpannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.attachmentTextView.setText(fileNameSpannable);

            // 3. Check the uploading state to control the UI
            if (chatMessage.getIsUploading()) {
                // The file is currently being uploaded
                holder.uploadingIndicator.setVisibility(View.VISIBLE);
                holder.attachmentTextView.setClickable(false); // Make link unclickable
                holder.attachmentTextView.setAlpha(0.5f); // Dim the link to show it's disabled
                holder.attachmentTextView.setOnClickListener(null); // Remove any previous listener
            } else {
                // The file has finished uploading
                holder.uploadingIndicator.setVisibility(View.GONE);
                holder.attachmentTextView.setClickable(true); // Make link clickable
                holder.attachmentTextView.setAlpha(1.0f); // Restore full opacity

                // 4. Set the click listener to trigger the file preview
                holder.attachmentTextView.setOnClickListener(v -> {
                    // Call the static bridge method in your plugin to handle the preview

                    JSONObject filedata = new JSONObject();
                    try {
                        filedata.put("documentId", chatMessage.getAttachmentId());
                        filedata.put("fileName", chatMessage.getFileName());
                        filedata.put("fileMimetype",  chatMessage.getMimType());
                    } catch (JSONException e) {
                        Log.e("ChatAdapter", "Error parsing file URL", e);
                    }
                    ZoomVideo.registerDownloadFileListener(filedata);
                });
            }

        } else {
            // --- THIS IS A SIMPLE TEXT MESSAGE ---

            // 1. Show the regular text view and hide the attachment layout
            holder.messageTextView.setVisibility(View.VISIBLE);
            holder.attachmentLayout.setVisibility(View.GONE);

            // 2. Process the text for any clickable URLs (your existing logic is correct)
            String message = chatMessage.getMessage();
            if (message.contains("https://fileupload.bupa.com.sa")) {
                message = message.replace(" ", "").replace("\n", "");
            }

            SpannableString spannableString = new SpannableString(message);
            Pattern pattern = Patterns.WEB_URL;
            Matcher matcher = pattern.matcher(message);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String url = matcher.group();

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (url.contains("fileupload.bupa.com.sa")) {
                            String[] parts = url.split("#");
                            JSONObject filedata = new JSONObject();
                            try {
                                filedata.put("documentId", parts[1]);
                                filedata.put("fileName", parts[2]);
                                filedata.put("fileMimetype", parts[3]);
                            } catch (JSONException e) {
                                Log.e("ChatAdapter", "Error parsing file URL", e);
                            }
                            ZoomVideo.registerDownloadFileListener(filedata);
                        } else {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            widget.getContext().startActivity(browserIntent);
                        }
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(Color.BLUE);
                        ds.setUnderlineText(true);
                    }
                };
                spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            holder.messageTextView.setText(spannableString);
            holder.messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }


    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void addMessage(ChatMessage chatMessage) {
        chatMessages.add(chatMessage);
        notifyItemInserted(chatMessages.size() - 1);
    }

    /**
     * This ViewHolder now contains references to all the views in the new layout,
     * including the ones for handling attachments.
     */
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        // Views for all message types
        TextView senderTextView;

        // Views for TEXT messages
        TextView messageTextView;

        // Views for ATTACHMENT messages
        LinearLayout attachmentLayout;
        TextView attachmentTextView;
        ProgressBar uploadingIndicator;

        private Context context;
        final String ID = "id";

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();

            // Find all the views by their ID
            senderTextView = itemView.findViewById(getResourceId(context, ID, "senderTextView"));
            messageTextView = itemView.findViewById(getResourceId(context, ID, "messageTextView"));
            attachmentLayout = itemView.findViewById(getResourceId(context, ID, "attachmentLayout"));
            attachmentTextView = itemView.findViewById(getResourceId(context, ID, "attachmentTextView"));
            uploadingIndicator = itemView.findViewById(getResourceId(context, ID, "uploadingIndicator"));
        }
    }
}
