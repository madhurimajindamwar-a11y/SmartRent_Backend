package com.smartrent.service;

import com.smartrent.dto.MatchRequest;
import com.smartrent.dto.MatchResponse;
import com.smartrent.entity.Property;
import com.smartrent.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MatchingService {

    private final PropertyService propertyService;
    private final CurrentUserService currentUserService;

    public MatchingService(
            PropertyService propertyService,
            CurrentUserService currentUserService) {

        this.propertyService = propertyService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> findMatches(MatchRequest preferences) {
        User user = currentUserService.getCurrentUser();

        if (!"TENANT".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only tenants can request personalized matches"
            );
        }

        int totalWeight = applicableWeight(preferences);

        if (totalWeight == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide at least one preference"
            );
        }

        // Strict budget limit; locality and room type remain preferences.
        List<Property> candidates = propertyService.searchProperties(
                null,
                preferences.maxRent(),
                null
        );

        List<MatchResponse> results = new ArrayList<>();

        for (Property property : candidates) {
            results.add(score(property, preferences, totalWeight));
        }

        results.sort(
                Comparator.comparingInt(MatchResponse::matchScore)
                        .reversed()
                        .thenComparing(
                                result -> result.property().getRent()
                        )
                        .thenComparing(
                                result -> result.property().getId()
                        )
        );

        return results;
    }

    private MatchResponse score(
            Property property,
            MatchRequest preferences,
            int totalWeight) {

        double earnedPoints = 0;
        List<String> reasons = new ArrayList<>();

        if (preferences.maxRent() != null) {
            earnedPoints += 40;
            reasons.add("Within your maximum budget");
        }

        if (hasText(preferences.locality())) {
            if (property.getLocality().trim().equalsIgnoreCase(
                    preferences.locality().trim())) {
                earnedPoints += 30;
                reasons.add("Matches your preferred locality");
            } else {
                reasons.add("Different from your preferred locality");
            }
        }

        if (hasText(preferences.roomType())) {
            if (property.getRoomType().trim().equalsIgnoreCase(
                    preferences.roomType().trim())) {
                earnedPoints += 20;
                reasons.add("Matches your preferred room type");
            } else {
                reasons.add("Different from your preferred room type");
            }
        }

        int requestedAmenities = amenityCount(preferences);
        int matchedAmenities = 0;

        if (Boolean.TRUE.equals(preferences.wifi())) {
            if (property.isWifi()) {
                matchedAmenities++;
                reasons.add("Wi-Fi available");
            } else {
                reasons.add("Preferred Wi-Fi is unavailable");
            }
        }

        if (Boolean.TRUE.equals(preferences.food())) {
            if (property.isFood()) {
                matchedAmenities++;
                reasons.add("Food available");
            } else {
                reasons.add("Preferred food service is unavailable");
            }
        }

        if (Boolean.TRUE.equals(preferences.ac())) {
            if (property.isAc()) {
                matchedAmenities++;
                reasons.add("AC available");
            } else {
                reasons.add("Preferred AC is unavailable");
            }
        }

        if (requestedAmenities > 0) {
            earnedPoints +=
                    10.0 * matchedAmenities / requestedAmenities;
        }

        int matchScore = (int) Math.round(
                100.0 * earnedPoints / totalWeight
        );

        return new MatchResponse(property, matchScore, reasons);
    }

    private int applicableWeight(MatchRequest preferences) {
        int weight = 0;

        if (preferences.maxRent() != null) {
            weight += 40;
        }

        if (hasText(preferences.locality())) {
            weight += 30;
        }

        if (hasText(preferences.roomType())) {
            weight += 20;
        }

        if (amenityCount(preferences) > 0) {
            weight += 10;
        }

        return weight;
    }

    private int amenityCount(MatchRequest preferences) {
        int count = 0;

        if (Boolean.TRUE.equals(preferences.wifi())) {
            count++;
        }

        if (Boolean.TRUE.equals(preferences.food())) {
            count++;
        }

        if (Boolean.TRUE.equals(preferences.ac())) {
            count++;
        }

        return count;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}