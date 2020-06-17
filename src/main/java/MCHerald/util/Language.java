package MCHerald.util;

public class Language {

    public static final String SERVER_MAP_KEY = "ServerMap";
    public static final String ICON_ADD_DESCRIPTION = "MCHerald Icon";
    public static final String ICON_APP_DESCRIPTION = "Add Server Icon";
    public static final String ICON_NOT_FOUND = "Icon file is missing.";

    public static class R {
        public static final String ICON_APP = "/icon.png";
        public static final String ICON_APP_LARGE = "/icon_large.png";
        public static final String ICON_ADD = "/add.png";
        public static final String CONFIG_NAME = "mc_herald_config.xml";
        public static final String CODE_LINK = "https://github.com/";
    }

    public static class TRAY {
        public static final String UNSUPPORTED = "SystemTray is not supported";
        public static final String TOOLTIP = "MC Player Herald";

        // Menu Items
        public static final String NOTIFICATIONS = "Notifications";
        public static final String SERVERS_LIST = "Watched Servers";
        public static final String SERVER_TABLE = "Open Server List";
        public static final String ABOUT = "About";
        public static final String EXIT = "Exit";
    }

    public static class ADD_SERVER {
        public static final String TITLE = "Add Server";

        // Prompts / Form Labels
        public static final String NAME = "Name: ";
        public static final String HOST = "Server Host/IP: ";
        public static final String FREQUENCY = "Frequency (minutes): ";
        public static final String SUBMIT = "Add";
    }

    public static class DELETE_SERVER {
        public static final String TITLE = "Delete Server?";
        public static final String PROMPT_FORMAT = "Are you sure you want to delete the server \"%s\" ?";
        public static final String CANCEL = "Cancel";
        public static final String ACCEPT = "Delete Forever";
    }

    public static class ABOUT {
        public static final String TITLE = "About MC Herald v1.0";
        public static final String MESSAGE_HTML = "<html><body>"
                + "MC Herald, an open-source notifier for you to <br>"
                + "be notified when other players are online. <br><br>"
                + "The source code can be found <a href='%s'> here</a>.<br><br></body></html>";
    }

    public static class UI {
        public static final String LOOK_FEEL = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        public static final String LOOK_FEEL_ERROR = "Could not set UI to Windows Feel.";
    }

    public static class TABLE {
        public static final String REFRESH = "Refresh";
        public static final String ADD = "Add";
        public static final String DELETE = "Delete";
        public static final String ABOUT = "About";
        public static final String TITLE = "Servers Settings";
        public static final String[] COLUMN_NAMES = {"Notifications", "Name", "Host / IP", "Frequency (Minutes)", "Online", "UUID"};
        public static final String PLAYER_COUNT_ERROR = "?? / ??";
    }

    public static class SERVER {
        public static final String NOTIFICATION_SMALL_SINGULAR_FORMAT = "%s has logged in!";
        public static final String NOTIFICATION_SMALL_PLURAL_FORMAT = "%s have logged in!";
        public static final String NOTIFICATION_LARGE_FORMAT = "%s players are online right now.";
    }
}
