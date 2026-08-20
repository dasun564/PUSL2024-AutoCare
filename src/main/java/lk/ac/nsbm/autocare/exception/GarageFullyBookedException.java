package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Booking rule: the garage has a fixed number of service bays, so it accepts
 * only a fixed number of jobs per calendar day.
 */
public class GarageFullyBookedException extends AutoCareException {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy");

    private final LocalDate requestedDate;
    private final int dailyCapacity;

    public GarageFullyBookedException(LocalDate requestedDate, int dailyCapacity) {
        super("No capacity on " + requestedDate + "; daily limit " + dailyCapacity);
        this.requestedDate = requestedDate;
        this.dailyCapacity = dailyCapacity;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public int getDailyCapacity() {
        return dailyCapacity;
    }

    @Override
    public String getTitle() {
        return "That day is fully booked";
    }

    @Override
    public String getUserMessage() {
        return "The garage is already handling its full capacity of " + dailyCapacity
                + " vehicles on " + requestedDate.format(DISPLAY) + ".";
    }

    @Override
    public String getSuggestedAction() {
        return "Choose another date - " + requestedDate.plusDays(1).format(DISPLAY) + " may be available.";
    }

    @Override
    public String getErrorCode() {
        return "GARAGE_FULLY_BOOKED";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
