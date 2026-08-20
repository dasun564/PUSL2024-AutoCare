package lk.ac.nsbm.autocare.service;

import lk.ac.nsbm.autocare.dto.BookingCapacityView;
import lk.ac.nsbm.autocare.dto.BookingForm;
import lk.ac.nsbm.autocare.dto.JobView;
import lk.ac.nsbm.autocare.dto.VehicleForm;
import lk.ac.nsbm.autocare.dto.VehicleView;

import java.math.BigDecimal;
import java.util.List;

/**
 * Customer-facing and staff-facing operations on service jobs.
 *
 * Controllers depend on this interface, never on the implementation, so the
 * web layer is coupled to a contract rather than to a class.
 *
 * Every customer-facing method takes the caller's USERNAME, which the
 * controller reads from Spring Security's Authentication. There is no
 * signature here through which a browser-supplied customer identity could
 * enter the business layer.
 */
public interface ServiceJobService {

    // --- vehicles ---

    List<VehicleView> listMyVehicles(String username);

    VehicleView registerVehicle(String username, VehicleForm form);

    // --- booking ---

    BookingCapacityView describeCapacity(String username);

    /**
     * Books a service for one of the caller's own vehicles.
     *
     * @throws lk.ac.nsbm.autocare.exception.InvalidBookingDateException   past date, Sunday, or beyond the window
     * @throws lk.ac.nsbm.autocare.exception.TooManyOpenJobsException      customer already at the open-job limit
     * @throws lk.ac.nsbm.autocare.exception.GarageFullyBookedException    no capacity left that day
     * @throws lk.ac.nsbm.autocare.exception.JobNotFoundException          vehicle is not the caller's
     */
    JobView bookService(String username, BookingForm form);

    List<JobView> listMyJobs(String username);

    JobView getMyJob(String username, Long jobId);

    JobView cancelMyJob(String username, Long jobId);

    // --- staff ---

    List<JobView> listAllJobs();

    JobView getJob(Long jobId);

    JobView beginWork(Long jobId);

    JobView addPartToJob(Long jobId, Long partId, int quantity);

    JobView removeLineFromJob(Long jobId, Long lineId);

    JobView recordLabour(Long jobId, BigDecimal hours);

    /**
     * Completes the job: consumes every planned part from stock and raises the
     * invoice, atomically.
     *
     * @throws lk.ac.nsbm.autocare.exception.InsufficientPartStockException any line's part is short
     * @throws lk.ac.nsbm.autocare.exception.EmptyJobException              nothing to charge for
     * @throws lk.ac.nsbm.autocare.exception.JobNotOpenException            already completed or cancelled
     */
    JobView completeJob(Long jobId);
}
