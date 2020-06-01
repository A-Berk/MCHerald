package messenger;

import messenger.gui.AddServer;
import messenger.gui.ServerTable;
import messenger.gui.SystemTrayMenu;
import messenger.util.Constants;
import messenger.util.Language;
import messenger.util.ServerInfo;
import messenger.util.Shuttable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

public class MCHerald implements Shuttable {

    private LinkedHashMap<String, ServerInfo> serverList;
    private Preferences pref;
    private SystemTrayMenu tray;
    private ServerTable serverTable;
    private AddServer addServer;
    private JFrame dialogPopupFrame;
    private boolean isNotifying = true; // TODO: Load this in from preferences
    private final Image appIcon, appIconLarge;

    private MCHerald() throws FileNotFoundException {
        UIManager.put("EditorPane.inactiveBackground", UIManager.get("OptionPane.background"));
        this.loadConfig();

        // Load all images.
        this.appIcon = Constants.createImage(Language.R.ICON_APP, "App Icon");
        this.appIconLarge = Constants.createImage(Language.R.ICON_APP_LARGE, "App Icon Large");

        this.dialogPopupFrame = new JFrame();
        this.tray = new SystemTrayMenu(this, serverList);
        this.serverTable = new ServerTable(this);
        this.addServer = new AddServer(this, serverTable);

        this.addServer(new ServerInfo(this, "72.69.253.223", "Selig's Server", true, 1));
        //this.addServer(new ServerInfo(this, "play.mineville.org", "MineVille", true, 2));
    }

    /* Public Methods */

    public void openAddServerMenu(){
        addServer.open();
    }

    public void deleteServer(String serverName){
        // delete, then update
        if(JOptionPane.showOptionDialog(dialogPopupFrame,
                String.format(Language.DELETE_SERVER.PROMPT_FORMAT, serverName),
                Language.DELETE_SERVER.TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{Language.DELETE_SERVER.ACCEPT, Language.DELETE_SERVER.CANCEL},
                Language.DELETE_SERVER.CANCEL
        ) == JOptionPane.OK_OPTION) {
            this.serverList.remove(serverName);
            this.tray.removeWatched(serverName);
            this.serverTable.update();
        }
    }

    public void addServer(ServerInfo newServer){
        if(this.serverList.put(newServer.getName(), newServer) != null) return;
        this.tray.addWatched(newServer);
        this.serverTable.update();
    }

    public void editServer(String serverName, int column, String change){
        ServerInfo server = serverList.get(serverName);
        switch (column) {
            case Constants.COLUMNS.NOTIFICATION_STATUS:
                server.toggleState();
                tray.toggleWatched(serverName);
                break;
            case Constants.COLUMNS.NAME:
                tray.updateWatchedName(server.getName(), change);
                server.setName(change);
                break;
            case Constants.COLUMNS.IP:
                server.setHost(change);
                break;
            case Constants.COLUMNS.FREQUENCY:
                server.setFrequency(Integer.parseInt(change));
                break;
        }
        updateServerTable();
    }

    public void openServerTable(){
        serverTable.open();
    }

    public void refreshServerTable(){
        // re-pings all valid servers, then calls update.
        for(ServerInfo server : serverList.values())
            if(server.getState()) server.refresh();
        this.updateServerTable();
    }

    public void updateServerTable(){
        // updates table based on current server list / information
        this.serverTable.update();
    }

    public void sendNotification(String caption, String text, TrayIcon.MessageType messageType){
        if(isNotifying) tray.sendNotification(caption, text, messageType);
    }

    public void openAbout(){
        JEditorPane htmlPane = new JEditorPane("text/html", String.format(Language.ABOUT.MESSAGE_HTML, Language.R.CODE_LINK));
        htmlPane.setEditable(false);
        htmlPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        htmlPane.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
        });

        JLabel author = new JLabel("- Programmer, Adam Berkowitz", JLabel.RIGHT);

        htmlPane.setFont(author.getFont());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(htmlPane, BorderLayout.CENTER);
        panel.add(author, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));

        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(dialogPopupFrame,
                    panel,
                    Language.ABOUT.TITLE,
                    JOptionPane.INFORMATION_MESSAGE,
                    new ImageIcon(appIcon)
            )
        );
    }

    public void toggleServerNotifications(String serverName) {
        serverList.get(serverName).toggleState();
    }

    /* Getters & Setters */

    public Image getAppIcon(){
        return this.appIcon;
    }

    public Image getAppIconLarge(){
        return this.appIconLarge;
    }

    public boolean getNotifying(){return this.isNotifying;}

    public void setNotifying(boolean isNotifying){
        this.isNotifying = isNotifying;
    }

    public LinkedHashMap<String, ServerInfo> getServerList(){
        return this.serverList;
    }

    /* Private Methods */

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        pref = Preferences.userNodeForPackage(MCHerald.class);
        byte[] data = pref.getByteArray(Language.SERVER_MAP_KEY, null);

        if (data != null) {
            try {
                this.serverList = (LinkedHashMap<String, ServerInfo>) Constants.deserialize(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (serverList == null) this.serverList = new LinkedHashMap<>();
    }

    private void saveConfig() {
        try {
            pref.putByteArray(Language.SERVER_MAP_KEY, Constants.serialize(serverList));
            pref.exportNode(new FileOutputStream(Language.R.CONFIG_NAME));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Contract Methods */

    @Override
    public void shutdown(){
        //saveConfig();
        for(ServerInfo server : serverList.values()) server.shutdown();
        tray.shutdown();
        addServer.shutdown();
        serverTable.shutdown();
        dialogPopupFrame.dispose();

        // If anything (for whatever reason) is still running, call to end.
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                Arrays.stream(Window.getWindows()).forEach(Window::dispose);
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.exit(-1);
                    }
                }, 3_000);
            }
        }, 7_000);
    }

    public static void main(String[] args) throws FileNotFoundException {
        new MCHerald();
    }
}
