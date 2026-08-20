package lk.ac.nsbm.autocare.repository;

import lk.ac.nsbm.autocare.entity.Customer;
import lk.ac.nsbm.autocare.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Data access for customer vehicles. */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    List<Vehicle> findByOwnerOrderByRegistrationNumberAsc(Customer owner);

    @Query("""
            select v from Vehicle v
            join fetch v.owner
            where v.id = :id
            """)
    Optional<Vehicle> findByIdWithOwner(@Param("id") Long id);

    @Query("""
            select v from Vehicle v
            join fetch v.owner
            order by v.registrationNumber asc
            """)
    List<Vehicle> findAllWithOwner();
}
