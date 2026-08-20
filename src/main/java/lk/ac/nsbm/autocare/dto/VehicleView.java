package lk.ac.nsbm.autocare.dto;

import lk.ac.nsbm.autocare.entity.Vehicle;

/** Read-only projection of a customer vehicle. */
public record VehicleView(
        Long id,
        String registrationNumber,
        String make,
        String model,
        int year,
        int mileageKm,
        String description,
        String ownerName) {

    public static VehicleView from(Vehicle vehicle) {
        return new VehicleView(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getMileageKm(),
                vehicle.getDescription(),
                vehicle.getOwner().getFullName());
    }
}
