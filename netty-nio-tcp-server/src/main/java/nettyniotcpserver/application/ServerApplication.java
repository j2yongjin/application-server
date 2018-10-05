package nettyniotcpserver.application;

import com.sun.javafx.scene.control.Logging;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.time.LocalDateTime;

/**
 * Created by yjlee on 2018-09-09.
 */

@SpringBootApplication
@ComponentScan("nettyniotcpserver.*")
@Slf4j
public class ServerApplication {

    static Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {

        SpringApplication springApplication =
                new SpringApplication(ServerApplication.class);
        springApplication.addListeners(new GracefulShutdown());
        ConfigurableApplicationContext configurableApplicationContext =
                springApplication.run(args);
        log.info(" Server Application Start ");

    }
}
