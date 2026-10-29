package com.phishshield.security;

import com.phishshield.domain.User;
import com.phishshield.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new UsernameNotFoundException("Username or email cannot be empty");
        }

        String lookup = usernameOrEmail.trim();
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(lookup, lookup);

        // Alias compatibility: support both @prahar.internal and @phishshield.security domains
        if (userOpt.isEmpty() && lookup.contains("@prahar.internal")) {
            String alt = lookup.replace("@prahar.internal", "@phishshield.security");
            userOpt = userRepository.findByUsernameOrEmail(alt, alt);
        } else if (userOpt.isEmpty() && lookup.contains("@phishshield.security")) {
            String alt = lookup.replace("@phishshield.security", "@prahar.internal");
            userOpt = userRepository.findByUsernameOrEmail(alt, alt);
        }

        // Preset username fallback
        if (userOpt.isEmpty()) {
            if ("user".equalsIgnoreCase(lookup) || "demo".equalsIgnoreCase(lookup)) {
                userOpt = userRepository.findByUsernameOrEmail("demo_user", "demo_user");
            }
        }

        User user = userOpt.orElseThrow(() -> 
                new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail));

        return UserPrincipal.create(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return UserPrincipal.create(user);
    }
}
