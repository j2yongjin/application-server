package niotcpserver.config;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-11
 * @desc :
 */

public class ServerConfig {

    int port;
    boolean block;

    public ServerConfig(int port, boolean block) {
        this.port = port;
        this.block = block;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    public int getPort() {
        return port;
    }

    public boolean isBlock() {
        return block;
    }
}
