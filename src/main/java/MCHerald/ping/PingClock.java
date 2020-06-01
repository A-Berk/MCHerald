package MCHerald.ping;

import MCHerald.MCHerald;
import MCHerald.util.ServerInfo;
import MCHerald.util.Shuttable;

import java.util.*;

public class PingClock implements Shuttable {
    // pings all servers on list, on a pseudo system clock fashion

    // the servers have a list of changes to freq.
    // after pingTask, accept (last/only) change on queue
    // this accepts a waitObj, from each ServerInfo type in it's addWork()
    // updates data on each server for that tick (minute)
    // then calls "Update" (gui)

    private int nextIn;
    private LinkedList<Packet> updatingQueue;
    private Timer updateHandler;

    private static final int MINUTE = 1000 * 60;

    PingClock(MCHerald herald){
        this.nextIn = 0;
        this.updatingQueue = new LinkedList<>();
        this.updateHandler = new Timer(true);

        this.updateHandler.schedule(new TimerTask() {
            @Override
            public void run() {
                if(nextIn-- == 0){
                    PingClock.Packet nextPacket;
                    do {
                        nextPacket = PingClock.this.updatingQueue.remove();
                        nextPacket.serverInfos.refresh();
                        PingClock.this.add(nextPacket.serverInfos);

                        nextPacket = PingClock.this.updatingQueue.peek();
                        if(nextPacket == null) break;
                        PingClock.this.nextIn = nextPacket.nextIn;
                    } while (nextPacket.nextIn == 0);
                    herald.updateServerTable();
                }
            }
        }, 0, MINUTE);
    }

    /**
     * Adds the given server to the updatingQueue, updating future timers as needed
     * @param server The server to be added to the updatingQueue
     */
    public void add(ServerInfo server){
        int nextIn = server.getFrequency();
        ListIterator<Packet> listIterator = updatingQueue.listIterator();

        for(int i = listIterator.nextIndex(); listIterator.hasNext(); listIterator.next(), i = listIterator.nextIndex()){
            nextIn -= this.updatingQueue.get(i).nextIn;
            if (nextIn == 0) {
                listIterator.next();
                if(listIterator.hasNext()){
                    updatingQueue.add(listIterator.nextIndex(), new Packet(server, nextIn));
                    return;
                } else break;
            } else if(nextIn < 0) {
                int prevIn = nextIn + this.updatingQueue.get(i).nextIn;
                listIterator.forEachRemaining((packet -> packet.nextIn -= prevIn));
                this.updatingQueue.add(i, new Packet(server, prevIn));
                return;
            }
        }
        // if nothing found yet, its more than anything in the queue so far, so add it to last with the remaining curFreq value.
        updatingQueue.addLast(new Packet(server, nextIn));
    }

    /**
     * Preforms a linear search to find and remove the given server from the updatingQueue
     * @param server The server to be removed from the updatingQueue
     */
    public void remove(ServerInfo server){
        for (Packet s: this.updatingQueue) {
            if(s.serverInfos.equals(server)){
                this.updatingQueue.remove(s);
                return;
            }
        }
    }

    /**
     * First removes old server from updatingQueue,
     * then adds back
     * @param server The server with the frequency to be updated
     */
    private void updateFrequency(ServerInfo server){
        this.remove(server);
        this.add(server);
    }

    @Override
    public void shutdown() {
        this.updateHandler.cancel();
        this.updateHandler.purge();
    }

    private static class Packet {
        ServerInfo serverInfos;
        int nextIn;

        Packet(ServerInfo server, int nextIn){
            this.serverInfos = server;
            this.nextIn = nextIn;
        }
    }

    @Override
    public String toString(){
        StringJoiner stringJoiner = new StringJoiner(", ", "PingClock <", ">");
        for(Packet o : this.updatingQueue) stringJoiner.add("[" + o.nextIn + ", (" +o.serverInfos.getFrequency()+ ")]");
        return stringJoiner.toString();
    }
}
