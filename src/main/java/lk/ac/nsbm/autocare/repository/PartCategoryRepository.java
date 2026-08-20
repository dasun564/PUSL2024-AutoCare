package lk.ac.nsbm.autocare.repository;

import lk.ac.nsbm.autocare.entity.PartCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Data access for the parts categories. */
@Repository
public interface PartCategoryRepository extends JpaRepository<PartCategory, Long> {

    Optional<PartCategory> findByNameIgnoreCase(String name);

    List<PartCategory> findAllByOrderByNameAsc();
}
