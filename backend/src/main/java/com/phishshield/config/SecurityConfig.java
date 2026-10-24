package com.phishshield.config;

import com.phishshield.security.CustomAccessDeniedHandler;
import com.phishshield.security.CustomUserDetailsService;
import com.phishshield.security.JwtAuthenticationEntryPoint;
import com.phishshield.security.JwtAuthenticationFilter;
import com.phishshield.security.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final PasswordEncoder passwordEncoder;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationEntryPoint unauthorizedHandler,
                          CustomAccessDeniedHandler accessDeniedHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitingFilter rateLimitingFilter,
                          PasswordEncoder passwordEncoder,
                          CorsConfigurationSource corsConfigurationSource) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.passwordEncoder = passwordEncoder;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> {
                    headers.frameOptions(f -> f.sameOrigin());
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.permissionsPolicy(p -> p.policy("geolocation=(), microphone=(), camera=(), payment=()"));
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https:; frame-ancestors 'self';"
                    ));
                })
                .authorizeHttpRequests(auth -> auth
                        // Root, Health & Public Docs
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/api/health", "/api/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Public Auth Endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public Scanning & Informational Endpoints
                        .requestMatchers(HttpMethod.POST, "/api/scans/url").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/scans/email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/scans/message").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/scans/screenshot").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/scans/qr").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/scans/unified-image").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/scans/model/evaluations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/scans/ml-status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/scans/{id}").permitAll()
                        .requestMatchers("/api/prevention/**").permitAll()
                        .requestMatchers("/api/alerts/active").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reports").permitAll()

                        // Prahar AI Chatbot (Public endpoints for query & health, Authenticated for history)
                        .requestMatchers(HttpMethod.POST, "/api/chat", "/api/chat/message").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/chat/status").permitAll()
                        .requestMatchers("/api/chat/conversations/**").authenticated()

                        // H2 Console in Dev
                        .requestMatchers("/h2-console/**").permitAll()

                        // Admin / Analyst Endpoints
                        .requestMatchers("/api/dashboard/admin").hasAnyRole("ADMIN", "SECURITY_ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/reports").hasAnyRole("ADMIN", "SECURITY_ANALYST")
                        .requestMatchers(HttpMethod.PATCH, "/api/reports/*/status").hasAnyRole("ADMIN", "SECURITY_ANALYST")
                        .requestMatchers(HttpMethod.POST, "/api/alerts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/prevention").hasRole("ADMIN")
                        .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")
                        .requestMatchers("/api/incidents/admin/**").hasAnyRole("ADMIN", "SECURITY_ANALYST")

                        // Authenticated User Endpoints
                        .requestMatchers("/api/scans/my-scans").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/scans/*").authenticated()
                        .requestMatchers("/api/reports/my-reports").authenticated()
                        .requestMatchers("/api/dashboard/user").authenticated()
                        .requestMatchers("/api/feedback").authenticated()
                        .requestMatchers("/api/transactions/**").authenticated()
                        .requestMatchers("/api/incidents/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
