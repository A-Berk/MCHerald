package MCHerald.gui;

import MCHerald.MCHerald;
import MCHerald.util.Constants;
import MCHerald.util.GUI;
import MCHerald.util.Language;
import MCHerald.util.ServerInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class SystemTrayMenu implements GUI {

    private MCHerald herald;
    private Menu serversMenu;

    private final SystemTray tray;
    private final TrayIcon trayIcon;
    private final HashMap<String, ServerMenuItem> serversMenuItem;

    public SystemTrayMenu(MCHerald herald, LinkedHashMap<String, ServerInfo> servers) throws FileNotFoundException {
        if (!SystemTray.isSupported()) throw new UnsupportedOperationException(Language.TRAY.UNSUPPORTED);
        try {
            UIManager.setLookAndFeel(Language.UI.LOOK_FEEL);
        } catch (Exception ignore) {System.out.println(Language.UI.LOOK_FEEL_ERROR);}
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        this.herald = herald;
        this.tray = SystemTray.getSystemTray();
        PopupMenu popup = new PopupMenu();

        try {
            trayIcon = new TrayIcon(
                Constants.createImage(Language.R.ICON_APP, Language.ICON_ADD_DESCRIPTION),
                Language.TRAY.TOOLTIP,
                popup
            );
            trayIcon.setImageAutoSize(true);
        } catch (FileNotFoundException ignore) {
            throw new FileNotFoundException(Language.ICON_NOT_FOUND);
        }

        // Create a popup menu components
        CheckboxMenuItem notify = new CheckboxMenuItem(Language.TRAY.NOTIFICATIONS);
        serversMenu = new Menu(Language.TRAY.SERVERS_LIST);
        serversMenuItem = new HashMap<>();
        for (ServerInfo s : servers.values()) {
            ServerMenuItem i = new ServerMenuItem(s.getName(), s.getUUID());
            i.setState(s.getState());
            serversMenuItem.put(s.getUUID(), i);
        }
        MenuItem addServerItem = new MenuItem(Language.ADD_SERVER.TITLE);
        MenuItem settingsItem = new MenuItem(Language.TRAY.SERVER_TABLE);
        MenuItem aboutItem = new MenuItem(Language.TRAY.ABOUT);
        MenuItem exitItem = new MenuItem(Language.TRAY.EXIT);

        // Add Components to Menu
        popup.add(notify);
        popup.addSeparator();
        popup.add(serversMenu);
        if (serversMenuItem.size() > 0) {
            serversMenuItem.values().forEach(serversMenu::add);
            serversMenu.addSeparator();
        }
        serversMenu.add(addServerItem);
        popup.addSeparator();
        popup.add(settingsItem);
        popup.add(aboutItem);
        popup.add(exitItem);

        // Add Listeners
        notify.addItemListener(e -> herald.setNotifying(((CheckboxMenuItem) e.getSource()).getState()));
        settingsItem.addActionListener(e -> herald.openServerTable());
        aboutItem.addActionListener(e -> herald.openAbout());
        exitItem.addActionListener(e -> herald.shutdown());
        addServerItem.addActionListener(e -> herald.openAddServerMenu());

        for (CheckboxMenuItem i : serversMenuItem.values()) {
            i.addItemListener(new ServerToggle());
        }

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if(e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1){
                    herald.openServerTable();
                }
            }
        });

        this.open();
    }

    /* Public Methods */

    public void sendNotification(String caption, String text, TrayIcon.MessageType messageType){
        SwingUtilities.invokeLater(() -> trayIcon.displayMessage(caption, text, messageType));
    }

    public void addWatched(ServerInfo server){
        ServerMenuItem serverItem = new ServerMenuItem(server.getName(), server.getUUID());
        serverItem.setState(server.getState());
        // if the only item is "Add Server", ie no servers
        if(serversMenu.getItemCount() == 1){
            serversMenu.insert(serverItem, 0);
            serversMenu.insertSeparator(1);
        } else {
            serversMenu.insert(serverItem, serversMenu.getItemCount()-2);
        }
        serverItem.addItemListener(new ServerToggle());
        serversMenuItem.put(server.getUUID(), serverItem);
    }

    public void updateWatchedName(String uuid, String newName){
        serversMenuItem.get(uuid).setLabel(newName);
    }

    public void toggleWatched(String uuid){
        CheckboxMenuItem item = serversMenuItem.get(uuid);
        item.setState(!item.getState());
    }

    public void removeWatched(String uuid){
        serversMenu.remove(getMenuItemIndex(uuid));
        // if the only item is "Add Server" and the separator, ie no servers
        if(serversMenu.getItemCount() == 2){
            serversMenu.remove(0);
        }
    }

    /* Private Methods */

    private int getMenuItemIndex(String uuid){
        CheckboxMenuItem item = serversMenuItem.get(uuid);
        for(int i = 0; i < serversMenu.getItemCount(); i++){
            if(item.equals(serversMenu.getItem(i))) return i;
        }
        return -1;
    }

    /* Listener Classes */

    private class ServerToggle implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            herald.toggleServerNotifications(((CheckboxMenuItem) e.getSource()).getLabel());
        }
    }

    // Adds a uuid to the CheckboxMenuItem to identify the server.
    private static class ServerMenuItem extends CheckboxMenuItem {
        private final String UUID;
        ServerMenuItem(String name, String uuid){
            super(name);
            this.UUID = uuid;
        }

        public String getUUID(){
            return this.UUID;
        }
    }

   /* Contract Methods */

    @Override
    public void open(){
        SwingUtilities.invokeLater(() -> {
            try {
                tray.add(trayIcon);
            } catch (AWTException ignore) {}
        });
    }

    @Override
    public void close(){
        SwingUtilities.invokeLater(() -> tray.remove(trayIcon));
    }

    @Override
    public void shutdown(){
        this.close();
    }
}
