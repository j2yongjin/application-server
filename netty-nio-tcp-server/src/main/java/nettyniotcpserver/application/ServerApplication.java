package nettyniotcpserver.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by yjlee on 2018-09-09.
 */

@SpringBootApplication
@ComponentScan("nettyniotcpserver.*")
@Slf4j
public class ServerApplication {

    public static void main(String[] args) {

        SpringApplication springApplication =
                new SpringApplication(ServerApplication.class);
        springApplication.addListeners(new GracefulShutdown());
        ConfigurableApplicationContext configurableApplicationContext =
                springApplication.run(args);
        log.info(" Server Application Start ");

    }
}
