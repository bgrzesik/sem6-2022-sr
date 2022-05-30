package pl.edu.agh.student.sr.lab5.server.transport;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
//@ConfigurationProperties("grpc")
public class GrpcConfig {
    private int port = 8080;

    public int getPort() {
        return port;
    }

}
