package MCHerald.util;

import java.awt.*;

// wrapper class for a system tray notification.
public class Notification {
    public String caption, text;
    public TrayIcon.MessageType messageType;

    Notification(String caption, String text, TrayIcon.MessageType messageType){
        this.caption = caption;
        this.text = text;
        this.messageType = messageType;
    }
}
