package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

/** The requested part does not exist, or has been withdrawn from the inventory. */
public class PartNotFoundException extends AutoCareException {

    private final Long requestedId;

    public PartNotFoundException(Long requestedId) {
        super("No active part with id " + requestedId);
        this.requestedId = requestedId;
    }

    public Long getRequestedId() {
        return requestedId;
    }

    @Override
    public String getTitle() {
        return "Part not found";
    }

    @Override
    public String getUserMessage() {
        return "We could not find a part with reference number " + requestedId
                + ". It may have been withdrawn from the inventory.";
    }

    @Override
    public String getSuggestedAction() {
        return "Search the parts inventory again to find the current entry.";
    }

    @Override
    public String getErrorCode() {
        return "PART_NOT_FOUND";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
