package pl.edu.agh.student.sr.lab5.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
//@EnableConfigurationProperties({GrpcConfig.class})
public class ServerMain {
    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        log.info("Starting server Spring application");
        new SpringApplicationBuilder(ServerMain.class)
                .bannerMode(Banner.Mode.OFF)
                .web(WebApplicationType.NONE)
                .run(args);
    }

}
