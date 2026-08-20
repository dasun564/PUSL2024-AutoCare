package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

/**
 * A job could not be completed because one of its planned parts is not in
 * stock in the quantity the job needs.
 *
 * This is the failure that proves the transaction boundary is real: it can be
 * raised on the fifth of five lines, after four parts have already been
 * decremented, and every one of those decrements must be undone.
 */
public class InsufficientPartStockException extends AutoCareException {

    private final String partNumber;
    private final String partName;
    private final int required;
    private final int available;

    public InsufficientPartStockException(String partNumber, String partName, int required, int available) {
        super("Part " + partNumber + " short: need " + required + ", have " + available);
        this.partNumber = partNumber;
        this.partName = partName;
        this.required = required;
        this.available = available;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public String getPartName() {
        return partName;
    }

    public int getRequired() {
        return required;
    }

    public int getAvailable() {
        return available;
    }

    public int getShortfall() {
        return required - available;
    }

    @Override
    public String getTitle() {
        return "Not enough stock to complete this job";
    }

    @Override
    public String getUserMessage() {
        return "This job needs " + required + " x \"" + partName + "\" (" + partNumber
                + ") but only " + available + " " + (available == 1 ? "is" : "are")
                + " in stock - short by " + getShortfall() + ". No parts have been taken from stock and the job is"
                + " still open.";
    }

    @Override
    public String getSuggestedAction() {
        return "Order " + getShortfall() + " more of " + partNumber
                + ", or reduce the quantity on the job line, then complete the job again.";
    }

    @Override
    public String getErrorCode() {
        return "INSUFFICIENT_PART_STOCK";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
