package com.smartrent.service;

import com.smartrent.entity.User;
import com.smartrent.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw unauthorized();
        }

        Long userId;

        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw unauthorized();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> unauthorized());

        if (!user.isEnabled()) {
            throw unauthorized();
        }

        return user;
    }

    public User requireOwnerOrAdmin() {
        User user = getCurrentUser();

        if (!"OWNER".equals(user.getRole())
                && !"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only owners or admins can manage properties"
            );
        }

        return user;
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Please log in with an active account"
        );
    }
}