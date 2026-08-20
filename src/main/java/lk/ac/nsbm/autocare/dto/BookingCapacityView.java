package lk.ac.nsbm.autocare.dto;

/**
 * A customer's standing against the booking rules, shown as a banner before
 * they submit a booking rather than only in the rejection afterwards.
 */
public record BookingCapacityView(
        long openJobs,
        int openJobLimit,
        int dailyCapacity,
        int registeredVehicles) {

    public boolean canBook() {
        return openJobs < openJobLimit && registeredVehicles > 0;
    }

    public long remainingAllowance() {
        return Math.max(0, openJobLimit - openJobs);
    }

    public boolean hasNoVehicles() {
        return registeredVehicles == 0;
    }
}
