package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Fluids and single-use items: engine oil, coolant, filters, brake fluid.
 *
 * Because the strategy is JOINED, these two columns live in their own table
 * and are declared NOT NULL - a consumable without a shelf life cannot be
 * stored at all.
 */
@Entity
@Table(name = "consumable_part")
@PrimaryKeyJoinColumn(name = "part_id")
public class ConsumablePart extends Part {

    @Column(name = "shelf_life_months", nullable = false)
    private int shelfLifeMonths;

    /** Requires controlled disposal, e.g. used oil and brake fluid. */
    @Column(nullable = false)
    private boolean hazardous;

    protected ConsumablePart() {
        // required by JPA
    }

    public ConsumablePart(String partNumber, String name, PartCategory category,
                          BigDecimal unitPrice, int stockQuantity, int reorderLevel,
                          int shelfLifeMonths, boolean hazardous) {
        super(partNumber, name, category, unitPrice, stockQuantity, reorderLevel);
        this.shelfLifeMonths = shelfLifeMonths;
        this.hazardous = hazardous;
    }

    @Override
    public String getPartType() {
        return "Consumable";
    }

    @Override
    public String getHandlingNote() {
        return hazardous
                ? "Hazardous - controlled disposal required. Shelf life " + shelfLifeMonths + " months."
                : "Shelf life " + shelfLifeMonths + " months.";
    }

    public int getShelfLifeMonths() {
        return shelfLifeMonths;
    }

    public void setShelfLifeMonths(int shelfLifeMonths) {
        this.shelfLifeMonths = shelfLifeMonths;
    }

    public boolean isHazardous() {
        return hazardous;
    }

    public void setHazardous(boolean hazardous) {
        this.hazardous = hazardous;
    }
}
