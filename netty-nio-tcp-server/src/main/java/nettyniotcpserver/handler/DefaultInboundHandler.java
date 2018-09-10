package nettyniotcpserver.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nettyniotcpserver.protocol.Protocol;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-10
 * @desc :
 */
@NoArgsConstructor
@Slf4j
public class DefaultInboundHandler extends SimpleChannelInboundHandler<Protocol>{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Protocol msg) throws Exception {




    }
}
