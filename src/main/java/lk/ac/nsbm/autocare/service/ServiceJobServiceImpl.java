package lk.ac.nsbm.autocare.service;

import lk.ac.nsbm.autocare.dto.BookingCapacityView;
import lk.ac.nsbm.autocare.dto.BookingForm;
import lk.ac.nsbm.autocare.dto.JobView;
import lk.ac.nsbm.autocare.dto.VehicleForm;
import lk.ac.nsbm.autocare.dto.VehicleView;
import lk.ac.nsbm.autocare.entity.Customer;
import lk.ac.nsbm.autocare.entity.JobLine;
import lk.ac.nsbm.autocare.entity.Part;
import lk.ac.nsbm.autocare.entity.ServiceJob;
import lk.ac.nsbm.autocare.entity.Vehicle;
import lk.ac.nsbm.autocare.exception.DuplicateRegistrationException;
import lk.ac.nsbm.autocare.exception.EmptyJobException;
import lk.ac.nsbm.autocare.exception.GarageFullyBookedException;
import lk.ac.nsbm.autocare.exception.InsufficientPartStockException;
import lk.ac.nsbm.autocare.exception.InvalidBookingDateException;
import lk.ac.nsbm.autocare.exception.JobNotFoundException;
import lk.ac.nsbm.autocare.exception.JobNotOpenException;
import lk.ac.nsbm.autocare.exception.PartNotFoundException;
import lk.ac.nsbm.autocare.exception.TooManyOpenJobsException;
import lk.ac.nsbm.autocare.repository.CustomerRepository;
import lk.ac.nsbm.autocare.repository.PartRepository;
import lk.ac.nsbm.autocare.repository.ServiceJobRepository;
import lk.ac.nsbm.autocare.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The only place AutoCare's business rules are decided.
 *
 * Layer: @Service. It owns the transaction boundary and every rule that spans
 * more than one entity. Controllers above do HTTP translation only;
 * repositories below do data access only.
 *
 * BUSINESS RULES ENFORCED HERE
 *   1. A customer may hold at most 2 open jobs at once.
 *   2. The garage accepts at most 4 vehicles per calendar day.
 *   3. Bookings must be today or later, not a Sunday, within 90 days.
 *   4. A job can only be completed if every planned part is in stock, and the
 *      parts are consumed all-or-nothing.
 *   5. A customer may only see and cancel their own jobs.
 */
@Service
public class ServiceJobServiceImpl implements ServiceJobService {

    private static final Logger log = LoggerFactory.getLogger(ServiceJobServiceImpl.class);

    /** Rule 1: concurrent open jobs allowed per customer. */
    private static final int OPEN_JOB_LIMIT = 2;

    /** Rule 2: service bays, hence vehicles accepted per day. */
    private static final int DAILY_CAPACITY = 4;

    /** Rule 3: how far ahead the booking diary is open. */
    private static final int BOOKING_WINDOW_DAYS = 90;

    /** Standard labour rate, snapshotted onto each job when it is opened. */
    private static final BigDecimal LABOUR_RATE = new BigDecimal("2500.00");

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceJobRepository jobRepository;
    private final PartRepository partRepository;

    /**
     * Constructor injection: the dependencies are final, the object cannot be
     * built half-configured, and the class is unit-testable without a Spring
     * context.
     */
    public ServiceJobServiceImpl(CustomerRepository customerRepository,
                                 VehicleRepository vehicleRepository,
                                 ServiceJobRepository jobRepository,
                                 PartRepository partRepository) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.jobRepository = jobRepository;
        this.partRepository = partRepository;
    }

    // ==================================================================
    // Vehicles
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public List<VehicleView> listMyVehicles(String username) {
        Customer customer = requireCustomer(username);
        return vehicleRepository.findByOwnerOrderByRegistrationNumberAsc(customer).stream()
                .map(VehicleView::from)
                .toList();
    }

    @Override
    @Transactional
    public VehicleView registerVehicle(String username, VehicleForm form) {
        Customer customer = requireCustomer(username);
        String registration = form.getRegistrationNumber().trim().toUpperCase();

        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(registration)) {
            throw new DuplicateRegistrationException(registration);
        }

        Vehicle vehicle = new Vehicle(registration, form.getMake().trim(), form.getModel().trim(),
                form.getYear(), form.getMileageKm());
        customer.addVehicle(vehicle);
        vehicleRepository.save(vehicle);

        log.info("Customer {} registered vehicle {}", username, registration);
        return VehicleView.from(vehicle);
    }

    // ==================================================================
    // Booking
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public BookingCapacityView describeCapacity(String username) {
        Customer customer = requireCustomer(username);
        return new BookingCapacityView(
                jobRepository.countOpenJobsFor(customer),
                OPEN_JOB_LIMIT,
                DAILY_CAPACITY,
                vehicleRepository.findByOwnerOrderByRegistrationNumberAsc(customer).size());
    }

    /**
     * Books a service.
     *
     * ATOMICITY: @Transactional wraps the whole method, so a rule rejected
     * anywhere below leaves no job row behind.
     *
     * GUARD ORDER: facts about the REQUEST are checked before facts about the
     * CUSTOMER, and facts about the CUSTOMER before facts about the GARAGE.
     * The customer is told the reason that is actually theirs to fix first.
     */
    @Override
    @Transactional
    public JobView bookService(String username, BookingForm form) {
        Customer customer = requireCustomer(username);

        // Ownership: the vehicle id arrives in the request, so it is verified
        // against the session account rather than trusted.
        Vehicle vehicle = vehicleRepository.findByIdWithOwner(form.getVehicleId())
                .filter(v -> v.getOwner().getId().equals(customer.getId()))
                .orElseThrow(() -> new JobNotFoundException(form.getVehicleId()));

        validateBookingDate(form.getBookedFor());

        long openJobs = jobRepository.countOpenJobsFor(customer);
        if (openJobs >= OPEN_JOB_LIMIT) {
            throw new TooManyOpenJobsException(openJobs, OPEN_JOB_LIMIT);
        }

        long bookedThatDay = jobRepository.countBookingsOn(form.getBookedFor());
        if (bookedThatDay >= DAILY_CAPACITY) {
            throw new GarageFullyBookedException(form.getBookedFor(), DAILY_CAPACITY);
        }

        ServiceJob job = new ServiceJob(nextJobNumber(), vehicle, form.getBookedFor(),
                form.getReportedProblem().trim(), LABOUR_RATE, LocalDateTime.now());
        jobRepository.save(job);

        log.info("Customer {} booked job {} for vehicle {} on {}",
                username, job.getJobNumber(), vehicle.getRegistrationNumber(), form.getBookedFor());

        return JobView.from(job);
    }

    /** Rule 3, split out so each refusal reports its own specific reason. */
    private void validateBookingDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new InvalidBookingDateException(date, InvalidBookingDateException.Reason.IN_THE_PAST);
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new InvalidBookingDateException(date, InvalidBookingDateException.Reason.CLOSED_DAY);
        }
        if (date.isAfter(today.plusDays(BOOKING_WINDOW_DAYS))) {
            throw new InvalidBookingDateException(date, InvalidBookingDateException.Reason.TOO_FAR_AHEAD);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobView> listMyJobs(String username) {
        Customer customer = requireCustomer(username);
        return jobRepository.findAllForCustomer(customer).stream()
                .map(JobView::summaryFrom)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobView getMyJob(String username, Long jobId) {
        Customer customer = requireCustomer(username);
        return JobView.from(requireOwnJob(customer, jobId));
    }

    @Override
    @Transactional
    public JobView cancelMyJob(String username, Long jobId) {
        Customer customer = requireCustomer(username);
        ServiceJob job = requireOwnJob(customer, jobId);

        if (!job.getStatus().isOpen()) {
            throw new JobNotOpenException(job.getJobNumber(), job.getStatus(), "cancelled");
        }

        job.cancel(LocalDateTime.now());
        log.info("Customer {} cancelled job {}", username, job.getJobNumber());
        return JobView.from(job);
    }

    // ==================================================================
    // Staff operations
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public List<JobView> listAllJobs() {
        return jobRepository.findAllWithVehicleAndOwner().stream()
                .map(JobView::summaryFrom)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobView getJob(Long jobId) {
        return JobView.from(requireJob(jobId));
    }

    @Override
    @Transactional
    public JobView beginWork(Long jobId) {
        ServiceJob job = requireJob(jobId);
        if (job.getStatus() != lk.ac.nsbm.autocare.entity.JobStatus.BOOKED) {
            throw new JobNotOpenException(job.getJobNumber(), job.getStatus(), "started");
        }
        job.beginWork();
        return JobView.from(job);
    }

    @Override
    @Transactional
    public JobView addPartToJob(Long jobId, Long partId, int quantity) {
        ServiceJob job = requireOpenJob(jobId, "added to");

        Part part = partRepository.findByIdAndActiveTrue(partId)
                .orElseThrow(() -> new PartNotFoundException(partId));

        job.addLine(part, quantity);
        log.info("Added {} x {} to job {}", quantity, part.getPartNumber(), job.getJobNumber());

        // Reload fully so the returned view carries every line and its part.
        return JobView.from(requireJob(jobId));
    }

    @Override
    @Transactional
    public JobView removeLineFromJob(Long jobId, Long lineId) {
        ServiceJob job = requireOpenJob(jobId, "changed");

        JobLine line = job.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new JobNotFoundException(lineId));

        job.removeLine(line);
        return JobView.from(requireJob(jobId));
    }

    @Override
    @Transactional
    public JobView recordLabour(Long jobId, BigDecimal hours) {
        ServiceJob job = requireOpenJob(jobId, "changed");
        job.setLabour(hours);
        return JobView.from(requireJob(jobId));
    }

    /**
     * Completes a job: consumes every planned part from stock, then raises the
     * invoice.
     *
     * THIS IS THE ATOMIC OPERATION THE SYSTEM IS BUILT AROUND.
     *
     * A single job can consume five or six different parts. The loop below
     * decrements them one at a time, and the shortfall might only be
     * discovered on the last line - by which point four parts have already
     * been taken from stock. Because the method is @Transactional and
     * InsufficientPartStockException is unchecked, Spring's proxy rolls the
     * whole transaction back: every earlier decrement is undone, the job stays
     * open, and the garage's stock figures still match the shelves.
     *
     * Without the transaction, each repository write would commit on its own
     * and a failure part-way through would silently destroy inventory that no
     * job ever used.
     *
     * CONCURRENCY: each part row is locked with PESSIMISTIC_WRITE as it is
     * read, so two jobs completing at the same instant cannot both see the
     * same stock figure and both decrement it. The second transaction waits,
     * then re-reads the reduced figure and is correctly rejected.
     */
    @Override
    @Transactional
    public JobView completeJob(Long jobId) {
        ServiceJob job = requireJob(jobId);

        if (!job.getStatus().isOpen()) {
            throw new JobNotOpenException(job.getJobNumber(), job.getStatus(), "completed");
        }
        if (job.getLines().isEmpty() && job.getLabourHours().signum() == 0) {
            throw new EmptyJobException(job.getJobNumber());
        }

        for (JobLine line : job.getLines()) {
            // Locked read: blocks any other transaction touching this part
            // until this one commits or rolls back.
            Part part = partRepository.findByIdForUpdate(line.getPart().getId())
                    .orElseThrow(() -> new PartNotFoundException(line.getPart().getId()));

            if (!part.hasStock(line.getQuantity())) {
                // Rolls back every decrement already applied in this loop.
                throw new InsufficientPartStockException(
                        part.getPartNumber(), part.getName(), line.getQuantity(), part.getStockQuantity());
            }
            part.consumeStock(line.getQuantity());
        }

        job.complete(LocalDateTime.now());

        log.info("Completed job {} for {}: parts {}, labour {}, total {}",
                job.getJobNumber(), job.getVehicle().getRegistrationNumber(),
                job.getPartsTotal(), job.getLabourTotal(), job.getGrandTotal());

        return JobView.from(requireJob(jobId));
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private Customer requireCustomer(String username) {
        return customerRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal '" + username + "' is not a customer account"));
    }

    private ServiceJob requireJob(Long jobId) {
        return jobRepository.findByIdFully(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    private ServiceJob requireOpenJob(Long jobId, String action) {
        ServiceJob job = requireJob(jobId);
        if (!job.getStatus().isOpen()) {
            throw new JobNotOpenException(job.getJobNumber(), job.getStatus(), action);
        }
        return job;
    }

    /** A customer may only ever reach their own jobs. */
    private ServiceJob requireOwnJob(Customer customer, Long jobId) {
        ServiceJob job = requireJob(jobId);
        if (!job.getCustomer().getId().equals(customer.getId())) {
            throw new JobNotFoundException(jobId);
        }
        return job;
    }

    /** Human-friendly sequential job number, e.g. AC-2026-0007. */
    private String nextJobNumber() {
        long sequence = jobRepository.count() + 1;
        String candidate;
        do {
            candidate = String.format("AC-%d-%04d", LocalDate.now().getYear(), sequence);
            sequence++;
        } while (jobRepository.existsByJobNumber(candidate));
        return candidate;
    }
}
