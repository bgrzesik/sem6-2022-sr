package pl.edu.agh.student.sr.lab5.server.transport;

import io.grpc.*;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import pl.edu.agh.student.sr.lab5.server.service.GroupRepository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;

@Service
public class GrpcServer implements DisposableBean, CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(GrpcServer.class);

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private List<BindableService> grpcServices;

    @Autowired
    private GrpcConfig config;

    private Server grpcServer;

    @PostConstruct
    public void start() throws InterruptedException {
        log.debug("start");
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(config.getPort());
        for (BindableService service : grpcServices) {
            log.info("Binding service {}", service.getClass().getName());
            serverBuilder = serverBuilder.addService(service);
        }

        serverBuilder.addService(ProtoReflectionService.newInstance());
        log.info("GRPC Server built");
        grpcServer = serverBuilder.build();
    }

    @Override
    @PreDestroy
    public void destroy() throws Exception {
        log.debug("destroy");
        log.info("Shutting down GRPC server");
        grpcServer.shutdown();
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("run: args={}", Arrays.toString(args));
        log.info("Starting GRPC server on port {}", config.getPort());
        grpcServer.start();
        log.info("Awaiting termination of GRPC server");
        grpcServer.awaitTermination();
    }
}
