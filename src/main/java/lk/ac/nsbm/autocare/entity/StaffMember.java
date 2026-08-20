package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Garage staff. Maintains the parts inventory and works service jobs.
 */
@Entity
@DiscriminatorValue("STAFF")
public class StaffMember extends AppUser {

    @Column(length = 20)
    private String staffCode;

    /** e.g. "Senior Technician", "Service Manager". */
    @Column(length = 60)
    private String jobTitle;

    protected StaffMember() {
        // required by JPA
    }

    public StaffMember(String username, String password, String fullName, String phone,
                       String staffCode, String jobTitle) {
        super(username, password, fullName, phone);
        this.staffCode = staffCode;
        this.jobTitle = jobTitle;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String getAccountType() {
        return "Garage staff";
    }

    public String getStaffCode() {
        return staffCode;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
}
