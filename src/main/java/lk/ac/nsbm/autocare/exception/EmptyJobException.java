package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

/** A job cannot be invoiced with neither parts nor labour recorded against it. */
public class EmptyJobException extends AutoCareException {

    private final String jobNumber;

    public EmptyJobException(String jobNumber) {
        super("Job " + jobNumber + " has no parts and no labour");
        this.jobNumber = jobNumber;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    @Override
    public String getTitle() {
        return "Nothing to invoice";
    }

    @Override
    public String getUserMessage() {
        return "Job " + jobNumber + " has no parts and no labour hours recorded, so there is nothing to charge for.";
    }

    @Override
    public String getSuggestedAction() {
        return "Add the parts used and the labour hours worked, then complete the job.";
    }

    @Override
    public String getErrorCode() {
        return "EMPTY_JOB";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
