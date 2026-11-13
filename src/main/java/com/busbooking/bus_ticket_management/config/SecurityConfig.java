package com.busbooking.bus_ticket_management.config;


import com.busbooking.bus_ticket_management.repository.UserRepository;
import com.busbooking.bus_ticket_management.security.*;
import com.busbooking.bus_ticket_management.service.CustomUserDetailsService;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import java.time.Clock;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomUserDetailsService details,
                                                   UserRepository users, Clock clock,
                                                   com.busbooking.bus_ticket_management.service.LoginAttemptService attempts,
                                                   com.busbooking.bus_ticket_management.service.AccountSessionService sessions,
                                                   AccountAuthenticationHandlers handlers) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(passwordEncoder());
        http.authenticationProvider(new GuardedAuthenticationProvider(provider, attempts))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**", "/booking/**", "/tickets/**", "/payments/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .successHandler(handlers)
                        .failureHandler(handlers)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .addLogoutHandler(handlers)
                        .logoutSuccessUrl("/auth/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .exceptionHandling(errors -> errors.accessDeniedPage("/auth/access-denied"))
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.NO_REFERRER)))
                .addFilterBefore(new AuthRateLimitFilter(clock), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CredentialVersionFilter(users, sessions), AuthorizationFilter.class);
        // Keep Spring Security's CSRF protection and secure default response headers.
        return http.build();
    }
}
