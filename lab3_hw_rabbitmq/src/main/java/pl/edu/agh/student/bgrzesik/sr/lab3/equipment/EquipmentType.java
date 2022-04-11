package pl.edu.agh.student.bgrzesik.sr.lab3.equipment;

import java.util.HashMap;
import java.util.Map;

public enum EquipmentType {
    O2_CYLINDER("o2-cylinder"),
    SHOES("shoes"),
    BACKPACK("backpack"),
    WARM_JACKET("warm-jacket"),
    ROPES("ropes"),
    TENT("tent"),
    ;
    private static final Map<String, EquipmentType> idCache;

    private final String id;
    private final String queueName;

    EquipmentType(String id) {
        this.id = id;
        this.queueName = String.format("orders.%s", this.id);
    }

    public String getId() {
        return this.id;
    }

    public String getQueueName() {
        return this.queueName;
    }

    public static EquipmentType fromId(String id) {
        EquipmentType equipmentType = idCache.get(id);
        if (equipmentType == null) {
            throw new IllegalArgumentException("invalid equipment id");
        }
        return equipmentType;
    }

    static {
        idCache = new HashMap<>();

        for (EquipmentType type : EquipmentType.values()) {
            idCache.put(type.getId(), type);
        }
    }

}
