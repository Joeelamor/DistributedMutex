import util.ConfigLoader;
import util.HostConfig;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Node {
    private int id;
    private int time;
    private ConcurrentHashMap<Integer, Socket> socketMap;
    private ConcurrentLinkedQueue<Message> messageQueue;
    private ConcurrentHashMap<Integer, ObjectOutputStream>  outputStreamMap;

    final int port = 46378;

    public Node(int id) {
        this.id = id;
        this.time = 0;
        this.socketMap = new ConcurrentHashMap<>();
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.outputStreamMap = new ConcurrentHashMap<>();
    }

    private void init() {
        ConfigLoader configLoader = new ConfigLoader("config.txt");
        Collection<HostConfig> hostConfigs = new ArrayList<>();
        try {
            hostConfigs.addAll(configLoader.loadExistingHostConfigs());
            for (HostConfig hostConfig : hostConfigs) {
                System.out.println(hostConfig);
                Socket socket = new Socket(hostConfig.getIP(), hostConfig.getPort());
                System.out.println(socket);
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                System.out.println(outputStream);
                Message message = new Message(id, Message.Type.INI);
                outputStream.writeObject(message);

                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                System.out.println(inputStream);

                socketMap.put(hostConfig.getId(), socket);
                outputStreamMap.put(hostConfig.getId(), outputStream);
                new Thread(new Receiver(inputStream, messageQueue)).start();
                System.out.println("Finish");
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
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(new Message(id, Message.Type.INI));
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Message message = (Message) in.readObject();
                    socketMap.put(message.getSenderId(), socket);
                    outputStreamMap.put(message.getSenderId(), out);
                    System.out.println(message);
                    new Thread(new Receiver(in, messageQueue)).start();
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

    }

    private void start() {
        new Thread(() -> {
            Random rand = new Random();
            while (true) {
                broadcast(new Message(id, Message.Type.REQ));
                try {
                    Thread.sleep(rand.nextInt(500) + 500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        while (true) {
            if (!messageQueue.isEmpty()) {
                System.out.println(messageQueue.poll());
            }
        }
    }

    private void broadcast(Message message) {
        for (ObjectOutputStream outputStream : outputStreamMap.values()) {
            try {
                outputStream.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void send(int id, Message message) {
        ObjectOutputStream outputStream = outputStreamMap.get(id);
        try {
            outputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        Node node = new Node(Integer.parseInt(args[0]));
        node.init();
        node.start();
    }
}
