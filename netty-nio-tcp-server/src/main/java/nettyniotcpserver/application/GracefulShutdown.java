package nettyniotcpserver.application;

import lombok.extern.slf4j.Slf4j;
import nettyniotcpserver.server.NettyServer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Created by yjlee on 2018-09-09.
 */
@Slf4j
public class GracefulShutdown implements ApplicationListener<ContextClosedEvent> {
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        NettyServer nettyServer = (NettyServer) event.getApplicationContext()
                .getBean("nettyServer");
        nettyServer.stop();
        log.info("graceful shutDown");
    }
}
