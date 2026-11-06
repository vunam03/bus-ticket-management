package com.busbooking.bus_ticket_management.security;


import com.busbooking.bus_ticket_management.service.LoginAttemptService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;

/** Compose with the standard DAO provider instead of replacing password authentication. */
public class GuardedAuthenticationProvider implements AuthenticationProvider {
    private final AuthenticationProvider delegate;
    private final LoginAttemptService attempts;

    public GuardedAuthenticationProvider(AuthenticationProvider delegate, LoginAttemptService attempts) {
        this.delegate = delegate;
        this.attempts = attempts;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        synchronized (attempts.monitor(username)) {
            attempts.checkAllowed(username);
            try {
                Authentication result = delegate.authenticate(authentication);
                attempts.succeeded(username);
                return result;
            } catch (BadCredentialsException exception) {
                attempts.failed(username);
                throw exception;
            }
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }
}
