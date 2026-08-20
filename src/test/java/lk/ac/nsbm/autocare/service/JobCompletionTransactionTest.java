package lk.ac.nsbm.autocare.service;

import lk.ac.nsbm.autocare.dto.JobView;
import lk.ac.nsbm.autocare.entity.JobStatus;
import lk.ac.nsbm.autocare.entity.Part;
import lk.ac.nsbm.autocare.entity.ServiceJob;
import lk.ac.nsbm.autocare.exception.EmptyJobException;
import lk.ac.nsbm.autocare.exception.InsufficientPartStockException;
import lk.ac.nsbm.autocare.exception.JobNotOpenException;
import lk.ac.nsbm.autocare.repository.PartRepository;
import lk.ac.nsbm.autocare.repository.ServiceJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for the atomic operation the system is built around: completing a
 * service job consumes every planned part from stock and raises the invoice,
 * all-or-nothing.
 *
 * The seeded job AC-nnnn-0001 plans one oil filter (in stock) and three
 * alternators (only one in stock). Completing it must therefore fail - AND
 * must leave the oil filter untouched, even though its decrement happens
 * first in the loop. That is the property @Transactional exists to provide.
 *
 * @WithMockUser is needed because PartAdminService and the job service run
 * behind method security.
 */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "admin10965261", roles = "ADMIN")
// Each test needs the seeded state described above, and two of them complete
// the job for good. The context (and with it the create-drop test database and
// the seeder) is therefore rebuilt between tests.
//
// The test class is deliberately NOT @Transactional. Wrapping these tests in an
// outer transaction would make them prove nothing: the service's own
// transaction would simply join the test's, and "did the rollback happen"
// could not be distinguished from "nothing was ever committed".
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JobCompletionTransactionTest {

    @Autowired
    private ServiceJobService jobService;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private ServiceJobRepository jobRepository;

    @Test
    @DisplayName("Completing a job whose parts exceed stock is rejected, naming the short part")
    void shortStockIsRejectedWithASpecificMessage() {
        ServiceJob job = shortStockJob();

        assertThatExceptionOfType(InsufficientPartStockException.class)
                .isThrownBy(() -> jobService.completeJob(job.getId()))
                .satisfies(ex -> {
                    assertThat(ex.getPartNumber()).isEqualTo("EL-ALT-90A");
                    assertThat(ex.getRequired()).isEqualTo(3);
                    assertThat(ex.getAvailable()).isEqualTo(1);
                    assertThat(ex.getShortfall()).isEqualTo(2);
                    assertThat(ex.getErrorCode()).isEqualTo("INSUFFICIENT_PART_STOCK");
                    assertThat(ex.getUserMessage()).contains("Alternator 90A");
                });
    }

    /**
     * THE CENTRAL TEST. The oil filter is decremented before the alternator is
     * even examined. If the transaction did not roll back, that decrement
     * would be committed and one filter would vanish from the inventory
     * without ever being fitted to a car.
     */
    @Test
    @DisplayName("A failed completion rolls back every decrement already applied in the loop")
    void failedCompletionConsumesNothingAtAll() {
        ServiceJob job = shortStockJob();

        int oilFilterBefore = stockOf("FT-OIL-STD");
        int alternatorBefore = stockOf("EL-ALT-90A");

        assertThatExceptionOfType(InsufficientPartStockException.class)
                .isThrownBy(() -> jobService.completeJob(job.getId()));

        assertThat(stockOf("FT-OIL-STD"))
                .as("the oil filter was decremented earlier in the same loop and must be restored")
                .isEqualTo(oilFilterBefore);
        assertThat(stockOf("EL-ALT-90A"))
                .as("the alternator was never consumed")
                .isEqualTo(alternatorBefore);
    }

    @Test
    @DisplayName("A failed completion leaves the job open and editable")
    void failedCompletionLeavesTheJobOpen() {
        ServiceJob job = shortStockJob();

        assertThatExceptionOfType(InsufficientPartStockException.class)
                .isThrownBy(() -> jobService.completeJob(job.getId()));

        ServiceJob reloaded = jobRepository.findByIdFully(job.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(reloaded.getGrandTotal()).isNull();
    }

    @Test
    @DisplayName("Removing the short line lets the job complete, consuming stock and pricing the invoice")
    void successfulCompletionConsumesStockAndInvoices() {
        ServiceJob job = shortStockJob();
        Long jobId = job.getId();

        Long shortLineId = job.getLines().stream()
                .filter(l -> l.getPart().getPartNumber().equals("EL-ALT-90A"))
                .findFirst().orElseThrow().getId();

        int oilFilterBefore = stockOf("FT-OIL-STD");

        jobService.removeLineFromJob(jobId, shortLineId);
        JobView invoiced = jobService.completeJob(jobId);

        assertThat(invoiced.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(stockOf("FT-OIL-STD"))
                .as("exactly one oil filter is consumed")
                .isEqualTo(oilFilterBefore - 1);

        // Invoice arithmetic: 1 oil filter at 2150.00, plus 2.5 h at 2500.00
        assertThat(invoiced.partsTotal()).isEqualByComparingTo(new BigDecimal("2150.00"));
        assertThat(invoiced.labourTotal()).isEqualByComparingTo(new BigDecimal("6250.00"));
        assertThat(invoiced.grandTotal()).isEqualByComparingTo(new BigDecimal("8400.00"));
        assertThat(invoiced.grandTotal())
                .as("the total must equal parts plus labour")
                .isEqualByComparingTo(invoiced.partsTotal().add(invoiced.labourTotal()));
    }

    @Test
    @DisplayName("A completed job cannot be completed a second time")
    void doubleCompletionIsRefused() {
        ServiceJob job = shortStockJob();
        Long jobId = job.getId();

        Long shortLineId = job.getLines().stream()
                .filter(l -> l.getPart().getPartNumber().equals("EL-ALT-90A"))
                .findFirst().orElseThrow().getId();
        jobService.removeLineFromJob(jobId, shortLineId);
        jobService.completeJob(jobId);

        assertThatExceptionOfType(JobNotOpenException.class)
                .isThrownBy(() -> jobService.completeJob(jobId))
                .satisfies(ex -> assertThat(ex.getStatus()).isEqualTo(JobStatus.COMPLETED));
    }

    @Test
    @DisplayName("A job with no parts and no labour cannot be invoiced")
    void emptyJobIsRefused() {
        // Reloaded with findByIdFully because lines are lazy and open-in-view
        // is disabled - the summary query does not fetch them.
        ServiceJob empty = jobRepository.findAllWithVehicleAndOwner().stream()
                .map(j -> jobRepository.findByIdFully(j.getId()).orElseThrow())
                .filter(j -> j.getStatus().isOpen())
                .filter(j -> j.getLines().isEmpty() && j.getLabourHours().signum() == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed data should contain an empty open job"));

        assertThatExceptionOfType(EmptyJobException.class)
                .isThrownBy(() -> jobService.completeJob(empty.getId()));
    }

    // ------------------------------------------------------------------

    /** The seeded job that plans more alternators than the shelf holds. */
    private ServiceJob shortStockJob() {
        return jobRepository.findAllWithVehicleAndOwner().stream()
                .map(j -> jobRepository.findByIdFully(j.getId()).orElseThrow())
                .filter(j -> j.getStatus().isOpen())
                .filter(j -> j.getLines().stream()
                        .anyMatch(l -> l.getPart().getPartNumber().equals("EL-ALT-90A")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed data should contain the short-stock job"));
    }

    private int stockOf(String partNumber) {
        return partRepository.findAllWithCategory().stream()
                .filter(p -> p.getPartNumber().equals(partNumber))
                .findFirst()
                .map(Part::getStockQuantity)
                .orElseThrow(() -> new AssertionError("No part " + partNumber));
    }
}
