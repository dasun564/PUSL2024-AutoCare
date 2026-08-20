package lk.ac.nsbm.autocare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the whole Spring context - entities, repositories, services,
 * controllers, both security filter chains and the seeder - wires up.
 */
@SpringBootTest
@ActiveProfiles("test")
class AutocareApplicationTests {

    @Test
    @DisplayName("Application context loads")
    void contextLoads() {
        // Fails the build if any bean cannot be created or injected.
    }
}
