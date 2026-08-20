package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

/**
 * The service job does not exist, or does not belong to the signed-in
 * customer.
 *
 * Both cases produce the same message on purpose. Answering "that job exists
 * but is not yours" would confirm the existence of another customer's record.
 */
public class JobNotFoundException extends AutoCareException {

    private final Long jobId;

    public JobNotFoundException(Long jobId) {
        super("No service job with id " + jobId + " visible to this user");
        this.jobId = jobId;
    }

    public Long getJobId() {
        return jobId;
    }

    @Override
    public String getTitle() {
        return "Service job not found";
    }

    @Override
    public String getUserMessage() {
        return "We could not find service job reference " + jobId + " on your account.";
    }

    @Override
    public String getSuggestedAction() {
        return "Open \"My Service Jobs\" to see the bookings on your account.";
    }

    @Override
    public String getErrorCode() {
        return "JOB_NOT_FOUND";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
