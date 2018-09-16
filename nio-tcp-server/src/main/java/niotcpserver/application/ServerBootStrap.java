package niotcpserver.application;

import niotcpserver.config.ServerConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-17
 * @desc :
 */
public class ServerBootStrap {


    ServerConfig serverConfig;

    public void start(){


        try {
            Selector selector;
            selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            SocketAddress socketAddress = new InetSocketAddress(serverConfig.getPort());

            int ops = serverSocketChannel.validOps();
            SelectionKey selectionKey = serverSocketChannel.register(selector,ops,null);
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();


        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public ServerBootStrap setServerConfig(ServerConfig serverConfig){
        this.serverConfig = serverConfig;
        return this;
    }







}
