package nettyniotcpserver.init;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import nettyniotcpserver.codec.DefaultDecoder;
import nettyniotcpserver.handler.DefaultInboundHandler;


/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-10
 * @desc :
 */
@Slf4j
public class SocketChannelInitializer extends ChannelInitializer<SocketChannel>{
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        try {
            ChannelPipeline channelPipeline = ch.pipeline();
            channelPipeline.addLast(new ReadTimeoutHandler(10));
            channelPipeline.addLast(new DefaultDecoder());
            channelPipeline.addLast(new DefaultInboundHandler());
        }catch (Exception e){
            log.error("Init Channel Error " , e);
            throw e;
        }
    }
}
