package lk.ac.nsbm.autocare.controller;

import lk.ac.nsbm.autocare.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Puts the signed-in user's real name into the model for every Thymeleaf page,
 * so the navigation bar can greet them by name rather than by username.
 *
 * Scoped by assignableTypes to the MVC controllers - the REST controller has
 * no model to populate.
 */
@ControllerAdvice(assignableTypes = {
        AuthController.class,
        CustomerJobController.class,
        AdminJobController.class,
        AdminPartController.class,
        PartBrowseController.class
})
public class CurrentUserAdvice {

    private final AppUserRepository appUserRepository;

    public CurrentUserAdvice(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @ModelAttribute("currentUserFullName")
    public String currentUserFullName(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return appUserRepository.findByUsername(authentication.getName())
                .map(user -> user.getFullName())
                .orElse(authentication.getName());
    }

    @ModelAttribute("currentUsername")
    public String currentUsername(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
