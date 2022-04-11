package pl.edu.agh.student.bgrzesik.sr.lab3;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public abstract class Channels {
    private static final Logger LOGGER = Logger.getLogger(Channels.class.getSimpleName());
//    public static final String BILLING_EXCHANGE = "billing";
    public static final String ORDERS_EXCHANGE = "orders";
    public static final String ADMIN_EXCHANGE = "admin";

    public static final String ADMIN_KEY_ALL = "admin.all";
    public static final String ADMIN_KEY_TEAMS = "admin.teams";
    public static final String ADMIN_KEY_SUPPLIERS = "admin.suppliers";
    public static final String ADMIN_KEY_CC_BILLS = "admin.cc_bills";

    protected Connection connection;
    protected Channel ordersChannel;
    protected Channel billingChannel;
    protected Channel adminChannel;
    protected final String adminQueue;

    public Channels(Connection connection, Channel ordersChannel, Channel billingChannel, String adminQueue, Channel adminChannel) {
        this.adminQueue = adminQueue;
        Objects.requireNonNull(connection);
        Objects.requireNonNull(adminChannel);
        this.connection = connection;
        this.ordersChannel = ordersChannel;
        this.billingChannel = billingChannel;
        this.adminChannel = adminChannel;
    }

    public Channel getOrdersChannel() {
        return ordersChannel;
    }

    public Channel getBillingChannel() {
        return billingChannel;
    }

    public Channel getAdminChannel() {
        return adminChannel;
    }

    public String getAdminQueue() {
        return adminQueue;
    }

    public void close() throws IOException, TimeoutException {
        LOGGER.info("Closing channels");

        if (billingChannel != null && billingChannel.isOpen()) {
            billingChannel.close();
        }
        if (ordersChannel != null && ordersChannel.isOpen()) {
            ordersChannel.close();
        }
        if (adminChannel != null && adminChannel.isOpen()) {
            adminChannel.close();
        }
        if (connection != null) {
            connection.close();
        }

        billingChannel = null;
        ordersChannel = null;
        adminChannel = null;
        connection = null;
    }
}
