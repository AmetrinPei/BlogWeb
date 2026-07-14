package com.example.blog.auth;

import com.example.blog.user.UserRepository;
import com.example.blog.user.UserRole;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AdminUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> {
                    String role = user.getRole() == null ? UserRole.AUTHOR.name() : user.getRole().name();
                    return User.withUsername(user.getUsername())
                            .password(user.getPasswordHash())
                            .roles(role)
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }
}
