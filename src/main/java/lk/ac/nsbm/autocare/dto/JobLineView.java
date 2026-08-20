package lk.ac.nsbm.autocare.dto;

import lk.ac.nsbm.autocare.entity.JobLine;

import java.math.BigDecimal;

/** Read-only invoice line. Immutable record: the view cannot alter a charge. */
public record JobLineView(
        Long id,
        String partNumber,
        String partName,
        String partType,
        int quantity,
        BigDecimal unitPriceAtTime,
        BigDecimal lineTotal,
        int stockOnHand,
        boolean shortOfStock) {

    public static JobLineView from(JobLine line) {
        int onHand = line.getPart().getStockQuantity();
        return new JobLineView(
                line.getId(),
                line.getPart().getPartNumber(),
                line.getPart().getName(),
                line.getPart().getPartType(),
                line.getQuantity(),
                line.getUnitPriceAtTime(),
                line.getLineTotal(),
                onHand,
                onHand < line.getQuantity());
    }
}
