package pl.edu.agh.student.bgrzesik.sr.lab3.admin;

import com.rabbitmq.client.*;
import pl.edu.agh.student.bgrzesik.sr.lab3.Channels;
import pl.edu.agh.student.bgrzesik.sr.lab3.utils.ConnectionUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class AdminSniffer extends DefaultConsumer {
    public static final Logger LOGGER = Logger.getLogger(AdminSniffer.class.getSimpleName());

    public AdminSniffer(Channel channel) {
        super(channel);
    }

    public static void sniff() throws IOException, TimeoutException {
        Connection conn = ConnectionUtils.newLocalhostConnection();
        Channel adminChannel = conn.createChannel();

        String orderQueue = adminChannel.queueDeclare().getQueue();
        LOGGER.info("Declaring orders exchange");
        adminChannel.exchangeDeclare(Channels.ORDERS_EXCHANGE, BuiltinExchangeType.TOPIC);
        adminChannel.queueBind(orderQueue, Channels.ORDERS_EXCHANGE, "orders.*");
        LOGGER.info("Adding order consumer");
        adminChannel.basicConsume(orderQueue, true, new AdminOrderConsumer(adminChannel));

        String billQueue = adminChannel.queueDeclare().getQueue();
        LOGGER.info("Declaring admin exchange");
        adminChannel.exchangeDeclare(Channels.ADMIN_EXCHANGE, BuiltinExchangeType.DIRECT);
        adminChannel.queueBind(billQueue, Channels.ADMIN_EXCHANGE, Channels.ADMIN_KEY_CC_BILLS);
        LOGGER.info("Adding bill consumer");
        adminChannel.basicConsume(billQueue, true, new AdminBillConsumer(adminChannel));

        LOGGER.info("Sniffin...");
        // Leaking everything so client can CTRL-C to exit
    }
}
