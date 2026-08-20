package lk.ac.nsbm.autocare.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

import java.io.IOException;
import java.time.Instant;

/**
 * Decides how a refused request is answered, based on what asked.
 *
 * Browser requests are forwarded to the styled /403 page. REST requests are
 * not: forwarding preserves the original HTTP method, and /403 is a GET-only
 * mapping, so a refused {@code DELETE /api/parts/1} would be answered
 * "405 Method Not Allowed" - which tells an API client the wrong thing
 * entirely. Those get a plain 403 with a JSON body instead.
 */
public class ApiAwareAccessDeniedHandler implements AccessDeniedHandler {

    private final AccessDeniedHandler browserHandler;

    public ApiAwareAccessDeniedHandler(String errorPage) {
        AccessDeniedHandlerImpl impl = new AccessDeniedHandlerImpl();
        impl.setErrorPage(errorPage);
        this.browserHandler = impl;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        if (isApiRequest(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"timestamp":"%s","status":403,"errorCode":"ACCESS_DENIED",\
                    "title":"Access denied",\
                    "message":"Your account does not have permission to perform this operation."}"""
                    .formatted(Instant.now()));
            return;
        }

        browserHandler.handle(request, response, accessDeniedException);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json") && !accept.contains("text/html");
    }
}
