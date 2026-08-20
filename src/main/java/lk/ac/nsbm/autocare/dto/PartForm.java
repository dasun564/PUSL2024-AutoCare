package lk.ac.nsbm.autocare.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Input DTO for creating and editing inventory parts, validated with Bean
 * Validation (JSR-380).
 *
 * A dedicated form object is used instead of binding onto the Part entity.
 * That keeps unvalidated request data out of the persistence context, and
 * makes it impossible for a crafted request to write fields the form was never
 * meant to expose - {@code stockQuantity}, {@code active} and {@code version}
 * have no counterpart here at all. Stock is corrected through its own
 * stocktake action, not by editing a part.
 */
public class PartForm {

    private Long id;

    @NotBlank(message = "Part number is required")
    @Pattern(regexp = "^[A-Z0-9][A-Z0-9-]{2,29}$",
             message = "Part number must be 3-30 characters: capitals, digits and hyphens only")
    private String partNumber;

    @NotBlank(message = "Part name is required")
    @Size(max = 120, message = "Part name must be 120 characters or fewer")
    private String name;

    @NotNull(message = "Choose a category")
    private Long categoryId;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be at least 0.01")
    @Digits(integer = 8, fraction = 2, message = "Unit price may have at most 2 decimal places")
    private BigDecimal unitPrice;

    @NotNull(message = "Opening stock is required")
    @Min(value = 0, message = "Opening stock cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    /** CONSUMABLE or MECHANICAL - selects which Part subclass is instantiated. */
    @NotBlank(message = "Choose a part type")
    @Pattern(regexp = "CONSUMABLE|MECHANICAL", message = "Part type must be CONSUMABLE or MECHANICAL")
    private String partType = "MECHANICAL";

    // --- consumable-specific ---

    @Min(value = 1, message = "Shelf life must be at least 1 month")
    private Integer shelfLifeMonths;

    private boolean hazardous;

    // --- mechanical-specific ---

    @Size(max = 80, message = "Manufacturer must be 80 characters or fewer")
    private String manufacturer;

    @Min(value = 0, message = "Warranty cannot be negative")
    private Integer warrantyMonths;

    private boolean requiresSpecialistFitting;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public String getPartType() {
        return partType;
    }

    public void setPartType(String partType) {
        this.partType = partType;
    }

    public Integer getShelfLifeMonths() {
        return shelfLifeMonths;
    }

    public void setShelfLifeMonths(Integer shelfLifeMonths) {
        this.shelfLifeMonths = shelfLifeMonths;
    }

    public boolean isHazardous() {
        return hazardous;
    }

    public void setHazardous(boolean hazardous) {
        this.hazardous = hazardous;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(Integer warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public boolean isRequiresSpecialistFitting() {
        return requiresSpecialistFitting;
    }

    public void setRequiresSpecialistFitting(boolean requiresSpecialistFitting) {
        this.requiresSpecialistFitting = requiresSpecialistFitting;
    }
}
