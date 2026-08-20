package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

/**
 * Root of every business failure AutoCare can report.
 *
 * It extends RuntimeException deliberately. Spring's declarative transaction
 * management rolls back automatically on unchecked exceptions only; a checked
 * exception would let a half-finished transaction commit unless every
 * @Transactional were given an explicit rollbackFor. Making the whole
 * hierarchy unchecked means rollback is the default and cannot be forgotten -
 * which matters most in completeJob, where several parts are consumed in one
 * transaction and a failure on the last one must undo all the earlier ones.
 *
 * Each subclass carries the data needed to explain itself, so the presentation
 * layer never has to reconstruct why the operation failed. That is what makes
 * several genuinely specific messages possible instead of one generic error.
 */
public abstract class AutoCareException extends RuntimeException {

    protected AutoCareException(String technicalMessage) {
        super(technicalMessage);
    }

    /** Short heading for the error page. */
    public abstract String getTitle();

    /** Full sentence written for the user, naming the specific problem. */
    public abstract String getUserMessage();

    /** What the user can actually do about it. */
    public abstract String getSuggestedAction();

    /** Stable machine-readable code for the REST API and for log searching. */
    public abstract String getErrorCode();

    public abstract HttpStatus getHttpStatus();
}
