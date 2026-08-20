package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

/**
 * Inventory integrity: a part number identifies exactly one inventory entry.
 *
 * The check lives in PartAdminService rather than the controller, because the
 * rule must hold for the REST endpoint as well as the web form.
 */
public class DuplicatePartNumberException extends AutoCareException {

    private final String partNumber;

    public DuplicatePartNumberException(String partNumber) {
        super("Part number already in use: " + partNumber);
        this.partNumber = partNumber;
    }

    public String getPartNumber() {
        return partNumber;
    }

    @Override
    public String getTitle() {
        return "Duplicate part number";
    }

    @Override
    public String getUserMessage() {
        return "Part number " + partNumber + " is already used by another entry in the inventory.";
    }

    @Override
    public String getSuggestedAction() {
        return "Check the part number, or edit the existing entry instead of creating a second one.";
    }

    @Override
    public String getErrorCode() {
        return "DUPLICATE_PART_NUMBER";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
