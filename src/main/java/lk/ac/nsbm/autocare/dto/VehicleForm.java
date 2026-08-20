package lk.ac.nsbm.autocare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Input DTO for a customer registering one of their vehicles. */
public class VehicleForm {

    @NotBlank(message = "Registration number is required")
    @Pattern(regexp = "^[A-Za-z0-9 -]{4,20}$",
             message = "Registration must be 4-20 characters: letters, digits, spaces and hyphens")
    private String registrationNumber;

    @NotBlank(message = "Make is required")
    @Size(max = 60, message = "Make must be 60 characters or fewer")
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 60, message = "Model must be 60 characters or fewer")
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1950, message = "Year must be 1950 or later")
    @Max(value = 2030, message = "Year cannot be later than 2030")
    private Integer year;

    @NotNull(message = "Current mileage is required")
    @Min(value = 0, message = "Mileage cannot be negative")
    @Max(value = 2_000_000, message = "Mileage looks too high - please check")
    private Integer mileageKm;

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMileageKm() {
        return mileageKm;
    }

    public void setMileageKm(Integer mileageKm) {
        this.mileageKm = mileageKm;
    }
}
