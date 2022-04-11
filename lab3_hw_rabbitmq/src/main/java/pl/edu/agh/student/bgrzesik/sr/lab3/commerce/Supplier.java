package pl.edu.agh.student.bgrzesik.sr.lab3.commerce;

import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.EquipmentType;

import java.util.List;
import java.util.logging.Logger;

public class Supplier {
    public static final Logger LOGGER = Logger.getLogger(Supplier.class.getSimpleName());

    private final String supplierId;
    private final String supplierName;
    private final List<EquipmentType> offer;

    public Supplier(String supplierId, String supplierName, List<EquipmentType> offer) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.offer = offer;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public List<EquipmentType> getOffer() {
        return offer;
    }
}
