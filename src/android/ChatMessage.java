package cordova.plugin.zoomvideo;

// Add this import at the top of the file
import java.io.Serializable;

// Add "implements Serializable" to your class definition
public class ChatMessage implements Serializable {

    private String sender;
    private String message;

    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }
}

