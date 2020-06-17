package MCHerald.util;

import MCHerald.MCHerald;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;

public class Constants {

    public static final ServerInfo ERROR = new ServerInfo.ServerDummy();
    public static final Font APP_FONT = new Font("Serif", Font.PLAIN, 14);
    public static final int SERVER_TIMEOUT = 7000;

    public static final class COLUMNS {
        public static final int NOTIFICATION_STATUS = 0;
        public static final int NAME = 1;
        public static final int IP = 2;
        public static final int FREQUENCY = 3;
        public static final int ONLINE_OUT_OF_MAX = 4;
        public static final int UUID = 5;
    }
    public static final int DEFAULT_FREQUENCY = 30;
    public static final int MC_PORT = 25565;

    //Obtain the image URL
    public static Image createImage(String path, String description) throws FileNotFoundException {
        URL imageURL = MCHerald.class.getResource(path);
        if (imageURL == null) throw new FileNotFoundException("Path: "+ path);
        return (new ImageIcon(imageURL, description)).getImage();
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }
}
