package lk.ac.nsbm.autocare.service;

import lk.ac.nsbm.autocare.dto.BookingCapacityView;
import lk.ac.nsbm.autocare.dto.BookingForm;
import lk.ac.nsbm.autocare.dto.JobView;
import lk.ac.nsbm.autocare.dto.VehicleView;
import lk.ac.nsbm.autocare.exception.GarageFullyBookedException;
import lk.ac.nsbm.autocare.exception.InvalidBookingDateException;
import lk.ac.nsbm.autocare.exception.JobNotFoundException;
import lk.ac.nsbm.autocare.exception.TooManyOpenJobsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for the three booking rules:
 *   - the booking date must be today or later, not a Sunday, within 90 days;
 *   - a customer may hold at most 2 open jobs;
 *   - the garage accepts at most 4 vehicles per day.
 *
 * Also covers the ownership check that stops a customer booking a service
 * against somebody else's vehicle.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingRuleTest {

    private static final String CLEAN = "10965261";      // 1 open job, allowance left
    private static final String AT_LIMIT = "10965261B";  // 2 open jobs
    private static final String OTHER = "k.perera";      // 1 open job

    @Autowired
    private ServiceJobService jobService;

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A customer under the limit books successfully")
    void bookingSucceeds() {
        JobView job = jobService.bookService(CLEAN, formFor(CLEAN, workingDay(3)));

        assertThat(job.customerUsername()).isEqualTo(CLEAN);
        assertThat(job.customerName()).isEqualTo("Dasun Edirisinghe");
        assertThat(job.jobNumber()).startsWith("AC-");
        assertThat(job.status().isOpen()).isTrue();
        assertThat(job.registrationNumber()).isEqualTo("WP-10965261");
    }

    @Test
    @DisplayName("Capacity view reports the customer's standing before they submit")
    void capacityIsReportedUpFront() {
        BookingCapacityView before = jobService.describeCapacity(CLEAN);
        assertThat(before.openJobs()).isEqualTo(1);
        assertThat(before.openJobLimit()).isEqualTo(2);
        assertThat(before.canBook()).isTrue();
        assertThat(before.remainingAllowance()).isEqualTo(1);

        jobService.bookService(CLEAN, formFor(CLEAN, workingDay(3)));

        BookingCapacityView after = jobService.describeCapacity(CLEAN);
        assertThat(after.openJobs()).isEqualTo(2);
        assertThat(after.canBook()).isFalse();
    }

    // ------------------------------------------------------------------
    // Rule 3 - the date
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A date in the past is refused, saying so")
    void pastDateRefused() {
        assertThatExceptionOfType(InvalidBookingDateException.class)
                .isThrownBy(() -> jobService.bookService(CLEAN, formFor(CLEAN, LocalDate.now().minusDays(1))))
                .satisfies(ex -> {
                    assertThat(ex.getReason()).isEqualTo(InvalidBookingDateException.Reason.IN_THE_PAST);
                    assertThat(ex.getUserMessage()).contains("already passed");
                });
    }

    @Test
    @DisplayName("A Sunday is refused, saying the garage is closed")
    void sundayRefused() {
        LocalDate sunday = LocalDate.now();
        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sunday = sunday.plusDays(1);
        }
        LocalDate target = sunday;

        assertThatExceptionOfType(InvalidBookingDateException.class)
                .isThrownBy(() -> jobService.bookService(CLEAN, formFor(CLEAN, target)))
                .satisfies(ex -> {
                    assertThat(ex.getReason()).isEqualTo(InvalidBookingDateException.Reason.CLOSED_DAY);
                    assertThat(ex.getUserMessage()).contains("closed on Sundays");
                });
    }

    @Test
    @DisplayName("A date beyond the 90 day window is refused")
    void tooFarAheadRefused() {
        assertThatExceptionOfType(InvalidBookingDateException.class)
                .isThrownBy(() -> jobService.bookService(CLEAN, formFor(CLEAN, LocalDate.now().plusDays(120))))
                .satisfies(ex ->
                        assertThat(ex.getReason()).isEqualTo(InvalidBookingDateException.Reason.TOO_FAR_AHEAD));
    }

    // ------------------------------------------------------------------
    // Rule 1 - open jobs per customer
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A third open job is refused")
    void openJobLimitEnforced() {
        assertThatExceptionOfType(TooManyOpenJobsException.class)
                .isThrownBy(() -> jobService.bookService(AT_LIMIT, formFor(AT_LIMIT, workingDay(3))))
                .satisfies(ex -> {
                    assertThat(ex.getLimit()).isEqualTo(2);
                    assertThat(ex.getCurrentCount()).isEqualTo(2);
                });
    }

    @Test
    @DisplayName("Cancelling a job frees the allowance again")
    void cancellingRestoresAllowance() {
        assertThat(jobService.describeCapacity(AT_LIMIT).canBook()).isFalse();

        Long openJobId = jobService.listMyJobs(AT_LIMIT).stream()
                .filter(JobView::isOpen)
                .findFirst().orElseThrow().id();
        jobService.cancelMyJob(AT_LIMIT, openJobId);

        assertThat(jobService.describeCapacity(AT_LIMIT).canBook()).isTrue();
    }

    // ------------------------------------------------------------------
    // Rule 2 - daily capacity
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A day already holding four jobs is refused")
    void dailyCapacityEnforced() {
        // The seeder books four jobs on this day.
        LocalDate fullDay = jobService.listAllJobs().stream()
                .collect(java.util.stream.Collectors.groupingBy(JobView::bookedFor,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() >= 4)
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed data should fill one day to capacity"));

        // OTHER still has allowance, so the capacity rule is what refuses this.
        assertThatExceptionOfType(GarageFullyBookedException.class)
                .isThrownBy(() -> jobService.bookService(OTHER, formFor(OTHER, fullDay)))
                .satisfies(ex -> {
                    assertThat(ex.getDailyCapacity()).isEqualTo(4);
                    assertThat(ex.getRequestedDate()).isEqualTo(fullDay);
                });
    }

    // ------------------------------------------------------------------
    // Ownership
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A customer cannot book a service against another customer's vehicle")
    void cannotBookSomebodyElsesVehicle() {
        Long otherVehicleId = jobService.listMyVehicles(OTHER).get(0).id();

        BookingForm form = new BookingForm();
        form.setVehicleId(otherVehicleId);
        form.setBookedFor(workingDay(3));
        form.setReportedProblem("Attempting to book against a vehicle I do not own.");

        assertThatExceptionOfType(JobNotFoundException.class)
                .isThrownBy(() -> jobService.bookService(CLEAN, form));
    }

    @Test
    @DisplayName("A customer cannot open another customer's job")
    void cannotReadSomebodyElsesJob() {
        Long otherJobId = jobService.listMyJobs(OTHER).get(0).id();

        assertThatExceptionOfType(JobNotFoundException.class)
                .isThrownBy(() -> jobService.getMyJob(CLEAN, otherJobId));
    }

    @Test
    @DisplayName("A customer only ever sees their own jobs")
    void jobListIsScopedToTheCaller() {
        assertThat(jobService.listMyJobs(CLEAN))
                .isNotEmpty()
                .allSatisfy(job -> assertThat(job.customerUsername()).isEqualTo(CLEAN));
    }

    // ------------------------------------------------------------------

    private BookingForm formFor(String username, LocalDate date) {
        List<VehicleView> vehicles = jobService.listMyVehicles(username);
        BookingForm form = new BookingForm();
        form.setVehicleId(vehicles.get(0).id());
        form.setBookedFor(date);
        form.setReportedProblem("Automated test booking for the rule under examination.");
        return form;
    }

    /** A date n days ahead, rolled off Sunday. */
    private LocalDate workingDay(int daysAhead) {
        LocalDate date = LocalDate.now().plusDays(daysAhead);
        return date.getDayOfWeek() == DayOfWeek.SUNDAY ? date.plusDays(1) : date;
    }
}
