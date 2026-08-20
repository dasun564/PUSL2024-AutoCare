package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Abstract base for every account that can sign in to AutoCare.
 *
 * GENERALISATION: {@link Customer} and {@link StaffMember} inherit identity and
 * credentials from here, so authentication works against one type. SINGLE_TABLE
 * is right for this hierarchy because sign-in looks a user up by username before
 * knowing which subtype it is - one indexed lookup, no join.
 */
@Entity
@Table(name = "app_user")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
public abstract class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    /** BCrypt hash - never a plaintext password. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(length = 30)
    private String phone;

    protected AppUser() {
        // required by JPA
    }

    protected AppUser(String username, String password, String fullName, String phone) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
    }

    /**
     * Spring Security authority without the ROLE_ prefix. POLYMORPHIC: each
     * subclass answers for itself, so the security layer never tests types.
     */
    public abstract String getRole();

    /** Human-readable account type for the interface. */
    public abstract String getAccountType();

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
