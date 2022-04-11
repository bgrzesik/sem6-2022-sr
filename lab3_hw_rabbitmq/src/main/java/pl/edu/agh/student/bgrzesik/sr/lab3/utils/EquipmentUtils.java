package pl.edu.agh.student.bgrzesik.sr.lab3.utils;

import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.Equipment;
import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.EquipmentType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EquipmentUtils {
    public static List<EquipmentType> parseTypesCLI(String arg) {
        List<EquipmentType> types = new ArrayList<>();

        for (String req : arg.split(",")) {
            EquipmentType equipmentType = EquipmentType.fromId(req);
            types.add(equipmentType);
        }

        return types;
    }

    public static List<Equipment> parseEquipmentCLI(String arg) {
        List<Equipment> equipmentList = new ArrayList<>();

        for (String req : arg.split(",")) {
            String[] pair = req.split(":");
            EquipmentType equipmentType = EquipmentType.fromId(pair[0]);
            Equipment equipment;

            if (pair.length == 1) {
                equipment = new Equipment(equipmentType, 1);
            } else if (pair.length == 2) {
                int amount = Integer.parseInt(pair[1]);
                equipment = new Equipment(equipmentType, amount);
            } else {
                String msg = String.format("Unable to parse equipment list: %s", Arrays.toString(pair));
                throw new IllegalArgumentException(msg);
            }

            equipmentList.add(equipment);
        }

        return equipmentList;
    }

}
