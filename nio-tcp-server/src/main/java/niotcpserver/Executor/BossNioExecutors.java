package niotcpserver.Executor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-11
 * @desc :
 */

public class BossNioExecutors  extends  DefaultNioExecutors{

    Integer threadPoolCount;
    ThreadFactory threadFactory;

    public void start(){
        createExecutors(threadPoolCount,threadFactory);
    }

    public void exec(Runnable runnable){
        threadPoolExecutor.execute(runnable);
    }


    public Runnable exec(Selector selector){
        return new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()){
                    try {
                        selector.select();
                        Iterator selectionKey = selector.selectedKeys().iterator();

                        while (selectionKey.hasNext()){
                            SelectionKey key = (SelectionKey) selectionKey.next();
                            selectionKey.remove();

                           if(key.isAcceptable()){
                               ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                               SocketChannel socketChannel = serverSocketChannel.accept();
                           }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }

            }
        };
    }

}
