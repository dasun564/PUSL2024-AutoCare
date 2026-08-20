package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

/**
 * Booking rule: a customer may only have a limited number of jobs open at
 * once, so the garage is not holding bays for work that never arrives.
 */
public class TooManyOpenJobsException extends AutoCareException {

    private final long currentCount;
    private final int limit;

    public TooManyOpenJobsException(long currentCount, int limit) {
        super("Customer already has " + currentCount + " open jobs, limit is " + limit);
        this.currentCount = currentCount;
        this.limit = limit;
    }

    public long getCurrentCount() {
        return currentCount;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public String getTitle() {
        return "You already have jobs open";
    }

    @Override
    public String getUserMessage() {
        return "You have " + currentCount + " service " + (currentCount == 1 ? "job" : "jobs")
                + " still open and the limit is " + limit
                + ". We cannot take another booking until one of them is completed or cancelled.";
    }

    @Override
    public String getSuggestedAction() {
        return "Open \"My Service Jobs\" to see your current bookings, or call the garage on 011 234 5678.";
    }

    @Override
    public String getErrorCode() {
        return "TOO_MANY_OPEN_JOBS";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
