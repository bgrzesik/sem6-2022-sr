package pl.edu.agh.student.sr.lab5.server.grpc.service;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.edu.agh.student.sr.lab5.proto.ReflectionDemoGrpc;

@Service
public class ReflectionDemoRpcImpl extends ReflectionDemoGrpc.ReflectionDemoImplBase {
    private static final Logger log = LoggerFactory.getLogger(ReflectionDemoRpcImpl.class);
    private static final Empty EMPTY = Empty.newBuilder().build();

    @Override
    public void test1(Empty request, StreamObserver<Empty> responseObserver) {
        log.debug("test1");
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    @Override
    public void test2(Empty request, StreamObserver<Empty> responseObserver) {
        log.debug("test2");
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    @Override
    public void test3(Empty request, StreamObserver<Empty> responseObserver) {
        log.debug("test3");
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

}
