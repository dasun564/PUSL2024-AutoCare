package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

import lk.ac.nsbm.autocare.entity.JobStatus;

/**
 * The requested action needs an open job, but this one has already been
 * completed or cancelled.
 *
 * Guards against a double-submitted form invoicing the same job twice, and
 * against a customer cancelling work that has already been finished and
 * charged for.
 */
public class JobNotOpenException extends AutoCareException {

    private final String jobNumber;
    private final JobStatus status;
    private final String attemptedAction;

    public JobNotOpenException(String jobNumber, JobStatus status, String attemptedAction) {
        super("Cannot " + attemptedAction + " job " + jobNumber + " in status " + status);
        this.jobNumber = jobNumber;
        this.status = status;
        this.attemptedAction = attemptedAction;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public JobStatus getStatus() {
        return status;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }

    @Override
    public String getTitle() {
        return "This job is already closed";
    }

    @Override
    public String getUserMessage() {
        return "Job " + jobNumber + " is marked \"" + status.getLabel()
                + "\", so it can no longer be " + attemptedAction + ".";
    }

    @Override
    public String getSuggestedAction() {
        return status == JobStatus.COMPLETED
                ? "Open the job to view its invoice."
                : "Book a new service if the vehicle still needs work.";
    }

    @Override
    public String getErrorCode() {
        return "JOB_NOT_OPEN";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
