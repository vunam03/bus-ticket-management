package com.busbooking.bus_ticket_management.security;


import com.busbooking.bus_ticket_management.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Objects;

/** Revoke existing sessions after password change/reset, disablement or role change. */
public class CredentialVersionFilter extends OncePerRequestFilter {
    private final UserRepository users;

    public CredentialVersionFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            boolean valid = users.findById(principal.getId())
                    .filter(user -> user.getEnabled())
                    .filter(user -> user.getCredentialVersion() == principal.getCredentialVersion())
                    .filter(user -> Objects.equals(user.getUsername(), principal.getUsername()))
                    .filter(user -> principal.getAuthorities().stream().anyMatch(authority ->
                            authority.getAuthority().equals("ROLE_" + user.getRole().getName())))
                    .isPresent();
            if (!valid) {
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/auth/login?expired");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
