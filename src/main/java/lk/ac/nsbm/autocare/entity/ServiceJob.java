package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One visit of one vehicle to the garage, and the invoice that results.
 *
 * AGGREGATE ROOT. A {@link JobLine} has no meaning outside its job, so the
 * association is a composition: lines cascade and are orphan-removed, and the
 * only way to add one is {@link #addLine(Part, int)}, which prices it from the
 * part at the moment it is added.
 *
 * ENCAPSULATION. status, closedAt and the three money fields have getters but
 * no setters. The job moves between states only through the intent-revealing
 * methods below, and each one refuses to run from a state where it makes no
 * sense - so a double-submitted form cannot invoice a job twice, and no caller
 * can hand the job a total it did not calculate itself.
 */
@Entity
@Table(name = "service_job")
public class ServiceJob {

    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String jobNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /** The day the customer booked the vehicle in. */
    @Column(nullable = false)
    private LocalDate bookedFor;

    /** What the customer reported. */
    @Column(nullable = false, length = 500)
    private String reportedProblem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.BOOKED;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal labourHours = BigDecimal.ZERO;

    /** Hourly rate agreed when the job was opened, snapshotted for the invoice. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal labourRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal partsTotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal labourTotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobLine> lines = new ArrayList<>();

    protected ServiceJob() {
        // required by JPA
    }

    public ServiceJob(String jobNumber, Vehicle vehicle, LocalDate bookedFor,
                      String reportedProblem, BigDecimal labourRate, LocalDateTime createdAt) {
        this.jobNumber = jobNumber;
        this.vehicle = vehicle;
        this.bookedFor = bookedFor;
        this.reportedProblem = reportedProblem;
        this.labourRate = labourRate;
        this.createdAt = createdAt;
        this.status = JobStatus.BOOKED;
    }

    // ------------------------------------------------------------------
    // State transitions - the only ways this job's state can change
    // ------------------------------------------------------------------

    public void beginWork() {
        if (status != JobStatus.BOOKED) {
            throw new IllegalStateException("Job " + jobNumber + " is " + status + ", not BOOKED");
        }
        this.status = JobStatus.IN_PROGRESS;
    }

    /**
     * Adds a part to the job plan and prices it at the part's CURRENT price.
     * The snapshot matters: a later price change must not silently alter an
     * invoice that has already been quoted to a customer.
     *
     * Note this does not touch stock. Parts are consumed atomically when the
     * job is completed - see ServiceJobServiceImpl.completeJob.
     */
    public JobLine addLine(Part part, int quantity) {
        requireOpen("add parts to");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        JobLine line = new JobLine(this, part, quantity, part.getUnitPrice());
        lines.add(line);
        return line;
    }

    public void removeLine(JobLine line) {
        requireOpen("remove parts from");
        lines.remove(line);
    }

    public void setLabour(BigDecimal hours) {
        requireOpen("record labour on");
        if (hours == null || hours.signum() < 0) {
            throw new IllegalArgumentException("Labour hours cannot be negative");
        }
        this.labourHours = hours.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Closes the job and raises the invoice.
     *
     * The totals are computed here, from this job's own lines and labour - a
     * caller cannot supply them. That is what guarantees the stored invoice
     * always agrees with the line items printed beneath it.
     */
    public void complete(LocalDateTime at) {
        requireOpen("complete");
        if (lines.isEmpty() && labourHours.signum() == 0) {
            throw new IllegalStateException("Job " + jobNumber + " has no parts and no labour to invoice");
        }

        this.partsTotal = lines.stream()
                .map(JobLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        this.labourTotal = labourRate.multiply(labourHours)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        this.grandTotal = partsTotal.add(labourTotal);
        this.status = JobStatus.COMPLETED;
        this.closedAt = at;
    }

    public void cancel(LocalDateTime at) {
        requireOpen("cancel");
        this.status = JobStatus.CANCELLED;
        this.closedAt = at;
    }

    private void requireOpen(String action) {
        if (!status.isOpen()) {
            throw new IllegalStateException(
                    "Cannot " + action + " job " + jobNumber + ": it is already " + status.getLabel().toLowerCase());
        }
    }

    // ------------------------------------------------------------------
    // Derived views
    // ------------------------------------------------------------------

    /** Running parts cost while the job is still open, before invoicing. */
    public BigDecimal estimatedPartsTotal() {
        return lines.stream()
                .map(JobLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal estimatedGrandTotal() {
        return estimatedPartsTotal()
                .add(labourRate.multiply(labourHours))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public Customer getCustomer() {
        return vehicle.getOwner();
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public LocalDate getBookedFor() {
        return bookedFor;
    }

    public String getReportedProblem() {
        return reportedProblem;
    }

    public JobStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public BigDecimal getLabourHours() {
        return labourHours;
    }

    public BigDecimal getLabourRate() {
        return labourRate;
    }

    public BigDecimal getPartsTotal() {
        return partsTotal;
    }

    public BigDecimal getLabourTotal() {
        return labourTotal;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    /** Unmodifiable: lines are added and removed only via addLine / removeLine. */
    public List<JobLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
