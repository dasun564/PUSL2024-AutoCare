package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Durable components fitted to a vehicle: brake pads, alternators, belts,
 * suspension parts.
 *
 * Manufacturer and warranty are mandatory here and meaningless for a
 * consumable, which is the reason this hierarchy uses JOINED - see {@link Part}.
 */
@Entity
@Table(name = "mechanical_part")
@PrimaryKeyJoinColumn(name = "part_id")
public class MechanicalPart extends Part {

    @Column(nullable = false, length = 80)
    private String manufacturer;

    @Column(name = "warranty_months", nullable = false)
    private int warrantyMonths;

    /** True for parts that must be fitted by a qualified technician. */
    @Column(nullable = false)
    private boolean requiresSpecialistFitting;

    protected MechanicalPart() {
        // required by JPA
    }

    public MechanicalPart(String partNumber, String name, PartCategory category,
                          BigDecimal unitPrice, int stockQuantity, int reorderLevel,
                          String manufacturer, int warrantyMonths, boolean requiresSpecialistFitting) {
        super(partNumber, name, category, unitPrice, stockQuantity, reorderLevel);
        this.manufacturer = manufacturer;
        this.warrantyMonths = warrantyMonths;
        this.requiresSpecialistFitting = requiresSpecialistFitting;
    }

    @Override
    public String getPartType() {
        return "Mechanical";
    }

    @Override
    public String getHandlingNote() {
        String warranty = manufacturer + " - " + warrantyMonths + " month warranty.";
        return requiresSpecialistFitting ? warranty + " Specialist fitting required." : warranty;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public boolean isRequiresSpecialistFitting() {
        return requiresSpecialistFitting;
    }

    public void setRequiresSpecialistFitting(boolean requiresSpecialistFitting) {
        this.requiresSpecialistFitting = requiresSpecialistFitting;
    }
}
