package lk.ac.nsbm.autocare.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grouping for the parts inventory - Engine, Brakes, Filters, Electrical and
 * so on.
 *
 * This is a first-class entity rather than a string column on {@link Part},
 * which gives the ONE-TO-MANY relationship the coursework asks for and lets a
 * category carry its own description and be renamed in one place.
 */
@Entity
@Table(name = "part_category")
public class PartCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(length = 200)
    private String description;

    @OneToMany(mappedBy = "category", cascade = CascadeType.PERSIST)
    private List<Part> parts = new ArrayList<>();

    protected PartCategory() {
        // required by JPA
    }

    public PartCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Part> getParts() {
        return Collections.unmodifiableList(parts);
    }
}
