package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Booking rule: a service can only be booked for a working day inside the
 * booking window - not in the past, not on a Sunday, and not more than 90 days
 * ahead.
 */
public class InvalidBookingDateException extends AutoCareException {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy");

    /** Why the date was refused, so the message can be specific. */
    public enum Reason {
        IN_THE_PAST("that date has already passed"),
        CLOSED_DAY("the garage is closed on Sundays"),
        TOO_FAR_AHEAD("bookings open only 90 days ahead");

        private final String phrase;

        Reason(String phrase) {
            this.phrase = phrase;
        }

        public String getPhrase() {
            return phrase;
        }
    }

    private final LocalDate requestedDate;
    private final Reason reason;

    public InvalidBookingDateException(LocalDate requestedDate, Reason reason) {
        super("Booking date " + requestedDate + " refused: " + reason);
        this.requestedDate = requestedDate;
        this.reason = reason;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public Reason getReason() {
        return reason;
    }

    @Override
    public String getTitle() {
        return "That booking date cannot be used";
    }

    @Override
    public String getUserMessage() {
        return "We cannot book your vehicle in for " + requestedDate.format(DISPLAY)
                + " because " + reason.getPhrase() + ".";
    }

    @Override
    public String getSuggestedAction() {
        return switch (reason) {
            case IN_THE_PAST -> "Choose today or a later date.";
            case CLOSED_DAY -> "Choose any day from Monday to Saturday.";
            case TOO_FAR_AHEAD -> "Choose a date within the next 90 days.";
        };
    }

    @Override
    public String getErrorCode() {
        return "INVALID_BOOKING_DATE";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
