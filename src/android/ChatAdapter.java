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
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    final String STRING = "string";
    final String DRAWABLE = "drawable";
    final String ID = "id";
    private List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(getResourceId(context,LAYOUT,("item_chat_message")), parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        holder.senderTextView.setText(chatMessage.getSender());

        String msg = chatMessage.getMessage();
        if(msg.contains("https://fileupload.bupa.com.sa") ){
            msg = msg.replace(" ", "");
            msg = msg.replace("\n", "");
        }
        holder.messageTextView.setText(msg);
        // Detect and make URLs clickable
        //Linkify.addLinks(holder.messageTextView, Linkify.WEB_URLS);

        //String message = chatMessages.get(position).getContent();

        String message = msg;
        SpannableString spannableString = new SpannableString(message);
        // Detect URLs and apply ClickableSpan
        Pattern pattern = Patterns.WEB_URL;
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String url = matcher.group();

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    // Handle the URL click
                   // Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                   // widget.getContext().startActivity(browserIntent);

                    //Log.d(TAG, "onClick: ");
                    if(url.contains("fileupload.bupa.com.sa")) {
                        String[] parts = url.split("#");
                        JSONObject filedata = new JSONObject();
                        try {
                            filedata.put("documentId", parts[1]);
                            filedata.put("fileName", parts[2]);
                            filedata.put("fileMimetype", parts[3] );
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        ZoomVideo.registerDownloadFileListener(filedata);
                    }else{
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        widget.getContext().startActivity(browserIntent);
                    }
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.BLUE); // Set the color of the link
                    ds.setUnderlineText(true); // Set underline
                }
            };
            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.messageTextView.setText(spannableString);
        holder.messageTextView.setMovementMethod(LinkMovementMethod.getInstance());

    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    public void addMessage(ChatMessage chatMessage) {
        chatMessages.add(chatMessage);
        notifyItemInserted(chatMessages.size() - 1);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView senderTextView;
        TextView messageTextView;
        private Context context;
        final String LAYOUT = "layout";
        final String STRING = "string";
        final String DRAWABLE = "drawable";
        final String ID = "id";

        public ChatViewHolder(@NonNull View itemView) {

            super(itemView);
            context = itemView.getContext();
            senderTextView = itemView.findViewById(getResourceId(context,ID,("senderTextView")));
            messageTextView = itemView.findViewById(getResourceId(context,ID,("messageTextView")));
        }
    }
}
