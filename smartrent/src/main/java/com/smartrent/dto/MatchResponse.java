package com.smartrent.dto;

import com.smartrent.entity.Property;
import java.util.List;

public record MatchResponse(
        Property property,
        int matchScore,
        List<String> reasons
) {
}