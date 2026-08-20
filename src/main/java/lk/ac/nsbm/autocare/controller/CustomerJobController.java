package lk.ac.nsbm.autocare.controller;

import jakarta.validation.Valid;
import lk.ac.nsbm.autocare.dto.BookingForm;
import lk.ac.nsbm.autocare.dto.JobView;
import lk.ac.nsbm.autocare.dto.VehicleForm;
import lk.ac.nsbm.autocare.exception.DuplicateRegistrationException;
import lk.ac.nsbm.autocare.service.ServiceJobService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Customer-facing screens: my vehicles, book a service, my jobs.
 *
 * Layer: @Controller. Note what is absent - no try/catch anywhere. Business
 * failures propagate out of the service as AutoCareException subclasses and
 * are rendered centrally by GlobalExceptionHandler.
 *
 * SECURITY: every service call passes {@code authentication.getName()}, the
 * username Spring Security read from the server-side session. No handler here
 * accepts a customer identifier from the request, so a crafted form cannot
 * book a service onto, or read the job history of, another customer's account.
 */
@Controller
public class CustomerJobController {

    private final ServiceJobService jobService;

    public CustomerJobController(ServiceJobService jobService) {
        this.jobService = jobService;
    }

    // ---------------- vehicles ----------------

    @GetMapping("/my-vehicles")
    public String myVehicles(Authentication authentication, Model model) {
        model.addAttribute("vehicles", jobService.listMyVehicles(authentication.getName()));
        if (!model.containsAttribute("vehicleForm")) {
            model.addAttribute("vehicleForm", new VehicleForm());
        }
        return "customer/vehicles";
    }

    @PostMapping("/my-vehicles")
    public String registerVehicle(@Valid @ModelAttribute("vehicleForm") VehicleForm vehicleForm,
                                  BindingResult binding,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("vehicles", jobService.listMyVehicles(authentication.getName()));
            return "customer/vehicles";
        }
        try {
            var saved = jobService.registerVehicle(authentication.getName(), vehicleForm);
            redirect.addFlashAttribute("flash", "Registered " + saved.registrationNumber() + ".");
            return "redirect:/my-vehicles";
        } catch (DuplicateRegistrationException ex) {
            // Attached to the offending field so the message appears beside the
            // registration box rather than as a page-level error.
            binding.rejectValue("registrationNumber", "duplicate", ex.getUserMessage());
            model.addAttribute("vehicles", jobService.listMyVehicles(authentication.getName()));
            return "customer/vehicles";
        }
    }

    // ---------------- booking ----------------

    @GetMapping("/book")
    public String bookingForm(Authentication authentication, Model model) {
        model.addAttribute("vehicles", jobService.listMyVehicles(authentication.getName()));
        model.addAttribute("capacity", jobService.describeCapacity(authentication.getName()));
        if (!model.containsAttribute("bookingForm")) {
            model.addAttribute("bookingForm", new BookingForm());
        }
        return "customer/book";
    }

    @PostMapping("/book")
    public String book(@Valid @ModelAttribute("bookingForm") BookingForm bookingForm,
                       BindingResult binding,
                       Authentication authentication,
                       Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("vehicles", jobService.listMyVehicles(authentication.getName()));
            model.addAttribute("capacity", jobService.describeCapacity(authentication.getName()));
            return "customer/book";
        }

        JobView job = jobService.bookService(authentication.getName(), bookingForm);
        model.addAttribute("job", job);
        return "customer/booking-confirmed";
    }

    // ---------------- my jobs ----------------

    @GetMapping("/my-jobs")
    public String myJobs(Authentication authentication, Model model) {
        model.addAttribute("jobs", jobService.listMyJobs(authentication.getName()));
        model.addAttribute("capacity", jobService.describeCapacity(authentication.getName()));
        return "customer/jobs";
    }

    @GetMapping("/my-jobs/{id}")
    public String myJobDetail(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("job", jobService.getMyJob(authentication.getName(), id));
        return "customer/job-detail";
    }

    @PostMapping("/my-jobs/{id}/cancel")
    public String cancelMyJob(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        JobView cancelled = jobService.cancelMyJob(authentication.getName(), id);
        redirect.addFlashAttribute("flash", "Job " + cancelled.jobNumber() + " has been cancelled.");
        return "redirect:/my-jobs";
    }
}
