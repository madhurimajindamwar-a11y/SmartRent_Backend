package com.smartrent.controller;

import com.smartrent.dto.UpdateProfileRequest;
import com.smartrent.dto.UserResponse;
import com.smartrent.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserResponse getProfile() {
        return profileService.getProfile();
    }

    @PutMapping
    public UserResponse updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(request);
    }
}