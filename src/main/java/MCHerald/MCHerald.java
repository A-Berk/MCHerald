package MCHerald;

import MCHerald.gui.AddServer;
import MCHerald.gui.ServerTable;
import MCHerald.gui.SystemTrayMenu;
import MCHerald.ping.PingClock;
import MCHerald.util.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class MCHerald implements Shuttable {

    private final PingClock pingClock;
    private final LinkedBlockingQueue<Notification> notificationQueue;
    private final SystemTrayMenu tray;
    private final ServerTable serverTable;
    private final AddServer addServer;
    private final JFrame dialogPopupFrame;
    private LinkedHashMap<String, ServerInfo> serverList; // <uuid, serverObj>
    private Preferences pref;

    private boolean isNotifying = true, isRunning = true; // TODO: Load "isNotifying" in from preferences
    private final Image appIcon, appIconLarge;

    private long UUID;

    private MCHerald() throws FileNotFoundException {
        UIManager.put("EditorPane.inactiveBackground", UIManager.get("OptionPane.background"));
        this.loadConfig();

        // Load all images.
        this.appIcon = Constants.createImage(Language.R.ICON_APP, "App Icon");
        this.appIconLarge = Constants.createImage(Language.R.ICON_APP_LARGE, "App Icon Large");

        // Load up message handlers.
        this.notificationQueue = new LinkedBlockingQueue<>();
        new Thread(){
            @Override
            public void run() {
                while (isRunning){
                    try {
                        Notification notification = notificationQueue.take();
                        tray.sendNotification(notification.caption, notification.text, notification.messageType);
                        TimeUnit.SECONDS.sleep(2);  // Give time for the this notification, before popping the next up
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        // Load GUIv
        this.pingClock = new PingClock(this);
        this.dialogPopupFrame = new JFrame();
        this.tray = new SystemTrayMenu(this, serverList);
        this.serverTable = new ServerTable(this);
        this.addServer = new AddServer(this, serverTable);

        // TEMP: Starts servers with preset MC servers, while load/save is disabled to prevent debugging overload.
        this.addServer(new ServerInfo(this, "72.69.253.223", "Minecraft Server", true, 1));

        for(int i = 0; i < 15; i++)
            this.addServer(new ServerInfo(this, "xxx.xxx.xxx.xxx", "Server #"+(i+1), true, 2));
        //this.addServer(new ServerInfo(this, "play.mineville.org", "MineVille", true, 2));
    }

    /* Public Methods */

    public void openAddServerMenu(){
        addServer.open();
    }

    public void deleteServer(String uuid){
        ServerInfo server = serverList.get(uuid);
        if(server == null) return; // Throw something?
        // delete, then update
        if(JOptionPane.showOptionDialog(dialogPopupFrame,
                String.format(Language.DELETE_SERVER.PROMPT_FORMAT, server.getName()),
                Language.DELETE_SERVER.TITLE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{Language.DELETE_SERVER.ACCEPT, Language.DELETE_SERVER.CANCEL},
                Language.DELETE_SERVER.CANCEL
        ) == JOptionPane.OK_OPTION) {
            this.serverList.remove(server.getUUID());
            this.tray.removeWatched(server.getUUID());
            this.serverTable.update();
            this.pingClock.remove(server);
        } //TODO: Is this a mem leak? 64mb -> 88mb -> 120mb ...
    }

    public void addServer(ServerInfo newServer){
        if(this.serverList.put(newServer.getUUID(), newServer) != null) return;
        this.tray.addWatched(newServer);
        this.serverTable.update();
        this.pingClock.add(newServer);
    }

    public void editServer(String uuid, int column, String change){
        ServerInfo server = serverList.get(uuid);
        switch (column) {
            case Constants.COLUMNS.NOTIFICATION_STATUS:
                server.toggleState();
                tray.toggleWatched(server.getUUID());
                break;
            case Constants.COLUMNS.NAME:
                tray.updateWatchedName(server.getUUID(), change);
                server.setName(change);
                break;
            case Constants.COLUMNS.IP:
                server.setHost(change);
                break;
            case Constants.COLUMNS.FREQUENCY:
                server.setFrequency(Integer.parseInt(change));
                this.pingClock.updateFrequency(server);
                break;
        }
        updateServerTable();
    }

    public void openServerTable(){
        serverTable.open();
    }

    public void refreshServerTable(){
        if(!this.serverTable.isVisible()) return;
        // re-pings all valid servers, then calls update.
        Thread t = new Thread(() -> {
            ExecutorService pool = Executors.newFixedThreadPool(10);

            for(ServerInfo server : serverList.values()){
                pool.execute(() -> {
                    System.out.println("Calling refresh on server: "+server);
                    if(server.getState()) server.refresh();
                    System.out.println("Finished refresh on server: "+server);
                });
            }

            try {
                pool.awaitTermination(Constants.SERVER_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.updateServerTable();
        });
        t.setDaemon(true);
        t.start();
    }

    public void updateServerTable(){
        if(!this.serverTable.isVisible()) return;
        // updates table based on current server list / information
        this.serverTable.update();
    }

    public void sendNotification(Notification notification){
        if(isNotifying) notificationQueue.offer(notification);
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

    public void toggleServerNotifications(String uuid) {
        serverList.get(uuid).toggleState();
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

    public synchronized Map<String, ServerInfo> getServerList(){
        return Collections.unmodifiableMap(this.serverList);
    }

    //does two things, returns and then increments
    public String requestUUID() {
        return String.valueOf(this.UUID++);
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
        this.UUID = 0; // TODO: Read UUID from preferences
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
        isRunning = false;
        pingClock.shutdown();
        tray.shutdown();
        addServer.shutdown();
        serverTable.shutdown();
        dialogPopupFrame.dispose();

        // If anything (for whatever reason) is still running, force end.
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                Arrays.stream(Window.getWindows()).forEach(Window::dispose);
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.exit(-2);
                    }
                }, 3_000);
            }
        }, 7_000);
    }

    public static void main(String[] args) throws FileNotFoundException {
        new MCHerald();
    }
}
