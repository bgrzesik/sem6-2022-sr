package pl.edu.agh.student.bgrzesik.sr.lab3.admin;

import com.rabbitmq.client.*;
import pl.edu.agh.student.bgrzesik.sr.lab3.Channels;
import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;

import java.io.IOException;
import java.util.logging.Logger;

public class AdminMessageConsumer extends DefaultConsumer {
    public static final Logger LOGGER = Logger.getLogger(AdminMessageConsumer.class.getSimpleName());
    private IAdminMessageHandler messageHandler;

    public AdminMessageConsumer(Channel channel, IAdminMessageHandler messageHandler) {
        super(channel);
        this.messageHandler = messageHandler;
    }

    public void register(String specificKey, String adminQueue) throws IOException {
        Channel adminChannel = getChannel();

        LOGGER.info("Declaring admin exchange");
        adminChannel.exchangeDeclare(Channels.ADMIN_EXCHANGE, BuiltinExchangeType.DIRECT);
        adminChannel.queueBind(adminQueue, Channels.ADMIN_EXCHANGE, Channels.ADMIN_KEY_ALL);
        if (specificKey != null) {
            adminChannel.queueBind(adminQueue, Channels.ADMIN_EXCHANGE, specificKey);
        }
        adminChannel.basicConsume(adminQueue, true, this);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        LOGGER.info(String.format("Handling delivery %s", envelope.getDeliveryTag()));

        Messages.AdminMessage adminMessage = Messages.AdminMessage.parseFrom(body);
        this.messageHandler.onAdminMessage(adminMessage, properties.getReplyTo());

        LOGGER.info(String.format("Acknowledging message %s", envelope.getDeliveryTag()));
    }

    public IAdminMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(IAdminMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }
}
