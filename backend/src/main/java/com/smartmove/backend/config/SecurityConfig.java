
package com.smartmove.backend.config;

import com.smartmove.backend.entity.UserAccount;
import com.smartmove.backend.repository.UserAccountRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String PASSENGER = "PASSENGER";
    private static final String DRIVER = "DRIVER";
    private static final String ADMIN = "ADMIN";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private static final String[] ADMIN_ROLES = {
            ADMIN,
            SUPER_ADMIN
    };

    private static final String[] STAFF_ROLES = {
            DRIVER,
            ADMIN,
            SUPER_ADMIN
    };

    private static final String[] ALL_ROLES = {
            PASSENGER,
            DRIVER,
            ADMIN,
            SUPER_ADMIN
    };

    private final UserAccountRepository userAccountRepository;

    public SecurityConfig(
            UserAccountRepository userAccountRepository
    ) {
        this.userAccountRepository = userAccountRepository;
    }

    // ==========================================
    // PASSWORD ENCODING
    // ==========================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==========================================
    // USER DETAILS FROM ORACLE
    // ==========================================

    @Bean
    public UserDetailsService userDetailsService() {

        return username -> {

            if (username == null || username.isBlank()) {
                throw new UsernameNotFoundException(
                        "Invalid username"
                );
            }

            UserAccount account =
                    userAccountRepository
                            .findByUsernameIgnoreCase(
                                    username.trim()
                            )
                            .orElseThrow(() ->
                                    new UsernameNotFoundException(
                                            "Invalid username or password"
                                    )
                            );

            String role = normalizeRole(
                    account.getRole()
            );

            if (!Set.of(
                    PASSENGER,
                    DRIVER,
                    ADMIN,
                    SUPER_ADMIN
            ).contains(role)) {
                throw new UsernameNotFoundException(
                        "Account role is invalid"
                );
            }

            String passwordHash = account.getPasswordHash();

            if (passwordHash == null
                    || passwordHash.isBlank()) {
                throw new UsernameNotFoundException(
                        "Account credentials are unavailable"
                );
            }

            String status = account.getAccountStatus();

            boolean enabled =
                    "ACTIVE".equalsIgnoreCase(status);

            return User.withUsername(
                            account.getUsername()
                    )
                    .password(passwordHash)
                    .authorities(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role
                            )
                    )
                    .disabled(!enabled)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .build();
        };
    }

    // ==========================================
    // AUTHENTICATION PROVIDER
    // ==========================================

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        provider.setPasswordEncoder(
                passwordEncoder
        );

        return provider;
    }

    // ==========================================
    // AUTHENTICATION MANAGER
    // ==========================================

    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider authenticationProvider
    ) {
        return new ProviderManager(
                authenticationProvider
        );
    }

    // ==========================================
    // HTTP SECURITY
    // ==========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .httpBasic(Customizer.withDefaults())

                .formLogin(form -> form.disable())

                .logout(logout -> logout.disable())

                .authorizeHttpRequests(auth -> auth

                        // ----------------------------------
                        // PUBLIC AUTHENTICATION
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/info"
                        ).permitAll()

                        // ----------------------------------
                        // PUBLIC HEALTH CHECKS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/system/health",
                                "/api/system/ready"
                        ).permitAll()

                        // ----------------------------------
                        // SUPER ADMIN ONLY
                        // ----------------------------------

                        .requestMatchers(
                                "/api/audit-logs/**",
                                "/api/database-initialization/**"
                        ).hasRole(SUPER_ADMIN)

                        // ----------------------------------
                        // SYSTEM CONFIGURATION
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/system-configurations",
                                "/api/system-configurations/defaults/initialize"
                        ).hasRole(SUPER_ADMIN)

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/system-configurations/**"
                        ).hasRole(SUPER_ADMIN)

                        .requestMatchers(
                                "/api/system-configurations/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // SYSTEM DIAGNOSTICS
                        // ----------------------------------

                        .requestMatchers(
                                "/api/system/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // ADMIN MANAGEMENT
                        // ----------------------------------

                        .requestMatchers(
                                "/api/admin/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // REPORTS
                        // ----------------------------------

                        .requestMatchers(
                                "/api/reports/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // CURRENT USER
                        // ----------------------------------

                        .requestMatchers(
                                "/api/users/me/**"
                        ).hasAnyRole(ALL_ROLES)

                        // ----------------------------------
                        // PASSENGERS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/passengers"
                        ).hasAnyRole(ADMIN_ROLES)

                        .requestMatchers(
                                "/api/passengers/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        // ----------------------------------
                        // DRIVERS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/drivers"
                        ).hasAnyRole(ADMIN_ROLES)

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/drivers/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/drivers/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/drivers/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        .requestMatchers(
                                "/api/drivers/**"
                        ).hasAnyRole(STAFF_ROLES)

                        // ----------------------------------
                        // VEHICLES
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/vehicles/**"
                        ).hasAnyRole(STAFF_ROLES)

                        .requestMatchers(
                                "/api/vehicles/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // ROUTES
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/routes/**"
                        ).hasAnyRole(ALL_ROLES)

                        .requestMatchers(
                                "/api/routes/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // TRIPS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/trips/**"
                        ).hasAnyRole(ALL_ROLES)

                        .requestMatchers(
                                "/api/trips/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // BOOKINGS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/bookings/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/bookings/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/bookings/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/bookings/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/bookings/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // PAYMENT RECORDS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/payments/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                "/api/payments/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // DRIVER-REPORTED VEHICLE ISSUES
                        .requestMatchers(HttpMethod.POST, "/api/driver-issues")
                        .hasRole(DRIVER)
                        .requestMatchers(HttpMethod.GET, "/api/driver-issues/mine")
                        .hasRole(DRIVER)
                        .requestMatchers("/api/driver-issues/admin/**")
                        .hasAnyRole(ADMIN_ROLES)

                        // MAINTENANCE
                        // ----------------------------------

                        .requestMatchers(
                                "/api/maintenance/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // FEEDBACK
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/feedback/**"
                        ).hasAnyRole(ALL_ROLES)

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/feedback/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/feedback/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/feedback/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        // ----------------------------------
                        // ANNOUNCEMENTS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/announcements/**"
                        ).hasAnyRole(ALL_ROLES)

                        .requestMatchers(
                                "/api/announcements/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // VEHICLE DOCUMENTS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/vehicle-documents/**"
                        ).hasAnyRole(STAFF_ROLES)

                        .requestMatchers(
                                "/api/vehicle-documents/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // DRIVER LOCATION TRACKING
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/tracking/**"
                        ).hasAnyRole(STAFF_ROLES)

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/tracking/**"
                        ).hasAnyRole(STAFF_ROLES)

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/tracking/**"
                        ).hasAnyRole(STAFF_ROLES)

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/tracking/**"
                        ).hasAnyRole(ALL_ROLES)

                        // ----------------------------------
                        // TRIP STATUS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/trip-status/**"
                        ).hasAnyRole(ALL_ROLES)

                        .requestMatchers(
                                "/api/trip-status/**"
                        ).hasAnyRole(STAFF_ROLES)

                        // ----------------------------------
                        // STAFF TRANSPORT REQUESTS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/staff-transport/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/staff-transport/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/staff-transport/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/staff-transport/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/staff-transport/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // ON-DEMAND TRANSPORT REQUESTS
                        // ----------------------------------

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/on-demand/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/on-demand/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/on-demand/**"
                        ).hasAnyRole(
                                PASSENGER,
                                ADMIN,
                                SUPER_ADMIN
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/on-demand/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/on-demand/**"
                        ).hasAnyRole(ADMIN_ROLES)

                        // ----------------------------------
                        // AUTHENTICATED AUTH ENDPOINTS
                        // ----------------------------------

                        .requestMatchers(
                                "/api/auth/**"
                        ).authenticated()

                        // ----------------------------------
                        // DENY UNKNOWN ENDPOINTS
                        // ----------------------------------

                        .anyRequest().denyAll()
                )

                .exceptionHandling(exceptions ->
                        exceptions
                                .authenticationEntryPoint(
                                        (request, response, exception) ->
                                                writeError(
                                                        response,
                                                        HttpStatus.UNAUTHORIZED,
                                                        "Authentication required"
                                                )
                                )
                                .accessDeniedHandler(
                                        (request, response, exception) ->
                                                writeError(
                                                        response,
                                                        HttpStatus.FORBIDDEN,
                                                        "Access denied"
                                                )
                                )
                );

        return http.build();
    }

    // ==========================================
    // CORS CONFIGURATION
    // ==========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOriginPatterns(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:5175",
                        "http://localhost:5176",
                        "http://localhost:3000",
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:5174",
                        "http://127.0.0.1:5175",
                        "http://127.0.0.1:5176",
                        "http://127.0.0.1:3000"
                )
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Location"
                )
        );

        configuration.setAllowCredentials(false);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/api/**",
                configuration
        );

        return source;
    }

    // ==========================================
    // ROLE NORMALIZATION
    // ==========================================

    private String normalizeRole(String role) {

        if (role == null || role.isBlank()) {
            return "";
        }

        String normalized =
                role.trim().toUpperCase(Locale.ROOT);

        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        return normalized;
    }

    // ==========================================
    // JSON SECURITY ERROR RESPONSE
    // ==========================================

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws java.io.IOException {

        response.setStatus(status.value());

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        String body = """
                {
                  "status": %d,
                  "error": "%s",
                  "message": "%s"
                }
                """.formatted(
                status.value(),
                status.getReasonPhrase(),
                message
        );

        response.getWriter().write(body);
    }
}
