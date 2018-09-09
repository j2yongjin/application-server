package nettyniotcpserver.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-10
 * @desc :
 */
@NoArgsConstructor
@Slf4j
public class DefaultInboundHandler extends SimpleChannelInboundHandler<Object>{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {




    }
}
