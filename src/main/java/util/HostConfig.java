package util;

public class HostConfig {
    public Integer id;
    public String IP;
    public Integer port;

    public HostConfig(Integer id, String IP, Integer port) {
        this.id = id;
        this.IP = IP;
        this.port = port;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "HostConfig{" +
            "id=" + id +
            ", IP='" + IP + '\'' +
            ", port=" + port +
            '}';
    }
}
