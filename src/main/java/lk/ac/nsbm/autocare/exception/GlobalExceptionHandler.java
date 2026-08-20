package lk.ac.nsbm.autocare.exception;

import jakarta.servlet.http.HttpServletRequest;
import lk.ac.nsbm.autocare.controller.AdminJobController;
import lk.ac.nsbm.autocare.controller.AdminPartController;
import lk.ac.nsbm.autocare.controller.AuthController;
import lk.ac.nsbm.autocare.controller.CustomerJobController;
import lk.ac.nsbm.autocare.controller.PartBrowseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * CENTRALISED ERROR HANDLING for the Thymeleaf screens.
 *
 * Spring mechanism: {@code @ControllerAdvice} plus {@code @ExceptionHandler}.
 * Spring registers this as a global handler for every controller listed in
 * {@code assignableTypes}, so an exception thrown deep inside the service
 * layer unwinds past the controller and is caught here.
 *
 * Why this rather than try/catch in each controller method:
 *
 *  - The alternative is the same catch block copied into every handler, which
 *    duplicates code and drifts out of step. Here the mapping from failure to
 *    page exists exactly once.
 *  - Controllers stay readable: each method describes only the happy path,
 *    which is the whole point of the layer.
 *  - A try/catch inside a @Transactional call chain invites the mistake of
 *    swallowing the exception and letting a half-finished transaction commit.
 *    Letting it propagate is precisely what triggers the rollback that makes
 *    completeJob safe.
 *  - New failure types are handled automatically: because every business
 *    failure extends AutoCareException and carries its own message, adding a
 *    ninth exception needs no change to this class at all.
 *
 * The handler is shared; the MESSAGE is not. Each exception instance supplies
 * its own wording from the data it carries - which part, how many short, which
 * date, which limit.
 */
@ControllerAdvice(assignableTypes = {
        AuthController.class,
        CustomerJobController.class,
        AdminJobController.class,
        AdminPartController.class,
        PartBrowseController.class
})
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AutoCareException.class)
    public ModelAndView handleBusinessFailure(AutoCareException ex, HttpServletRequest request) {
        log.warn("Business rule rejected {} {}: [{}] {}",
                request.getMethod(), request.getRequestURI(), ex.getErrorCode(), ex.getMessage());

        ModelAndView mav = new ModelAndView("shared/error");
        mav.setStatus(ex.getHttpStatus());
        mav.addObject("errorTitle", ex.getTitle());
        mav.addObject("errorMessage", ex.getUserMessage());
        mav.addObject("errorAction", ex.getSuggestedAction());
        mav.addObject("errorCode", ex.getErrorCode());
        mav.addObject("stockFailure", ex instanceof InsufficientPartStockException);
        mav.addObject("bookingFailure",
                ex instanceof TooManyOpenJobsException
                        || ex instanceof GarageFullyBookedException
                        || ex instanceof InvalidBookingDateException);
        return mav;
    }

    /**
     * Last-resort handler. Anything unforeseen still reaches a styled page
     * rather than a stack trace, and is logged at ERROR with the full cause.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);

        ModelAndView mav = new ModelAndView("shared/error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("errorTitle", "Something went wrong");
        mav.addObject("errorMessage", "The system could not complete that request.");
        mav.addObject("errorAction", "Please try again. If it keeps happening, contact the service desk.");
        mav.addObject("errorCode", "INTERNAL_ERROR");
        mav.addObject("stockFailure", false);
        mav.addObject("bookingFailure", false);
        return mav;
    }
}
