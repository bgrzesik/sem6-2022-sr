package pl.edu.agh.student.bgrzesik.sr.lab3.commerce;

import pl.edu.agh.student.bgrzesik.sr.lab3.equipment.EquipmentType;
import pl.edu.agh.student.bgrzesik.sr.lab3.utils.EquipmentUtils;
import pl.edu.agh.student.bgrzesik.sr.lab3.utils.NameUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class SupplierMain {
    private static final Logger LOGGER = Logger.getLogger(SupplierMain.class.getSimpleName());

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: <supplier name> <offer>");
        }

        String supplierName = args[0];
        String supplierId = NameUtils.nameToId(supplierName);

        List<EquipmentType> offer = EquipmentUtils.parseTypesCLI(args[1]);

        LOGGER.info(String.format("Starting supplier service %s with offer %s", supplierId, offer));

        Supplier supplier = new Supplier(supplierId, supplierName, offer);
        try {
            SupplierMessageHandler messageHandler = new SupplierMessageHandler(supplier);
            SupplierChannels supplierChannels = SupplierChannels.connect(supplier, messageHandler);

            while (messageHandler.isAlive()) {
                Thread.yield();
            }

            supplierChannels.close();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
