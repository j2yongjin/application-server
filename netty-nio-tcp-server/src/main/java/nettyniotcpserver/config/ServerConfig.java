package nettyniotcpserver.config;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * application-server
 *
 * @auther : yjlee
 * @date : 2018-09-10
 * @desc :
 */
@Configuration
@Getter
public class ServerConfig {

    @Value("${tcp.port}")
    private int tcpPort=10051;

    @Value("${boss.threadcount}")
    private int bossThreadCount;

    @Value("${worker.threadcount}")
    private int workerThreadCount;


}
