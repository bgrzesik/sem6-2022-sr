package pl.edu.agh.student.bgrzesik.sr.lab3.commerce;

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

public class SupplierChannels extends Channels {
    private static final Logger LOGGER = Logger.getLogger(SupplierChannels.class.getSimpleName());
    private final Supplier supplier;
    private final SupplierMessageHandler messageHandler;

    public SupplierChannels(Supplier supplier, SupplierMessageHandler messageHandler,
                            Connection connection, Channel ordersChannel, Channel billingChannel,
                            String adminQueue, Channel adminChannel) throws IOException {
        super(connection, ordersChannel, billingChannel, adminQueue, adminChannel);
        this.supplier = supplier;
        this.messageHandler = messageHandler;
        this.connection = connection;

        this.messageHandler.setSupplierChannels(new WeakReference<>(this));

        OrderMessageConsumer orderMessageConsumer = new OrderMessageConsumer();
        for (EquipmentType equipmentType : supplier.getOffer()) {
            String key = equipmentType.getQueueName();
            this.ordersChannel.basicConsume(key, false, orderMessageConsumer);
        }

    }

    public static SupplierChannels connect(Supplier supplier, SupplierMessageHandler messageHandler) throws IOException, TimeoutException {
        Connection conn = ConnectionUtils.newLocalhostConnection();

        LOGGER.info("Creating channel pair for billing");
        Channel ordersChannel = conn.createChannel();

        LOGGER.info("Declaring orders exchange");
        ordersChannel.exchangeDeclare(ORDERS_EXCHANGE, BuiltinExchangeType.TOPIC);
        for (EquipmentType equipmentType : supplier.getOffer()) {
            String key = equipmentType.getQueueName();
            LOGGER.info(String.format("Declaring and binding queue %s", key));
            ordersChannel.queueDeclare(key, false, false, false, null);
            ordersChannel.queueBind(key, ORDERS_EXCHANGE, key);
        }

        LOGGER.info("Creating channel for billing");
        Channel billingChannel = conn.createChannel();

        LOGGER.info("Creating channel-queue pair for administration");
        Channel adminChannel = conn.createChannel();
        String adminQueue = adminChannel.queueDeclare().getQueue();

        AdminMessageConsumer adminMessageConsumer = new AdminMessageConsumer(adminChannel, messageHandler);
        adminMessageConsumer.register(ADMIN_KEY_SUPPLIERS, adminQueue);

        return new SupplierChannels(supplier, messageHandler, conn,
                ordersChannel, billingChannel, adminQueue, adminChannel);
    }

    public void sendBill(String recipient, String teamId, EquipmentType equipmentType) throws IOException {
        Messages.Bill bill = Messages.Bill.newBuilder()
                .setTeamId(teamId)
                .setSupplierId(supplier.getSupplierId())
                .setEquipmentId(equipmentType.getId())
                .build();

        LOGGER.info(String.format("Sending BillMessage (%s, %s) to %s/%s",
                supplier.getSupplierId(), equipmentType.getId(), teamId, recipient));

        this.billingChannel.basicPublish("", recipient, null, bill.toByteArray());

        // This is ugly, but that's the task requirement
        this.adminChannel.basicPublish(ADMIN_EXCHANGE, ADMIN_KEY_CC_BILLS, null, bill.toByteArray());
    }

    private class OrderMessageConsumer extends DefaultConsumer {
        private final Logger LOGGER = Logger.getLogger(OrderMessageConsumer.class.getSimpleName());

        public OrderMessageConsumer() {
            super(SupplierChannels.this.ordersChannel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
            LOGGER.info(String.format("handling delivery %s", envelope.getDeliveryTag()));

            Messages.Order order = Messages.Order.parseFrom(body);
            SupplierChannels.this.messageHandler.onOrderMessage(order, properties.getReplyTo());

            LOGGER.info(String.format("acknowledging %s", envelope.getDeliveryTag()));
            getChannel().basicAck(envelope.getDeliveryTag(), false);
        }
    }

    public Supplier getSupplier() {
        return supplier;
    }
}
