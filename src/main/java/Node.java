import java.net.Socket;
import java.util.HashMap;

public class Node {
    private int id;
    private int time;
    private HashMap<Integer, Socket> map;

    public Node(int id) {
        this.id = id;
        this.time = 0;
        this.map = new HashMap<>();
    }

    private void init() {

    }

    private void start() {

    }

    public static void main(String[] args) {
        Node node = new Node(Integer.parseInt(args[0]));
        node.init();
        node.start();
    }
}
