import time.ScalarClock;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RequestGenerator implements Runnable{
    private ConcurrentLinkedQueue<Message> queue;
    private int timeFloor;
    private int timeRange;
    private int timeUnit;
    private int id;
    private ScalarClock time;
    private ConcurrentHashMap<Integer, Sender>  senderMap;

    public void setQueue(ConcurrentLinkedQueue<Message> queue) {
        this.queue = queue;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTime(ScalarClock time) {
        this.time = time;
    }

    public void setSenderMap(ConcurrentHashMap<Integer, Sender> senderMap) {
        this.senderMap = senderMap;
    }

    public RequestGenerator(int timeFloor, int timeRange, int timeUnit) {
        this.timeFloor = timeFloor;
        this.timeRange = timeRange;
        this.timeUnit = timeUnit;
    }

    public RequestGenerator(ConcurrentLinkedQueue<Message> queue, int timeFloor, int timeRange, int id, ScalarClock time,
                            ConcurrentHashMap<Integer, Sender> senderMap) {
        this.queue = queue;
        this.timeFloor = timeFloor;
        this.timeRange = timeRange;
        this.id = id;
        this.time = time;
        this.senderMap = senderMap;
    }


    @Override
    public void run() {
        Random random = new Random();
        while (true) {
            int x = (random.nextInt(timeFloor) + timeRange) * timeUnit;
            try {
                Thread.sleep(x);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Message reqMessage = new Message(id ,Message.Type.REQ, time.incrementAndGet());
            queue.offer(reqMessage);
            broadcast(reqMessage);
        }
    }

    private void broadcast(Message message) {
        for (Sender sender : senderMap.values()) {
            sender.queue.offer(message);
        }
    }


}
