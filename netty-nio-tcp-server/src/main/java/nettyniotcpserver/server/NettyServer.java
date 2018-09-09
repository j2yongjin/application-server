package nettyniotcpserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.annotation.PostConstruct;

/**
 * Created by yjlee on 2018-09-09.
 */
public class NettyServer {

    @PostConstruct
    public void init(){
        start();
    }

    EventLoopGroup bossGroup;
    EventLoopGroup wokerGroup;

    public void start(){
        bossGroup = new NioEventLoopGroup();
        wokerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,wokerGroup)
                    .channel(NioServerSocketChannel.class);
//            .childHandler();


        }finally {
            bossGroup.shutdownGracefully();
            wokerGroup.shutdownGracefully();
        }
    }

    public void stop(){
        if(bossGroup!=null)
            bossGroup.shutdownGracefully();
        if(wokerGroup!=null)
            wokerGroup.shutdownGracefully();
    }

}
