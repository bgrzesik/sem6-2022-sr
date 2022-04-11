package pl.edu.agh.student.bgrzesik.sr.lab3.admin;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;

import java.io.IOException;
import java.util.logging.Logger;

public class AdminBillConsumer extends DefaultConsumer {
    private static final Logger LOGGER = Logger.getLogger(AdminBillConsumer.class.getSimpleName());

    public AdminBillConsumer(Channel channel) {
        super(channel);
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        Messages.Bill billMessage = Messages.Bill.parseFrom(body);

        LOGGER.info(String.format("Bill:  teamId=%-20s equipmentId=%-20s supplierId=%-20s",
                billMessage.getTeamId(), billMessage.getEquipmentId(), billMessage.getSupplierId()));
    }
}
