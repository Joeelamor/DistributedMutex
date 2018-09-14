import algorithm.RicartAgrawala;
import util.ConfigLoader;
import util.HostConfig;
import time.ScalarClock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Node {
    private int id;
    private ScalarClock time;
    private ConcurrentLinkedQueue<Message> messageQueue;
    private ConcurrentHashMap<Integer, Sender> senderMap;
    private int nodeNum;

    final int port = 46378;

    public Node(int id, int nodeNum) {
        this.id = id;
        this.time = new ScalarClock(id, id); // use id value as initial timestamp
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.nodeNum = nodeNum;
        this.senderMap = new ConcurrentHashMap<>();
    }

    private void init() {
        ConfigLoader configLoader = new ConfigLoader("config.txt");
        Collection<HostConfig> hostConfigs = new ArrayList<>();
        try {
            hostConfigs.addAll(configLoader.loadExistingHostConfigs());
            for (HostConfig hostConfig : hostConfigs) {

                Socket socket = new Socket(hostConfig.getIP(), hostConfig.getPort());
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                System.out.println(inputStream);

                Sender sender = new Sender(outputStream);
                Thread senderThread = new Thread(sender);
                senderThread.start();
                senderMap.put(hostConfig.getId(), sender);

                Message message = new Message(id, Message.Type.INI, time.incrementAndGet());
                this.send(hostConfig.getId(), message);

                new Thread(new Receiver(inputStream, messageQueue, time)).start();
                System.out.println("Connected all exited hosts");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to connect to existing host");
        }

        Thread listenerThread = new Thread(() -> {
            try (ServerSocket listener = new ServerSocket(port)) {
                HostConfig localHostConfig = new HostConfig(id, InetAddress.getLocalHost().getHostAddress(), port);
                hostConfigs.add(localHostConfig);
                configLoader.dumpHostConfigs(hostConfigs);
                while (true) {
                    Socket socket = listener.accept();
                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Message message = (Message) inputStream.readObject();

                    Sender sender = new Sender(outputStream);
                    Thread senderThread = new Thread(sender);
                    senderThread.start();
                    senderMap.put(message.getSenderId(), sender);

                    this.send(message.getSenderId(), new Message(id, Message.Type.INI, time.incrementAndGet()));
                    System.out.println(message);
                    new Thread(new Receiver(inputStream, messageQueue, time)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Unable to start server logic");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.err.println("Class of a serialized object cannot be found");
            }
        });

        listenerThread.start();

        while (senderMap.size() < nodeNum - 1) {
            continue;
        }
    }

    private void start(RequestGenerator requestGenerator) {
        requestGenerator.setId(id);
        requestGenerator.setQueue(messageQueue);
        requestGenerator.setSenderMap(senderMap);
        requestGenerator.setTime(time);
        Thread requestGeneratorThread = new Thread(requestGenerator);
        requestGeneratorThread.start();

        RicartAgrawala ra = new RicartAgrawala(nodeNum);
        RicartAgrawala.Operation op;
        ArrayList<Integer> deferredReplies = new ArrayList<>(nodeNum - 1);
        while (true) {
            if (messageQueue.isEmpty()) {
                continue;
            }
            op = RicartAgrawala.Operation.NOP;
            Message message = messageQueue.poll();
            try {
                switch (message.getType()) {
                    case REQ:
                        if (message.getSenderId() == id) {
                            op = ra.createRequest(new ScalarClock(id, message.getTimestamp()));
                        } else
                            op = ra.receiveRequest(message.getSenderId(), message.getTimestamp());
                        break;
                    case RPY:
                        op = ra.receiveReply();
                        break;
                    case FINISH:
                        op = ra.exitCriticalSection();
                        requestGenerator.notifyNewRequest();
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                switch (op) {
                    case REPLY:
                        this.send(message.getSenderId(), new Message(this.id, Message.Type.RPY, time.incrementAndGet()));
                        break;
                    case DEFER:
                        deferredReplies.add(message.getSenderId());
                        break;
                    case SEND_DEFER:
                        for (int target : deferredReplies) {
                            this.send(target, new Message(this.id, Message.Type.RPY, time.incrementAndGet()));
                        }
                        deferredReplies.clear();
                        break;
                    case EXEC:
                        executeCriticalSection();
                        break;
                }
            }
        }
    }


    private void executeCriticalSection() {
        try {
            System.out.println("!!!!!!ENTER CRITICAL SECTION!!!!!!");
            Thread.sleep(3000);
            messageQueue.offer(new Message(this.id, Message.Type.FINISH, time.incrementAndGet()));
            System.out.println("Exit critical section");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private void send(int id, Message message) {
        senderMap.get(id).queue.offer(message);
    }


    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        int totalNodeCount = Integer.parseInt(args[1]);
        int ReqGenTimeFloor = Integer.parseInt(args[2]);
        int ReqGenTimeRange = Integer.parseInt(args[3]);
        int ReqGenTimeUnit = Integer.parseInt(args[4]);
        Node node = new Node(id, totalNodeCount);
        node.init();
        RequestGenerator requestGenerator = new RequestGenerator(ReqGenTimeFloor, ReqGenTimeRange, ReqGenTimeUnit);
        node.start(requestGenerator);
    }
}
