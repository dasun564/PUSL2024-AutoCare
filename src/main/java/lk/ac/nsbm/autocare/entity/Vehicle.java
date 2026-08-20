package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A customer's vehicle. MANY-TO-ONE to {@link Customer}; the inverse
 * One-to-Many is {@code Customer.vehicles}.
 */
@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(nullable = false, length = 60)
    private String make;

    @Column(nullable = false, length = 60)
    private String model;

    /** "year" is a reserved word in several SQL dialects, so the column is renamed. */
    @Column(name = "model_year", nullable = false)
    private int year;

    /** Odometer reading in kilometres at the last recorded service. */
    @Column(nullable = false)
    private int mileageKm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Customer owner;

    protected Vehicle() {
        // required by JPA
    }

    public Vehicle(String registrationNumber, String make, String model, int year, int mileageKm) {
        this.registrationNumber = registrationNumber;
        this.make = make;
        this.model = model;
        this.year = year;
        this.mileageKm = mileageKm;
    }

    /** Package-private-ish hook used by Customer.addVehicle to keep both sides in step. */
    void assignOwner(Customer owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return year + " " + make + " " + model;
    }

    /**
     * Mileage only ever moves forward - a garage cannot record a reading lower
     * than the one already on file, so the entity refuses it rather than
     * trusting the caller.
     */
    public void recordMileage(int newReading) {
        if (newReading < this.mileageKm) {
            throw new IllegalArgumentException(
                    "Odometer cannot go backwards: recorded " + this.mileageKm + " km, given " + newReading + " km");
        }
        this.mileageKm = newReading;
    }

    public Long getId() {
        return id;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
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

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMileageKm() {
        return mileageKm;
    }

    public Customer getOwner() {
        return owner;
    }
}
