package lk.ac.nsbm.autocare.config;

import lk.ac.nsbm.autocare.entity.ConsumablePart;
import lk.ac.nsbm.autocare.entity.Customer;
import lk.ac.nsbm.autocare.entity.JobLine;
import lk.ac.nsbm.autocare.entity.MechanicalPart;
import lk.ac.nsbm.autocare.entity.Part;
import lk.ac.nsbm.autocare.entity.PartCategory;
import lk.ac.nsbm.autocare.entity.ServiceJob;
import lk.ac.nsbm.autocare.entity.StaffMember;
import lk.ac.nsbm.autocare.entity.Vehicle;
import lk.ac.nsbm.autocare.repository.AppUserRepository;
import lk.ac.nsbm.autocare.repository.PartCategoryRepository;
import lk.ac.nsbm.autocare.repository.PartRepository;
import lk.ac.nsbm.autocare.repository.ServiceJobRepository;
import lk.ac.nsbm.autocare.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds the demonstration data the coursework evidence depends on.
 *
 * Runs once: if any account already exists the seeder does nothing, so
 * restarting never duplicates data or discards jobs booked through the UI.
 *
 * The seeded states are chosen so that every business rule can be
 * demonstrated and photographed:
 *
 *   10965261    - 1 open job whose parts EXCEED stock  -> "insufficient stock"
 *                 when staff try to complete it;
 *                 1 completed job with an invoice;
 *                 still has 1 booking allowance left   -> successful booking
 *   10965261B   - 2 open jobs                          -> "too many open jobs"
 *   k.perera,
 *   s.fernando  - 1 open job each, on the same day as
 *                 10965261B's two, filling the diary   -> "fully booked"
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final BigDecimal LABOUR_RATE = new BigDecimal("2500.00");

    private final AppUserRepository appUserRepository;
    private final VehicleRepository vehicleRepository;
    private final PartCategoryRepository categoryRepository;
    private final PartRepository partRepository;
    private final ServiceJobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository appUserRepository,
                      VehicleRepository vehicleRepository,
                      PartCategoryRepository categoryRepository,
                      PartRepository partRepository,
                      ServiceJobRepository jobRepository,
                      PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.vehicleRepository = vehicleRepository;
        this.categoryRepository = categoryRepository;
        this.partRepository = partRepository;
        this.jobRepository = jobRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            log.info("Seed data already present - skipping DataSeeder");
            return;
        }

        log.info("Seeding AutoCare demonstration data for student 10965261");

        LocalDate today = LocalDate.now();
        LocalDate fullDay = nextWorkingDay(today.plusDays(7));

        // ---------------------------------------------------------------
        // Categories
        // ---------------------------------------------------------------
        PartCategory engine = category("Engine", "Internal engine components and timing");
        PartCategory brakes = category("Brakes", "Pads, discs, callipers and hydraulics");
        PartCategory filters = category("Filters", "Oil, air, fuel and cabin filters");
        PartCategory electrical = category("Electrical", "Batteries, alternators, starters and ignition");
        PartCategory suspension = category("Suspension", "Dampers, springs and bushes");
        PartCategory fluids = category("Fluids", "Oils, coolants and hydraulic fluids");

        // ---------------------------------------------------------------
        // Parts inventory
        // ---------------------------------------------------------------
        Part oil = part(new ConsumablePart("FL-OIL-5W30", "Engine Oil 5W-30 (4 L)", fluids,
                new BigDecimal("6400.00"), 24, 6, 36, false));

        Part oilFilter = part(new ConsumablePart("FT-OIL-STD", "Oil Filter", filters,
                new BigDecimal("2150.00"), 18, 5, 60, false));

        Part airFilter = part(new ConsumablePart("FT-AIR-STD", "Air Filter", filters,
                new BigDecimal("3400.00"), 12, 4, 60, false));

        Part brakeFluid = part(new ConsumablePart("FL-BRK-DOT4", "Brake Fluid DOT 4 (1 L)", fluids,
                new BigDecimal("2800.00"), 9, 3, 24, true));

        Part coolant = part(new ConsumablePart("FL-CLT-GRN", "Coolant Concentrate (2 L)", fluids,
                new BigDecimal("3950.00"), 14, 4, 30, true));

        Part brakePads = part(new MechanicalPart("BR-PAD-FRT", "Brake Pads - Front Set", brakes,
                new BigDecimal("11500.00"), 8, 3, "Bosch", 24, false));

        Part brakeDisc = part(new MechanicalPart("BR-DSC-FRT", "Brake Disc - Front", brakes,
                new BigDecimal("14200.00"), 6, 2, "Brembo", 24, true));

        // Deliberately scarce: only ONE in stock. The seeded job below plans
        // for three, so completing that job must fail and roll back.
        Part alternator = part(new MechanicalPart("EL-ALT-90A", "Alternator 90A", electrical,
                new BigDecimal("48500.00"), 1, 2, "Denso", 12, true));

        Part battery = part(new MechanicalPart("EL-BAT-70A", "Battery 70Ah", electrical,
                new BigDecimal("32900.00"), 7, 3, "Exide", 18, false));

        Part timingBelt = part(new MechanicalPart("EN-TBT-KIT", "Timing Belt Kit", engine,
                new BigDecimal("26750.00"), 5, 2, "Gates", 24, true));

        Part sparkPlugs = part(new MechanicalPart("EN-SPK-SET", "Spark Plug Set (4)", engine,
                new BigDecimal("7800.00"), 15, 5, "NGK", 12, false));

        part(new MechanicalPart("SU-SHK-REAR", "Shock Absorber - Rear", suspension,
                new BigDecimal("18600.00"), 4, 2, "KYB", 24, true));

        // Carries the student number so authorship is visible in the data.
        part(new MechanicalPart("SE-10965261", "Diagnostic Service Kit (10965261 Edition)", engine,
                new BigDecimal("15000.00"), 3, 1, "AutoCare Workshop", 6, false));

        // ---------------------------------------------------------------
        // Accounts and vehicles
        // ---------------------------------------------------------------
        Customer main = customer("10965261", "Dasun Edirisinghe", "071 234 5678",
                "14 Galle Road, Colombo 03");
        Vehicle mainCar = vehicle(main, "WP-10965261", "Toyota", "Corolla", 2019, 68_400);
        Vehicle mainVan = vehicle(main, "WP-CAB-4471", "Nissan", "Caravan", 2016, 143_900);

        Customer atLimit = customer("10965261B", "Dasun Edirisinghe (open-jobs test account)",
                "071 234 5679", "14 Galle Road, Colombo 03");
        Vehicle limitCar = vehicle(atLimit, "WP-CAR-8802", "Honda", "Civic", 2020, 41_250);

        Customer perera = customer("k.perera", "Kasun Perera", "077 555 1200", "8 Temple Lane, Kandy");
        Vehicle pereraCar = vehicle(perera, "CP-CAR-3390", "Suzuki", "Swift", 2018, 88_700);

        Customer fernando = customer("s.fernando", "Sanduni Fernando", "076 909 4413", "22 Marine Drive, Negombo");
        Vehicle fernandoCar = vehicle(fernando, "WP-CAR-1177", "Mitsubishi", "Lancer", 2015, 162_300);

        StaffMember staff = new StaffMember("admin10965261",
                passwordEncoder.encode("admin123"),
                "Nimal Jayasuriya", "011 244 8890", "AC-10965261", "Service Manager");
        appUserRepository.save(staff);

        // ---------------------------------------------------------------
        // Job 1 - open, and deliberately impossible to complete.
        // It plans 3 alternators when only 1 is in stock, so pressing
        // "Complete job" raises InsufficientPartStockException and the
        // transaction rolls back: the oil filter consumed earlier in the same
        // loop is restored, and the job stays open.
        // ---------------------------------------------------------------
        ServiceJob stockFailureJob = job("AC-" + today.getYear() + "-0001", mainCar, today,
                "Battery warning light stays on and there is a whining noise from the engine bay.");
        stockFailureJob.beginWork();
        stockFailureJob.addLine(oilFilter, 1);
        stockFailureJob.addLine(alternator, 3);      // stock is 1
        stockFailureJob.setLabour(new BigDecimal("2.50"));

        // ---------------------------------------------------------------
        // Job 2 - already completed, with a real invoice, so the customer's
        // history and the invoice screen have content.
        // ---------------------------------------------------------------
        ServiceJob doneJob = job("AC-" + today.getYear() + "-0002", mainVan, today.minusDays(21),
                "Routine 140,000 km service and front brake inspection.");
        doneJob.beginWork();
        doneJob.addLine(oil, 1);
        doneJob.addLine(oilFilter, 1);
        doneJob.addLine(airFilter, 1);
        doneJob.addLine(brakePads, 1);
        doneJob.setLabour(new BigDecimal("3.00"));
        consumeLines(doneJob);
        doneJob.complete(LocalDateTime.now().minusDays(20));

        // ---------------------------------------------------------------
        // Jobs 3-6 - four bookings on the SAME day. They fill the garage's
        // daily capacity of 4, and give 10965261B its 2 open jobs.
        // ---------------------------------------------------------------
        job("AC-" + today.getYear() + "-0003", limitCar, fullDay,
                "Clutch slipping when pulling away in second gear.");
        job("AC-" + today.getYear() + "-0004", limitCar, fullDay,
                "Air conditioning blows warm and the cabin filter needs replacing.");
        job("AC-" + today.getYear() + "-0005", pereraCar, fullDay,
                "Front suspension knocking over rough surfaces.");
        job("AC-" + today.getYear() + "-0006", fernandoCar, fullDay,
                "Annual service plus emissions check before the vehicle test.");

        log.info("Seed complete: {} accounts, {} vehicles, {} categories, {} parts, {} jobs",
                appUserRepository.count(), vehicleRepository.count(), categoryRepository.count(),
                partRepository.count(), jobRepository.count());
        log.info("The garage diary is FULL on {} - use that date to demonstrate the capacity rule", fullDay);
        log.info("Sign in as 10965261 / customer123 (customer) or admin10965261 / admin123 (staff)");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private PartCategory category(String name, String description) {
        return categoryRepository.save(new PartCategory(name, description));
    }

    private Part part(Part part) {
        return partRepository.save(part);
    }

    private Customer customer(String username, String fullName, String phone, String address) {
        Customer customer = new Customer(username, passwordEncoder.encode("customer123"),
                fullName, phone, address);
        appUserRepository.save(customer);
        return customer;
    }

    private Vehicle vehicle(Customer owner, String registration, String make, String model,
                            int year, int mileage) {
        Vehicle vehicle = new Vehicle(registration, make, model, year, mileage);
        owner.addVehicle(vehicle);
        return vehicleRepository.save(vehicle);
    }

    private ServiceJob job(String jobNumber, Vehicle vehicle, LocalDate bookedFor, String problem) {
        ServiceJob job = new ServiceJob(jobNumber, vehicle, bookedFor, problem, LABOUR_RATE,
                LocalDateTime.now().minusDays(1));
        return jobRepository.save(job);
    }

    /** Consumes a completed job's parts, exactly as ServiceJobServiceImpl does. */
    private void consumeLines(ServiceJob job) {
        for (JobLine line : job.getLines()) {
            line.getPart().consumeStock(line.getQuantity());
        }
    }

    /** The garage is closed on Sundays, so bookings roll to the Monday. */
    private LocalDate nextWorkingDay(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY ? date.plusDays(1) : date;
    }
}
