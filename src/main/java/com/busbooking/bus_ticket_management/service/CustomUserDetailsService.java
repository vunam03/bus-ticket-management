package com.busbooking.bus_ticket_management.service;


import com.busbooking.bus_ticket_management.repository.UserRepository;
import com.busbooking.bus_ticket_management.security.AccountPrincipal;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByUsername(UserService.normalize(username))
                .map(AccountPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Tên đăng nhập hoặc mật khẩu không đúng."));
    }
}
