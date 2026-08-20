package lk.ac.nsbm.autocare.exception;

import org.springframework.http.HttpStatus;

/** A vehicle with this registration number is already on the system. */
public class DuplicateRegistrationException extends AutoCareException {

    private final String registrationNumber;

    public DuplicateRegistrationException(String registrationNumber) {
        super("Registration already present: " + registrationNumber);
        this.registrationNumber = registrationNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    @Override
    public String getTitle() {
        return "Vehicle already registered";
    }

    @Override
    public String getUserMessage() {
        return "Registration " + registrationNumber + " is already recorded on AutoCare.";
    }

    @Override
    public String getSuggestedAction() {
        return "Check the registration, or contact the garage if you have bought this vehicle from another customer.";
    }

    @Override
    public String getErrorCode() {
        return "DUPLICATE_REGISTRATION";
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
