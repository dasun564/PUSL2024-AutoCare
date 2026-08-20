package lk.ac.nsbm.autocare.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Sign-in, landing and access-denied screens.
 *
 * Layer: @Controller. Chooses views only - no business logic, no repository.
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Sends each role to the screen that is useful to it. */
    @GetMapping("/")
    public String home(Authentication authentication) {
        boolean staff = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return staff ? "redirect:/admin/jobs" : "redirect:/my-jobs";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}
