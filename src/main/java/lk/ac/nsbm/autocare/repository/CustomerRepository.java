package lk.ac.nsbm.autocare.repository;

import lk.ac.nsbm.autocare.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for customer accounts.
 *
 * The booking workflow always resolves the customer from the username held in
 * the Spring Security session, never from a request parameter.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUsername(String username);

    /**
     * Loads a customer with their vehicles in one query. Needed because
     * open-in-view is disabled, so the template cannot trigger a lazy load.
     */
    @Query("""
            select distinct c from Customer c
            left join fetch c.vehicles
            where c.username = :username
            """)
    Optional<Customer> findByUsernameWithVehicles(@Param("username") String username);

    @Query("select c from Customer c order by c.fullName asc")
    List<Customer> findAllOrderByName();
}
