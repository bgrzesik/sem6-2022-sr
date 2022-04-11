package pl.edu.agh.student.bgrzesik.sr.lab3.team;

import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;
import pl.edu.agh.student.bgrzesik.sr.lab3.admin.IAdminMessageHandler;
import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.Equipment;
import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.EquipmentType;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.logging.Logger;

public class TeamMessageHandler implements IAdminMessageHandler  {
    public static final Logger LOGGER = Logger.getLogger(TeamMessageHandler.class.getSimpleName());
    private final Team team;
    private WeakReference<TeamChannels> teamChannels = null;
    private final Object billLock = new Object();

    public TeamMessageHandler(Team team) {
        this.team = team;
    }

    public void orderAllAndWait() throws IOException, InterruptedException {
        for (Equipment equipment : team.getEquipment()) {
            for (int i = 0; i < equipment.getRequiredAmount(); i++) {
                this.orderSingle(equipment.getEquipmentType());
            }
        }

        synchronized (this.billLock) {
            while (!this.team.isFullEquipped()) {
                this.billLock.wait();
            }
        }

        LOGGER.info("Owns all required equipment");
    }

    public void onBillMessage(Messages.Bill bill, String replyTo) {
        LOGGER.info(String.format("Got bill from %s for %s",
                bill.getSupplierId(), bill.getEquipmentId()));

        this.team.addBill(bill);

        synchronized (this.billLock) {
            this.billLock.notifyAll();
        }
    }

    @Override
    public void onAdminMessage(Messages.AdminMessage adminMessage, String replyTo) {
        String content = adminMessage.getContent();
        LOGGER.info("Admin Message: " + content);
    }

    private void orderSingle(EquipmentType equipmentType) throws IOException {
        TeamChannels channels = getStrongTeamChannels();
        channels.sendOrder(equipmentType);
    }

    public void setTeamChannels(WeakReference<TeamChannels> teamChannels) {
        this.teamChannels = teamChannels;
    }

    public TeamChannels getStrongTeamChannels() {
        TeamChannels channels;
        if (this.teamChannels == null || (channels = this.teamChannels.get()) == null) {
            LOGGER.severe("channels died");
            throw new RuntimeException("TeamChannels died");
        }

        return channels;
    }

    public WeakReference<TeamChannels> getWeakTeamChannels() {
        return teamChannels;
    }

    public Team getTeam() {
        return team;
    }
}
