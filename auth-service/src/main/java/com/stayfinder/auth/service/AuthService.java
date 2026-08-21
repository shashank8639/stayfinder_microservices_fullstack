package com.stayfinder.auth.service;

import com.stayfinder.auth.dto.AuthResponse;
import com.stayfinder.auth.dto.LoginRequest;
import com.stayfinder.auth.dto.RegisterRequest;
import com.stayfinder.auth.dto.UserResponse;
import com.stayfinder.auth.entity.Role;
import com.stayfinder.auth.entity.UserAccount;
import com.stayfinder.auth.exception.DuplicateEmailException;
import com.stayfinder.auth.exception.InvalidCredentialsException;
import com.stayfinder.auth.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(Role.CUSTOMER));
        userAccountRepository.save(user);

        log.info("User registered email={}", email);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        log.info("User logged in email={}", email);
        return toAuthResponse(user);
    }

    public UserResponse toUserResponse(UserAccount user) {
        List<String> roles = user.getRoles().stream().map(Enum::name).sorted().toList();
        return new UserResponse(user.getId(), user.getEmail(), roles);
    }

    private AuthResponse toAuthResponse(UserAccount user) {
        return new AuthResponse(jwtService.issueToken(user), toUserResponse(user));
    }
}
