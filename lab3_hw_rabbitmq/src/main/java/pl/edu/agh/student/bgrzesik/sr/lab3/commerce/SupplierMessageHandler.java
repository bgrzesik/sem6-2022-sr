package pl.edu.agh.student.bgrzesik.sr.lab3.commerce;

import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;
import pl.edu.agh.student.bgrzesik.sr.lab3.admin.IAdminMessageHandler;
import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.EquipmentType;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SupplierMessageHandler implements IAdminMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(SupplierMessageHandler.class.getSimpleName());

    private final Supplier supplier;
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private WeakReference<SupplierChannels> supplierChannels = null;

    public SupplierMessageHandler(Supplier supplier) {
        this.supplier = supplier;
    }

    public void onOrderMessage(Messages.Order order, String replyTo) throws IOException {
        EquipmentType equipmentType = EquipmentType.fromId(order.getEquipmentId());
        if (!supplier.getOffer().contains(equipmentType)) {
            LOGGER.severe("Requested item not in the offer");
            throw new RuntimeException("Requested item not in the offer");
        }

        LOGGER.info(String.format("Sending bill to %s for %s",
                order.getTeamId(), order.getEquipmentId()));

        SupplierChannels channels = getStrongSupplierChannels();
        channels.sendBill(replyTo, order.getTeamId(), equipmentType);
    }

    @Override
    public void onAdminMessage(Messages.AdminMessage adminMessage, String replyTo) {
        String content = adminMessage.getContent();
        LOGGER.info("Admin Message: " + content);
    }

    public void setSupplierChannels(WeakReference<SupplierChannels> supplierChannels) {
        this.supplierChannels = supplierChannels;
    }

    public SupplierChannels getStrongSupplierChannels() {
        SupplierChannels channels;
        if (this.supplierChannels == null || (channels = this.supplierChannels.get()) == null) {
            LOGGER.severe("channels died");
            throw new RuntimeException("SupplierChannels died");
        }

        return channels;
    }

    public WeakReference<SupplierChannels> getWeakSupplierChannels() {
        return supplierChannels;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public boolean isAlive() {
        return this.alive.get();
    }

}
