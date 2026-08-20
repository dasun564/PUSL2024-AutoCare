package lk.ac.nsbm.autocare.repository;

import lk.ac.nsbm.autocare.entity.Customer;
import lk.ac.nsbm.autocare.entity.JobStatus;
import lk.ac.nsbm.autocare.entity.ServiceJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access for service jobs.
 *
 * The two counting queries here feed the booking rules enforced in
 * ServiceJobServiceImpl - how many jobs a customer already has open, and how
 * many the garage has taken for a given day. This layer supplies the numbers;
 * it does not decide what they mean.
 */
@Repository
public interface ServiceJobRepository extends JpaRepository<ServiceJob, Long> {

    boolean existsByJobNumber(String jobNumber);

    /** Open jobs held by one customer. Feeds the concurrent-jobs limit. */
    @Query("""
            select count(j) from ServiceJob j
            where j.vehicle.owner = :customer
              and j.status in (lk.ac.nsbm.autocare.entity.JobStatus.BOOKED,
                              lk.ac.nsbm.autocare.entity.JobStatus.IN_PROGRESS)
            """)
    long countOpenJobsFor(@Param("customer") Customer customer);

    /** Jobs already accepted for a given day. Feeds the daily capacity limit. */
    @Query("""
            select count(j) from ServiceJob j
            where j.bookedFor = :day
              and j.status <> lk.ac.nsbm.autocare.entity.JobStatus.CANCELLED
            """)
    long countBookingsOn(@Param("day") LocalDate day);

    @Query("""
            select distinct j from ServiceJob j
            join fetch j.vehicle v
            join fetch v.owner
            where v.owner = :customer
            order by j.bookedFor desc
            """)
    List<ServiceJob> findAllForCustomer(@Param("customer") Customer customer);

    @Query("""
            select distinct j from ServiceJob j
            join fetch j.vehicle v
            join fetch v.owner
            order by j.bookedFor desc
            """)
    List<ServiceJob> findAllWithVehicleAndOwner();

    @Query("""
            select distinct j from ServiceJob j
            join fetch j.vehicle v
            join fetch v.owner
            where j.status = :status
            order by j.bookedFor asc
            """)
    List<ServiceJob> findByStatusWithVehicle(@Param("status") JobStatus status);

    /**
     * Loads a job with everything the detail and invoice screens need:
     * the vehicle, its owner, every line, and each line's part.
     */
    @Query("""
            select distinct j from ServiceJob j
            join fetch j.vehicle v
            join fetch v.owner
            left join fetch j.lines l
            left join fetch l.part p
            left join fetch p.category
            where j.id = :id
            """)
    Optional<ServiceJob> findByIdFully(@Param("id") Long id);
}
