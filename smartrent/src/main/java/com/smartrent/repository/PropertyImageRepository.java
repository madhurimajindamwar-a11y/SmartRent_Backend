package com.smartrent.repository;

import com.smartrent.dto.PropertyImageResponse;
import com.smartrent.entity.PropertyImage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository
        extends JpaRepository<PropertyImage, Long> {

    long countByPropertyId(Long propertyId);

    Optional<PropertyImage> findByIdAndPropertyId(
            Long id,
            Long propertyId
    );

    @Query("""
            SELECT new com.smartrent.dto.PropertyImageResponse(
                image.id,
                image.propertyId,
                image.contentType,
                image.byteSize,
                image.width,
                image.height,
                image.sortOrder,
                image.createdAt
            )
            FROM PropertyImage image
            WHERE image.propertyId = :propertyId
            ORDER BY image.sortOrder ASC, image.id ASC
            """)
    List<PropertyImageResponse> findMetadataByPropertyId(
            @Param("propertyId") Long propertyId
    );

    @Query("""
            SELECT COALESCE(MAX(image.sortOrder), -1)
            FROM PropertyImage image
            WHERE image.propertyId = :propertyId
            """)
    int findMaximumSortOrder(
            @Param("propertyId") Long propertyId
    );
}