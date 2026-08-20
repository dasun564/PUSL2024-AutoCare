package lk.ac.nsbm.autocare.entity;

/**
 * Lifecycle of a {@link ServiceJob}.
 *
 * BOOKED      - the customer has reserved a slot; no work started.
 * IN_PROGRESS - staff are working on the vehicle and planning parts.
 * COMPLETED   - work finished, parts consumed from stock, invoice raised.
 * CANCELLED   - called off before completion; no parts were consumed.
 *
 * Only BOOKED and IN_PROGRESS count as "open" jobs for the rule that limits
 * how many a customer may hold at once.
 */
public enum JobStatus {
    BOOKED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean isOpen() {
        return this == BOOKED || this == IN_PROGRESS;
    }

    public String getLabel() {
        return switch (this) {
            case BOOKED -> "Booked";
            case IN_PROGRESS -> "In progress";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
        };
    }
}
