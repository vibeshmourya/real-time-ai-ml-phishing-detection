package com.phishshield.service;

import com.phishshield.domain.Role;
import com.phishshield.domain.RoleName;
import com.phishshield.domain.User;
import com.phishshield.dto.request.LoginRequest;
import com.phishshield.dto.request.RegisterRequest;
import com.phishshield.dto.response.AuthResponse;
import com.phishshield.dto.response.UserProfileResponse;
import com.phishshield.exception.BadRequestException;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.RoleRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.security.JwtTokenProvider;
import com.phishshield.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address is already in use");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_USER)
                        .description("Standard platform user")
                        .build()));

        User user = User.builder()
                .username(request.getUsername().trim().toLowerCase())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(userRole)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        auditLogService.logAction(savedUser, "USER_REGISTRATION", "USER", savedUser.getId().toString(), "User registered successfully");

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs())
                .user(mapToUserProfile(savedUser))
                .build();
    }

    public AuthResponse authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail().trim(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        auditLogService.logAction(user, "USER_LOGIN", "USER", user.getId().toString(), "User logged in successfully");

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs())
                .user(mapToUserProfile(user))
                .build();
    }

    public UserProfileResponse mapToUserProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getName().name())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
