package pl.edu.agh.student.bgrzesik.sr.lab3.admin;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;

import java.io.IOException;
import java.util.logging.Logger;

public class AdminOrderConsumer extends DefaultConsumer {
    private static final Logger LOGGER = Logger.getLogger(AdminOrderConsumer.class.getSimpleName());

    public AdminOrderConsumer(Channel channel) {
        super(channel);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        Messages.Order orderMessage = Messages.Order.parseFrom(body);

        LOGGER.info(String.format("Order: teamId=%-20s equipmentId=%-20s",
                orderMessage.getTeamId(), orderMessage.getEquipmentId()));
    }
}
