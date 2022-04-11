package pl.edu.agh.student.bgrzesik.sr.lab3.team;

import pl.edu.agh.student.bgrzesik.sr.lab3.Messages;
import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.Equipment;
import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.EquipmentType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Team {
    public static final Logger LOGGER = Logger.getLogger(Team.class.getSimpleName());

    private final String teamId;
    private final String teamName;
    private final List<Equipment> equipment;
    private final List<Messages.Bill> bills = new ArrayList<>();

    public Team(String teamId, String teamName, List<Equipment> equipment) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.equipment = Collections.unmodifiableList(equipment);

        boolean unique = this.equipment.size() == this.equipment.stream()
                .map(Equipment::getEquipmentType)
                .collect(Collectors.toUnmodifiableSet())
                .size();
        if (!unique) {
            throw new IllegalArgumentException("Equipment Types are not unique");
        }
    }

    public boolean isFullEquipped() {
        return equipment.stream()
                .allMatch(Equipment::hasRequiredCount);
    }

    public void addOwnedEquipment(EquipmentType equipmentType) {
        this.getEquipment(equipmentType)
                .addOneOwned();
    }

    public void addBill(Messages.Bill bill) {
        String equipmentId = bill.getEquipmentId();
        EquipmentType equipmentType = EquipmentType.fromId(equipmentId);
        this.addOwnedEquipment(equipmentType);
        this.bills.add(bill);
    }

    public String getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public List<Equipment> getEquipment() {
        return equipment;
    }

    public Equipment getEquipment(EquipmentType equipmentType) {
        Optional<Equipment> eq = equipment.stream()
                .filter(e -> e.getEquipmentType() == equipmentType)
                .findAny();

        if (eq.isEmpty()) {
            throw new IllegalArgumentException("Non required equipment");
        }

        return eq.get();
    }

    public List<Messages.Bill> getBills() {
        return bills;
    }
}
