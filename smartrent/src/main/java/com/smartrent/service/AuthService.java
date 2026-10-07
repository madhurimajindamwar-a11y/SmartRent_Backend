package com.smartrent.service;

import com.smartrent.dto.LoginRequest;
import com.smartrent.dto.LoginResponse;
import com.smartrent.dto.RegisterRequest;
import com.smartrent.dto.UserResponse;
import com.smartrent.entity.User;
import com.smartrent.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;

        this.dummyPasswordHash =
                passwordEncoder.encode("unused-dummy-password");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (!"TENANT".equals(request.role())
                && !"OWNER".equals(request.role())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be TENANT or OWNER"
            );
        }

        if (request.password() == null
                || request.password().isBlank()
                || request.password().length() < 8
                || request.password()
                        .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must have at least 8 characters "
                            + "and must not exceed 72 UTF-8 bytes"
            );
        }

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setRole(request.role());
        user.setEnabled(true);

        User saved = userRepository.save(user);

        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        if (request.password() == null
                || request.password().isBlank()
                || request.password()
                        .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email).orElse(null);

        String storedHash = user == null
                ? dummyPasswordHash
                : user.getPasswordHash();

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                storedHash
        );

        if (user == null || !passwordMatches || !user.isEnabled()) {
            throw invalidCredentials();
        }

        Instant now = Instant.now();
        long expiresIn = 900;

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("smartrent")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("role", user.getRole())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        return new LoginResponse(
                token,
                "Bearer",
                expiresIn,
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
    }
}