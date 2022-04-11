package pl.edu.agh.student.bgrzesik.sr.lab3.team;

import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;
import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.Equipment;
import pl.edu.agh.student.bgrzesik.sr.lab3.utils.EquipmentUtils;
import pl.edu.agh.student.bgrzesik.sr.lab3.utils.NameUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class TeamMain {
    private static final Logger LOGGER = Logger.getLogger(TeamMain.class.getSimpleName());

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: <team name> <required equipment>");
        }

        String teamName = args[0];
        String teamId = NameUtils.nameToId(teamName);
        List<Equipment> equipment = EquipmentUtils.parseEquipmentCLI(args[1]);

        LOGGER.info(String.format("Starting team service %s with requirements %s", teamId, args[1]));

        Team team = new Team(teamId, teamName, equipment);
        try {
            TeamMessageHandler messageHandler = new TeamMessageHandler(team);
            TeamChannels channels = TeamChannels.connect(team, messageHandler);

            messageHandler.orderAllAndWait();

            LOGGER.info("Billing: ");
            for (Messages.Bill bill : team.getBills()) {
                LOGGER.info(String.format("\tequipmentId=%s, supplierId=%s",
                        bill.getEquipmentId(), bill.getSupplierId()));
            }

            LOGGER.info("Sleeping 10s...");
            Thread.sleep(10000);

            channels.close();
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
