package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One part, in one quantity, on one {@link ServiceJob} - an invoice line.
 *
 * MANY-TO-ONE to both the job and the part. The unit price is copied in at the
 * moment the line is created rather than read through to the part, so
 * repricing the inventory never rewrites an invoice that has already been
 * quoted.
 *
 * The constructor is package-private: a line can only come into existence
 * through {@code ServiceJob.addLine}, which keeps the job's line list and its
 * totals consistent.
 */
@Entity
@Table(name = "job_line")
public class JobLine {

    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ServiceJob job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(nullable = false)
    private int quantity;

    /** Snapshot of the part's price when this line was added. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceAtTime;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected JobLine() {
        // required by JPA
    }

    JobLine(ServiceJob job, Part part, int quantity, BigDecimal unitPriceAtTime) {
        this.job = job;
        this.part = part;
        this.quantity = quantity;
        this.unitPriceAtTime = unitPriceAtTime;
        this.lineTotal = unitPriceAtTime
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public ServiceJob getJob() {
        return job;
    }

    public Part getPart() {
        return part;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPriceAtTime() {
        return unitPriceAtTime;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
