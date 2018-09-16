package niotcpserver.Executor;


import com.sun.security.ntlm.Server;
import niotcpserver.socket.ServerSocket;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-11
 * @desc :
 */
public class WorkerNioExecutors {

    public Runnable exec(SocketChannel socketChannel, Selector selector){

        return new Runnable() {
            @Override
            public void run() {

                try {
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                    while (!Thread.interrupted()){

                        selector.select();
                        Iterator keys = selector.selectedKeys().iterator();

                        while (keys.hasNext()){

                            SelectionKey selectionKey = (SelectionKey) keys.next();
                            keys.remove();
                            if(selectionKey.isReadable()){

                                ServerSocket serverSocket = new ServerSocket(socketChannel);
                                String readData = serverSocket.read(100);

                                serverSocket.write(readData.getBytes());
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };



    }
}
