package pl.edu.agh.student.bgrzesik.sr.lab3.team;

import com.rabbitmq.client.*;
import pl.edu.agh.student.bgrzesik.sr.lab3.admin.AdminMessageConsumer;
import pl.edu.agh.student.bgrzesik.sr.lab3.Channels;
import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;
import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.EquipmentType;
import pl.edu.agh.student.bgrzesik.sr.lab3.utils.ConnectionUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class TeamChannels extends Channels {
    public static final Logger LOGGER = Logger.getLogger(TeamChannels.class.getSimpleName());

    private final Team team;
    private final String billingQueue;
    private final TeamMessageHandler messageHandler;

    public TeamChannels(Team team, TeamMessageHandler messageHandler,
                        Connection connection,
                        Channel ordersChannel,
                        String billingQueue, Channel billingChannel,
                        String adminQueue, Channel adminChannel) throws IOException {
        super(connection, ordersChannel, billingChannel, adminQueue, adminChannel);
        this.team = team;
        this.messageHandler = messageHandler;
        this.billingQueue = billingQueue;

        // Avoid circular dependencies
        this.messageHandler.setTeamChannels(new WeakReference<>(this));

        LOGGER.info("Adding consumers");
        this.billingChannel.basicConsume(this.billingQueue, false, new BillingMessageConsumer());
    }

    public static TeamChannels connect(Team team, TeamMessageHandler messageHandler) throws IOException, TimeoutException {
        Connection conn = ConnectionUtils.newLocalhostConnection();

        LOGGER.info("Creating channel for orders");
        Channel ordersChannel = conn.createChannel();

        LOGGER.info("Declaring orders exchange");
        ordersChannel.exchangeDeclare(ORDERS_EXCHANGE, BuiltinExchangeType.TOPIC);

        LOGGER.info("Creating channel-queue pair for billing");
        Channel billingChannel = conn.createChannel();
        String billingQueue = billingChannel.queueDeclare().getQueue();

        LOGGER.info("Creating channel-queue pair for administration");
        Channel adminChannel = conn.createChannel();
        String adminQueue = adminChannel.queueDeclare().getQueue();

        AdminMessageConsumer adminMessageConsumer = new AdminMessageConsumer(adminChannel, messageHandler);
        adminMessageConsumer.register(ADMIN_KEY_TEAMS, adminQueue);

        return new TeamChannels(team, messageHandler, conn, ordersChannel,
                billingQueue, billingChannel, adminQueue, adminChannel);
    }

    public void sendOrder(EquipmentType equipmentType) throws IOException {
        String key = String.format("orders.%s", equipmentType.getId());

        Messages.Order order = Messages.Order.newBuilder()
                .setTeamId(this.team.getTeamId())
                .setEquipmentId(equipmentType.getId())
                .build();

        LOGGER.info(String.format("Sending OrderMessage (%s, %s) to %s(%s)",
                this.team.getTeamId(), equipmentType.getId(), Channels.ORDERS_EXCHANGE, key));

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .replyTo(this.billingQueue)
                .build();

        this.ordersChannel.basicPublish(Channels.ORDERS_EXCHANGE, key, props, order.toByteArray());
    }

    private class BillingMessageConsumer extends DefaultConsumer {
        private final Logger LOGGER = Logger.getLogger(TeamChannels.BillingMessageConsumer.class.getSimpleName());

        public BillingMessageConsumer() {
            super(TeamChannels.this.billingChannel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            LOGGER.info(String.format("handling delivery %s", envelope.getDeliveryTag()));

            Messages.Bill bill = Messages.Bill.parseFrom(body);
            TeamChannels.this.messageHandler.onBillMessage(bill, properties.getReplyTo());

            LOGGER.info(String.format("acknowledging %s", envelope.getDeliveryTag()));
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        }
    }

    public Team getTeam() {
        return team;
    }

}
