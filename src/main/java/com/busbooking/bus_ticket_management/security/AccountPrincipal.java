package com.busbooking.bus_ticket_management.security;


import com.busbooking.bus_ticket_management.model.User;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.io.Serial;
import java.util.Collection;
import java.util.List;

/** Serializable session identity. Contact details stay in the database. */
public class AccountPrincipal implements UserDetails, CredentialsContainer {
    @Serial
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String username;
    private String password;
    private final boolean enabled;
    private final long credentialVersion;
    private final List<SimpleGrantedAuthority> authorities;

    public AccountPrincipal(User user) {
        id = user.getId();
        username = user.getUsername();
        password = user.getPassword();
        enabled = user.getEnabled();
        credentialVersion = user.getCredentialVersion();
        authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
    }

    public Long getId() {
        return id;
    }

    public long getCredentialVersion() {
        return credentialVersion;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }
}
