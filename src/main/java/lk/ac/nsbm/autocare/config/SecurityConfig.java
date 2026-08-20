package lk.ac.nsbm.autocare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Authentication, role-based authorisation and session management.
 *
 * Roles: ROLE_CUSTOMER and ROLE_ADMIN, derived polymorphically from
 * AppUser.getRole() by AppUserDetailsService.
 *
 * {@code @EnableMethodSecurity} switches on the @PreAuthorize used by
 * PartAdminService, so authorisation is enforced at two independent levels:
 * the URL here, and the service method itself.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Passwords are stored as BCrypt hashes, never in plain text. BCrypt is
     * deliberately slow and salts each hash, so two users with the same
     * password get different stored values and offline cracking is expensive.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * No AuthenticationProvider bean is declared. Spring Boot builds a
     * DaoAuthenticationProvider automatically from the two beans this
     * application publishes - the AppUserDetailsService component and the
     * PasswordEncoder above - which is exactly the wiring an explicit bean
     * would produce, without displacing the auto-configured
     * AuthenticationManager.
     */

    /**
     * Filter chain for the REST API only.
     *
     * The API is deliberately configured differently from the web interface:
     *
     *  - STATELESS, so no session is created or consulted. Each call carries
     *    its own credentials.
     *  - HTTP Basic rather than a login form, because an API client has no
     *    browser to be redirected to a login page.
     *  - CSRF disabled, which is correct HERE and only here. CSRF is an attack
     *    on ambient credentials: it works because a browser attaches the
     *    session cookie to a forged request automatically. With no session and
     *    explicit per-request credentials there is nothing for an attacker's
     *    page to ride on. The browser-facing chain below keeps CSRF fully on.
     *
     * Ordered first so it claims /api/** before the web chain sees it.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .anyRequest().hasRole("ADMIN")
            )
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(new ApiAwareAccessDeniedHandler("/403"))
            );

        return http.build();
    }

    /** Filter chain for the Thymeleaf web interface. */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // --- public ---
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Developer console, permitted so the database evidence
                // screenshot can be taken. It would be removed in production.
                .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()

                // --- garage staff only ---
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // --- customers only ---
                .requestMatchers("/book/**", "/my-jobs/**", "/my-jobs", "/my-vehicles/**", "/my-vehicles")
                    .hasRole("CUSTOMER")

                // --- any signed-in user ---
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?loggedOut")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                // Browsers get the styled /403 page; /api/** gets a real 403
                // with JSON. A plain accessDeniedPage forwards while keeping
                // the original method, so a refused DELETE would surface as
                // 405 Method Not Allowed against the GET-only /403 mapping.
                .accessDeniedHandler(new ApiAwareAccessDeniedHandler("/403"))
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                // Issues a new session id on login, defeating session fixation.
                .sessionFixation(fixation -> fixation.migrateSession())
                .maximumSessions(1)
            )
            // CSRF protection stays on for the whole application; it is
            // disabled only for the H2 console, a developer tool that posts
            // its own forms and cannot supply Spring's token.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**"))
            )
            // The H2 console renders in a frameset, which the default
            // X-Frame-Options: DENY header would block.
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }
}
