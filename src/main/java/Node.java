import util.ConfigLoader;
import util.HostConfig;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class Node {
    private int id;
    private int time;
    private ConcurrentHashMap<Integer, Socket> map;

    final int port = 46378;

    public Node(int id) {
        this.id = id;
        this.time = 0;
        this.map = new ConcurrentHashMap<>();
    }

    private void init() {
        ConfigLoader configLoader = new ConfigLoader("config.txt");
        Collection<HostConfig> hostConfigs = new ArrayList<>();
        try {
            hostConfigs.addAll(configLoader.loadExistingHostConfigs());
            for (HostConfig hostConfig : hostConfigs) {
                System.out.println(hostConfig);
                Socket socket = new Socket(hostConfig.getIP(), hostConfig.getPort());
                new Thread(new Receiver(socket)).start();
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                Message message = new Message(id, Message.Type.INI);
                outputStream.writeObject(message);
                map.put(hostConfig.getId(), socket);
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
                    ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                    Message message = (Message) inputStream.readObject();
                    map.put(message.getSenderId(), socket);
                    System.out.println(message);
                    new Thread(new Receiver(socket)).start();
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
        for (Socket socket : map.values()) {
            new Thread(new Sender(socket, id)).start();
        }
    }

    public static void main(String[] args) {
        Node node = new Node(Integer.parseInt(args[0]));
        node.init();
        node.start();
    }
}
