package lk.ac.nsbm.autocare.repository;

import jakarta.persistence.LockModeType;
import lk.ac.nsbm.autocare.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the parts inventory. Queries only - no business rules.
 */
@Repository
public interface PartRepository extends JpaRepository<Part, Long> {

    /**
     * Loads a part and holds a write lock on its row until the surrounding
     * transaction commits (SELECT ... FOR UPDATE).
     *
     * Used when completing a job, where several parts are consumed in one
     * transaction. Without the lock, two jobs closing at the same moment could
     * both read the same stock figure and both decrement it, driving the count
     * below zero or losing one of the deductions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Part p where p.id = :id")
    Optional<Part> findByIdForUpdate(@Param("id") Long id);

    Optional<Part> findByIdAndActiveTrue(Long id);

    @Query("""
            select p from Part p
            join fetch p.category
            where p.active = true
            order by p.name asc
            """)
    List<Part> findAllActiveWithCategory();

    @Query("""
            select p from Part p
            join fetch p.category
            order by p.name asc
            """)
    List<Part> findAllWithCategory();

    @Query("""
            select p from Part p
            join fetch p.category c
            where p.active = true
              and (lower(p.name) like lower(concat('%', :term, '%'))
                   or lower(p.partNumber) like lower(concat('%', :term, '%'))
                   or lower(c.name) like lower(concat('%', :term, '%')))
            order by p.name asc
            """)
    List<Part> searchActive(@Param("term") String term);

    @Query("""
            select p from Part p
            join fetch p.category
            where p.active = true and p.stockQuantity <= p.reorderLevel
            order by p.stockQuantity asc
            """)
    List<Part> findLowStock();

    boolean existsByPartNumberIgnoreCase(String partNumber);
}
