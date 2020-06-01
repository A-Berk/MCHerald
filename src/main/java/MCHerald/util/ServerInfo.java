package MCHerald.util;

import MCHerald.MCHerald;
import MCHerald.ping.ServerPing;
import MCHerald.ping.StatusResponse;

import java.awt.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.*;

public class ServerInfo implements Serializable, Shuttable {
    private String host, name;
    private boolean state;
    private int frequency;

    private transient StatusResponse lastResponse;
    private transient ServerPing serverPinger;
    private transient Timer timer;
    private transient MCHerald herald;

    /* Constructors */
    public ServerInfo(MCHerald herald, String host, String name, boolean state, int frequencyMinutes) {
        this.host = host;
        this.name = name;
        this.state = state;
        this.lastResponse = null;
        this.herald = herald;
        this.serverPinger = new ServerPing(new InetSocketAddress(host, Constants.MC_PORT));
        this.setFrequency(frequencyMinutes);
    }

    private ServerInfo(){}
    static class ServerDummy extends ServerInfo {ServerDummy() {}}

    /* Public Methods */

    public Object[] getServerData(){
        Object[] serverData = new Object[Language.TABLE.COLUMN_NAMES.length];
        serverData[Constants.COLUMNS.NOTIFICATION_STATUS] = getState();
        serverData[Constants.COLUMNS.NAME] = getName();
        serverData[Constants.COLUMNS.IP] = getHost();
        serverData[Constants.COLUMNS.FREQUENCY] = getFrequency()/60/1000;
        if(lastResponse != null)
            serverData[Constants.COLUMNS.ONLINE_OUT_OF_MAX] = lastResponse.getPlayers().getOnline() + "/" + lastResponse.getPlayers().getMax();
        else
            serverData[Constants.COLUMNS.ONLINE_OUT_OF_MAX] = Language.TABLE.PLAYER_COUNT_ERROR;
        return serverData;
    }

    public void refresh(){
        try {
            lastResponse = serverPinger.fetchData();
        } catch (Exception ignore) {}
    }

    /* Getters & Setters */

    public String getName() {
        return this.name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void setFrequency(int frequencyMinutes){
        if(frequencyMinutes < 1) this.frequency = Constants.DEFAULT_FREQUENCY;
        this.frequency = frequencyMinutes * 60 * 1_000;
        //if(this.timer != null) this.timer.cancel();
        //this.timer = new Timer(("Server: "+name), true);
        //this.timer.scheduleAtFixedRate(new RefreshTask(), frequency, frequency);
        //new Thread(this::refresh).start();
    }

    public boolean getState() {
        return this.state;
    }

    public void toggleState() {
        this.state = !this.state;
    }

    /* Private Methods */

    private class RefreshTask extends TimerTask {
        @Override
        public void run() {
            System.out.println(ServerInfo.this.name+" Running task.");
            StatusResponse lastResponse = ServerInfo.this.lastResponse;
            refresh();
            StatusResponse thisResponse = ServerInfo.this.lastResponse;

            if(state && herald.getNotifying()) {
                // if last query failed, update, no notify
                if(lastResponse == null) {
                    System.out.println(ServerInfo.this.name+" last query failed, update, no notify");
                    herald.updateServerTable();
                    return;
                }

                // if this query failed, do nothing
                if(thisResponse == null) {
                    System.out.println(ServerInfo.this.name+" this query failed, do nothing");
                    return;
                }

                List<StatusResponse.Player> lastPlayers = lastResponse.getPlayers().getSample();
                List<StatusResponse.Player> thisPlayers = thisResponse.getPlayers().getSample();

                // if no players on last query, empty list, continue
                if(lastPlayers == null){
                    System.out.println(ServerInfo.this.name+" no players on last query, empty list, continue");
                    herald.updateServerTable();
                    lastPlayers = new ArrayList<>();
                    //return;
                }

                // if no players on this query, do nothing
                if(thisPlayers == null) {
                    System.out.println(ServerInfo.this.name+" no players on this query, update");
                    herald.updateServerTable();
                    return;
                }

                // Get player names
                StringBuilder messageText = new StringBuilder();

                HashMap<String, Boolean> counter = new HashMap<>();
                thisPlayers.forEach(player -> counter.put(player.getName(), true));

                for (StatusResponse.Player thisPlayer : lastPlayers) {
                    if (counter.containsKey(thisPlayer.getName())) counter.put(thisPlayer.getName(), false);
                    else counter.put(thisPlayer.getName(), true);
                }

                ArrayList<String> thisPlayerNames = new ArrayList<>();
                counter.forEach((playerName, flag) -> {
                    if(flag) thisPlayerNames.add(playerName);
                });

                int thisCount = thisResponse.getPlayers().getOnline();
                int lastCount = lastResponse.getPlayers().getOnline();
                int thisPlayerCount = thisPlayerNames.size();

                // if 0 players, do nothing
                // if the count is the same, do nothing
                if(lastCount - thisCount == 0 && thisPlayerCount == 0) {
                    System.out.println(ServerInfo.this.name+" 0 players AND the count is the same, do nothing");
                    return;
                }

                // if 1 or 2 players, provide "<player_name[n]> [has/have] logged in!"
                if(thisPlayerCount == 1 || thisPlayerCount == 2){
                    System.out.println(ServerInfo.this.name+" 1 or 2 players, provide \"<player_name[n]> [has/have] logged in!\"");
                    StringJoiner stringJoiner = new StringJoiner(", ");
                    for(int i = 0; i < 2 && i < thisPlayerNames.size(); i++){
                        stringJoiner.add(thisPlayerNames.get(i));
                    }

                    if(thisPlayerNames.size() == 1) {
                        messageText.append(String.format(Language.SERVER.NOTIFICATION_SMALL_SINGULAR_FORMAT, stringJoiner.toString()));
                    } else {
                        messageText.append(String.format(Language.SERVER.NOTIFICATION_SMALL_PLURAL_FORMAT, stringJoiner.toString()));
                    }
                }

                // if 3 or more players, provide "[c] / [t] players are online"
                // if the count differs, provide "[c] / [t] players are online"
                else if(thisPlayerCount > 2 || lastCount - thisCount != 0){
                    System.out.println(ServerInfo.this.name+" 3 or more players OR the count differs, provide \"[c] / [t] players are online\"");
                    messageText.append(ServerInfo.this.lastResponse.getPlayers().getOnline());
                    messageText.append("/");
                    messageText.append(ServerInfo.this.lastResponse.getPlayers().getMax());
                }
                System.out.println(ServerInfo.this.name+" Sending Notification.");
                herald.sendNotification(name, String.format(Language.SERVER.NOTIFICATION_LARGE_FORMAT, messageText), TrayIcon.MessageType.NONE);
            }
            herald.updateServerTable();
        }
    }

    public void sendNotification(){
        System.out.println(ServerInfo.this.name+" Running task.");
        StatusResponse lastResponse = ServerInfo.this.lastResponse;
        refresh();
        StatusResponse thisResponse = ServerInfo.this.lastResponse;

        if(state && herald.getNotifying()) {
            // if last query failed, update, no notify
            if(lastResponse == null) {
                System.out.println(ServerInfo.this.name+" last query failed, update, no notify");
                herald.updateServerTable();
                return;
            }

            // if this query failed, do nothing
            if(thisResponse == null) {
                System.out.println(ServerInfo.this.name+" this query failed, do nothing");
                return;
            }

            List<StatusResponse.Player> lastPlayers = lastResponse.getPlayers().getSample();
            List<StatusResponse.Player> thisPlayers = thisResponse.getPlayers().getSample();

            // if no players on last query, empty list, continue
            if(lastPlayers == null){
                System.out.println(ServerInfo.this.name+" no players on last query, empty list, continue");
                herald.updateServerTable();
                lastPlayers = new ArrayList<>();
                //return;
            }

            // if no players on this query, do nothing
            if(thisPlayers == null) {
                System.out.println(ServerInfo.this.name+" no players on this query, update");
                herald.updateServerTable();
                return;
            }

            // Get player names
            StringBuilder messageText = new StringBuilder();

            HashMap<String, Boolean> counter = new HashMap<>();
            thisPlayers.forEach(player -> counter.put(player.getName(), true));

            for (StatusResponse.Player thisPlayer : lastPlayers) {
                if (counter.containsKey(thisPlayer.getName())) counter.put(thisPlayer.getName(), false);
                else counter.put(thisPlayer.getName(), true);
            }

            ArrayList<String> thisPlayerNames = new ArrayList<>();
            counter.forEach((playerName, flag) -> {
                if(flag) thisPlayerNames.add(playerName);
            });

            int thisCount = thisResponse.getPlayers().getOnline();
            int lastCount = lastResponse.getPlayers().getOnline();
            int thisPlayerCount = thisPlayerNames.size();

            // if 0 players, do nothing
            // if the count is the same, do nothing
            if(lastCount - thisCount == 0 && thisPlayerCount == 0) {
                System.out.println(ServerInfo.this.name+" 0 players AND the count is the same, do nothing");
                return;
            }

            // if 1 or 2 players, provide "<player_name[n]> [has/have] logged in!"
            if(thisPlayerCount == 1 || thisPlayerCount == 2){
                System.out.println(ServerInfo.this.name+" 1 or 2 players, provide \"<player_name[n]> [has/have] logged in!\"");
                StringJoiner stringJoiner = new StringJoiner(", ");
                for(int i = 0; i < 2 && i < thisPlayerNames.size(); i++){
                    stringJoiner.add(thisPlayerNames.get(i));
                }

                if(thisPlayerNames.size() == 1) {
                    messageText.append(String.format(Language.SERVER.NOTIFICATION_SMALL_SINGULAR_FORMAT, stringJoiner.toString()));
                } else {
                    messageText.append(String.format(Language.SERVER.NOTIFICATION_SMALL_PLURAL_FORMAT, stringJoiner.toString()));
                }
            }

            // if 3 or more players, provide "[c] / [t] players are online"
            // if the count differs, provide "[c] / [t] players are online"
            else if(thisPlayerCount > 2 || lastCount - thisCount != 0){
                System.out.println(ServerInfo.this.name+" 3 or more players OR the count differs, provide \"[c] / [t] players are online\"");
                messageText.append(ServerInfo.this.lastResponse.getPlayers().getOnline());
                messageText.append("/");
                messageText.append(ServerInfo.this.lastResponse.getPlayers().getMax());
            }
            System.out.println(ServerInfo.this.name+" Sending Notification.");
            herald.sendNotification(name, String.format(Language.SERVER.NOTIFICATION_LARGE_FORMAT, messageText), TrayIcon.MessageType.NONE);
        }
        herald.updateServerTable();
    }

    /* Contract Methods */

    @Override
    public void shutdown(){
        timer.cancel();
        timer.purge();
    }

    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(", ", "ServerInfo [", "]");
        for(Object o : getServerData()) stringJoiner.add(o.toString());
        return stringJoiner.toString();
    }
}