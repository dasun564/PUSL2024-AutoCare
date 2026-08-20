package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A vehicle owner. Books services and views their own job history.
 *
 * COMPOSITION with {@link Vehicle}: a vehicle record exists only as part of a
 * customer's account, so the association cascades and orphans are removed.
 */
@Entity
@DiscriminatorValue("CUSTOMER")
public class Customer extends AppUser {

    @Column(length = 200)
    private String address;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vehicle> vehicles = new ArrayList<>();

    protected Customer() {
        // required by JPA
    }

    public Customer(String username, String password, String fullName, String phone, String address) {
        super(username, password, fullName, phone);
        this.address = address;
    }

    @Override
    public String getRole() {
        return "CUSTOMER";
    }

    @Override
    public String getAccountType() {
        return "Customer";
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /** Unmodifiable: vehicles are added through the service layer only. */
    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    /** Keeps both ends of the association consistent. */
    public void addVehicle(Vehicle vehicle) {
        this.vehicles.add(vehicle);
        vehicle.assignOwner(this);
    }
}
