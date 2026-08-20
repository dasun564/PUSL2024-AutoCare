package lk.ac.nsbm.autocare.dto;

import lk.ac.nsbm.autocare.entity.Part;

import java.math.BigDecimal;

/**
 * Read-only projection of an inventory part. Used by the REST API and the
 * parts screens so internal fields ({@code version}) and JPA proxies are never
 * exposed.
 */
public record PartView(
        Long id,
        String partNumber,
        String name,
        String category,
        String partType,
        String handlingNote,
        BigDecimal unitPrice,
        int stockQuantity,
        int reorderLevel,
        boolean lowStock,
        boolean active) {

    public static PartView from(Part part) {
        return new PartView(
                part.getId(),
                part.getPartNumber(),
                part.getName(),
                part.getCategory().getName(),
                part.getPartType(),
                part.getHandlingNote(),
                part.getUnitPrice(),
                part.getStockQuantity(),
                part.getReorderLevel(),
                part.isBelowReorderLevel(),
                part.isActive());
    }
}
