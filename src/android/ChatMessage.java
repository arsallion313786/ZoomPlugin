package cordova.plugin.zoomvideo;

// Add this import at the top of the file
import java.io.Serializable;

// Add "implements Serializable" to your class definition
public class ChatMessage implements Serializable {

    private String sender;
    private String message;

    private boolean isAttachmentMessage;

    private String attachmentId;

    private  String mimType;

    private  String fileName;

    private  boolean isUploading;

    public ChatMessage(String sender, String message,boolean isAttachmentMessage, String attachmentId , String mimType, String fileName, boolean isUploading) {
        this.sender = sender;
        this.message = message;
        this.isAttachmentMessage = isAttachmentMessage;
        this.attachmentId = attachmentId;
        this.mimType = mimType;
        this.fileName = fileName;
        this.isUploading = isUploading;
    }

    public ChatMessage(String sender, String message) {
        // Call the main constructor, passing default values for the attachment fields
        this(sender, message, false, null, null, null, false);
    }

    public void updateAttachmentDetails(String attachmentId, String mimType) {
        this.attachmentId = attachmentId;
        this.mimType = mimType;
        this.isUploading = false; // Mark the upload as complete
    }



    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public boolean getIsAttachment() {
        return isAttachmentMessage;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public String getMimType() {
        return mimType;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean getIsUploading() {
        return isUploading;
    }
}

