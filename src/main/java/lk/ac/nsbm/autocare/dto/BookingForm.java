package lk.ac.nsbm.autocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Input DTO for a customer booking a service.
 *
 * Note what it does NOT carry: any identifier for the customer. The account
 * the booking is charged to comes from the authenticated session, so there is
 * no field here through which a browser could nominate a different customer.
 * The vehicleId it does carry is checked for ownership in the service layer.
 */
public class BookingForm {

    @NotNull(message = "Choose which vehicle to book in")
    private Long vehicleId;

    @NotNull(message = "Choose a date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate bookedFor;

    @NotBlank(message = "Describe the problem so the technicians know what to look at")
    @Size(min = 10, max = 500, message = "Please give between 10 and 500 characters")
    private String reportedProblem;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDate getBookedFor() {
        return bookedFor;
    }

    public void setBookedFor(LocalDate bookedFor) {
        this.bookedFor = bookedFor;
    }

    public String getReportedProblem() {
        return reportedProblem;
    }

    public void setReportedProblem(String reportedProblem) {
        this.reportedProblem = reportedProblem;
    }
}
