package com.smartrent.dto;

import java.time.Instant;

public record PropertyImageResponse(
        Long id,
        Long propertyId,
        String contentType,
        int byteSize,
        int width,
        int height,
        int sortOrder,
        Instant createdAt
) {
}