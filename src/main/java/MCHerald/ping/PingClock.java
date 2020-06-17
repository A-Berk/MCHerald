package MCHerald.ping;

import MCHerald.MCHerald;
import MCHerald.util.Constants;
import MCHerald.util.ServerInfo;
import MCHerald.util.Shuttable;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    public PingClock(MCHerald herald){
        this.nextIn = 0;
        this.updatingQueue = new LinkedList<>();
        this.updateHandler = new Timer(true);

        this.updateHandler.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!updatingQueue.isEmpty() && nextIn-- == 0){
                    System.out.println("Running Minute-Ping:");
                    PingClock.Packet nextPacket;
                    /*do {
                        System.out.println("Running Minute-Ping:");
                        System.out.println(PingClock.this);
                        nextPacket = PingClock.this.updatingQueue.remove();
                        nextPacket.serverInfos.doUpdateTask();
                        PingClock.this.add(nextPacket.serverInfos);

                        nextPacket = PingClock.this.updatingQueue.peek();
                        if(nextPacket == null) break;
                        PingClock.this.nextIn = nextPacket.nextIn;
                    } while (nextPacket.nextIn == 0);*/
                    ExecutorService pool = Executors.newFixedThreadPool(10);
                    //System.out.println(updatingQueue.peek());

                    do {
                        //nextPacket = PingClock.this.updatingQueue.remove();
                        final Packet packet = PingClock.this.updatingQueue.remove();
                        pool.execute(() -> {
                            System.out.println(PingClock.this);
                            packet.serverInfos.doUpdateTask();
                            PingClock.this.add(packet.serverInfos);
                        });
                    } while (!updatingQueue.isEmpty() && updatingQueue.peek().nextIn == 0);


                    /*while((nextPacket = updatingQueue.remove()).nextIn == 0){
                        final Packet packet = nextPacket;
                        pool.execute(() -> {
                            System.out.println(PingClock.this);
                            packet.serverInfos.doUpdateTask();
                            PingClock.this.add(packet.serverInfos);
                        });
                        if(updatingQueue.peek() == null) break;
                    }*/

                    try {
                        pool.awaitTermination(Constants.SERVER_TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Done Minute-Ping:");
                    herald.updateServerTable();
                }
            }
        }, 0, MINUTE); // AT: debugging prints for the pings
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
                    if(!updatingQueue.isEmpty()) this.nextIn = updatingQueue.peekFirst().nextIn;
                    System.out.println("Add server (1)");
                    System.out.println(PingClock.this);
                    return;
                } else break;
            } else if(nextIn < 0) {
                int prevIn = nextIn + this.updatingQueue.get(i).nextIn;
                listIterator.forEachRemaining((packet -> packet.nextIn -= prevIn));
                this.updatingQueue.add(i, new Packet(server, prevIn));
                if(!updatingQueue.isEmpty()) this.nextIn = updatingQueue.peekFirst().nextIn;
                System.out.println("Add server (2)");
                System.out.println(PingClock.this);
                return;
            }
        }
        // if nothing found yet, its more than anything in the queue so far, so add it to last with the remaining curFreq value.
        updatingQueue.addLast(new Packet(server, nextIn));
        if(!updatingQueue.isEmpty()) this.nextIn = updatingQueue.peekFirst().nextIn;
        System.out.println("Add server (3)");
        System.out.println(PingClock.this);
    }

    /**
     * Preforms a linear search to find and remove the given server from the updatingQueue
     * @param server The server to be removed from the updatingQueue
     */
    public void remove(ServerInfo server){
        for (Packet s: this.updatingQueue) {
            if(s.serverInfos.equals(server)){
                this.updatingQueue.remove(s);
                if(!updatingQueue.isEmpty()) this.nextIn = updatingQueue.peekFirst().nextIn;
                return;
            }
        }
    }

    /**
     * First removes old server from updatingQueue,
     * then adds back
     * @param server The server with the frequency to be updated
     */
    public void updateFrequency(ServerInfo server){
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

        @Override
        public String toString() {
            return "Next: " + nextIn + " | " + serverInfos;
        }
    }

    @Override
    @SuppressWarnings("unchecked") // Cloning of updating queue.
    public String toString(){
        StringJoiner stringJoiner = new StringJoiner(", ", "PingClock <", ">");
        for(Packet o : (LinkedList<Packet>) this.updatingQueue.clone()) stringJoiner.add("[" + o.nextIn + ", (" +o.serverInfos.getFrequency()+ ")]");
        return stringJoiner.toString();
    }
}
