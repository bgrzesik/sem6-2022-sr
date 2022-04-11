package pl.edu.agh.student.bgrzesik.sr.lab3.equipment;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Equipment {
    public static final Logger LOGGER = Logger.getLogger(Equipment.class.getSimpleName());

    private final EquipmentType equipmentType;
    private final int requiredAmount;
    private final AtomicInteger ownedAmount = new AtomicInteger(0);

    public Equipment(EquipmentType equipmentType, int requiredAmount) {
        this.equipmentType = equipmentType;
        this.requiredAmount = requiredAmount;
    }

    public void addOneOwned() {
        LOGGER.info(String.format("adding one owned %s", equipmentType.getId()));

        if (this.ownedAmount.getAndIncrement() >= this.requiredAmount) {
            LOGGER.severe(String.format("Adding more then required %s", this.equipmentType.getId()));
            throw new RuntimeException("More then required equipment count");
        }

        LOGGER.info(String.format("owning %s %d/%d",
                equipmentType.getId(), ownedAmount.get(), requiredAmount));
    }

    public boolean hasRequiredCount() {
        return this.requiredAmount == this.ownedAmount.get();
    }

    public EquipmentType getEquipmentType() {
        return this.equipmentType;
    }

    public int getRequiredAmount() {
        return this.requiredAmount;
    }

    public int getOwnedAmount() {
        return this.ownedAmount.get();
    }

    public void setOwnedAmount(int ownedAmount) {
        this.ownedAmount.set(ownedAmount);
    }
}
