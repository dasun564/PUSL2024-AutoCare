package lk.ac.nsbm.autocare.dto;

import lk.ac.nsbm.autocare.entity.JobStatus;
import lk.ac.nsbm.autocare.entity.ServiceJob;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only projection of a service job, including its invoice lines.
 *
 * Built inside the service transaction, so every association it needs is
 * already loaded - the templates never touch a JPA proxy.
 */
public record JobView(
        Long id,
        String jobNumber,
        String registrationNumber,
        String vehicleDescription,
        String customerName,
        String customerUsername,
        LocalDate bookedFor,
        String reportedProblem,
        JobStatus status,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        BigDecimal labourHours,
        BigDecimal labourRate,
        BigDecimal partsTotal,
        BigDecimal labourTotal,
        BigDecimal grandTotal,
        List<JobLineView> lines) {

    public static JobView from(ServiceJob job) {
        boolean closed = job.getStatus() == JobStatus.COMPLETED;
        return new JobView(
                job.getId(),
                job.getJobNumber(),
                job.getVehicle().getRegistrationNumber(),
                job.getVehicle().getDescription(),
                job.getCustomer().getFullName(),
                job.getCustomer().getUsername(),
                job.getBookedFor(),
                job.getReportedProblem(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getClosedAt(),
                job.getLabourHours(),
                job.getLabourRate(),
                closed ? job.getPartsTotal() : job.estimatedPartsTotal(),
                closed ? job.getLabourTotal() : job.getLabourRate().multiply(job.getLabourHours()),
                closed ? job.getGrandTotal() : job.estimatedGrandTotal(),
                job.getLines().stream().map(JobLineView::from).toList());
    }

    /** Summary rows do not need the lines; this keeps the list queries light. */
    public static JobView summaryFrom(ServiceJob job) {
        return new JobView(
                job.getId(),
                job.getJobNumber(),
                job.getVehicle().getRegistrationNumber(),
                job.getVehicle().getDescription(),
                job.getCustomer().getFullName(),
                job.getCustomer().getUsername(),
                job.getBookedFor(),
                job.getReportedProblem(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getClosedAt(),
                job.getLabourHours(),
                job.getLabourRate(),
                job.getPartsTotal(),
                job.getLabourTotal(),
                job.getGrandTotal(),
                List.of());
    }

    public boolean isOpen() {
        return status.isOpen();
    }

    public boolean isInvoiced() {
        return status == JobStatus.COMPLETED;
    }
}
