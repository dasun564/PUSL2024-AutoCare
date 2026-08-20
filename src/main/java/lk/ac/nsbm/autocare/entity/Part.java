package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * An item held in the garage's parts inventory.
 *
 * JPA INHERITANCE STRATEGY: JOINED.
 *
 * Justification for AutoCare specifically - the two subtypes carry attributes
 * that are genuinely mandatory for that subtype and meaningless for the other:
 * a {@link ConsumablePart} must have a shelf life, a {@link MechanicalPart}
 * must have a manufacturer. With JOINED, each subclass gets its own table and
 * the database can declare those columns NOT NULL, so the integrity rule is
 * enforced by the database itself no matter which application writes the row.
 * A single shared table would have to make every subclass column nullable and
 * demote that guarantee to application-level validation.
 *
 * The cost is a join: loading the whole inventory polymorphically reads the
 * parent table plus both child tables. That is acceptable here because the
 * inventory is small and is read a page at a time, whereas an inventory
 * miscount caused by a missing shelf life would be a safety problem.
 *
 * ENCAPSULATION: stockQuantity has a getter but no setter. Stock only moves
 * through consumeStock and restock, which enforce that it can never go
 * negative. See ServiceJobServiceImpl.closeJob for the only caller.
 */
@Entity
@Table(name = "part")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String partNumber;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private PartCategory category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** No setter - see consumeStock / restock. */
    @Column(nullable = false)
    private int stockQuantity;

    /** Stock level at which staff should reorder. Display only. */
    @Column(nullable = false)
    private int reorderLevel;

    /** Soft-delete flag: a withdrawn part stays referenced by historic jobs. */
    @Column(nullable = false)
    private boolean active = true;

    /** Optimistic lock counter, paired with the pessimistic row lock at close time. */
    @Version
    private Long version;

    protected Part() {
        // required by JPA
    }

    protected Part(String partNumber, String name, PartCategory category,
                   BigDecimal unitPrice, int stockQuantity, int reorderLevel) {
        this.partNumber = partNumber;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.reorderLevel = reorderLevel;
    }

    /** POLYMORPHIC: each subtype names itself, so views never test the class. */
    public abstract String getPartType();

    /** POLYMORPHIC: subtype-specific advice shown on the parts screens. */
    public abstract String getHandlingNote();

    public boolean hasStock(int required) {
        return active && stockQuantity >= required;
    }

    public boolean isBelowReorderLevel() {
        return stockQuantity <= reorderLevel;
    }

    /**
     * Takes {@code quantity} units out of stock, guarding its own invariant so
     * the count can never go negative regardless of what the caller believes.
     */
    public void consumeStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity consumed must be positive, was " + quantity);
        }
        if (quantity > stockQuantity) {
            throw new IllegalStateException(
                    "Cannot consume " + quantity + " of part " + partNumber + "; only " + stockQuantity + " in stock");
        }
        this.stockQuantity -= quantity;
    }

    /** Returns units to stock, e.g. when a closed job is reopened or corrected. */
    public void restock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restock quantity must be positive, was " + quantity);
        }
        this.stockQuantity += quantity;
    }

    public Long getId() {
        return id;
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

    public PartCategory getCategory() {
        return category;
    }

    public void setCategory(PartCategory category) {
        this.category = category;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Direct stock correction by staff (stocktake), separate from job consumption. */
    public void correctStockTo(int countedQuantity) {
        if (countedQuantity < 0) {
            throw new IllegalArgumentException("Counted stock cannot be negative");
        }
        this.stockQuantity = countedQuantity;
    }
}
