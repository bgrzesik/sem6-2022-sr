package pl.edu.agh.student.bgrzesik.sr.lab3.utils;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.StrictExceptionHandler;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class ConnectionUtils {
    private static final Logger LOGGER = Logger.getLogger(ConnectionUtils.class.getSimpleName());

    public static Connection newLocalhostConnection() throws IOException, TimeoutException {
        LOGGER.info("Connecting to localhost");
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setExceptionHandler(new StrictExceptionHandler());
        connectionFactory.setHost("localhost");
        return connectionFactory.newConnection();
    }
}
