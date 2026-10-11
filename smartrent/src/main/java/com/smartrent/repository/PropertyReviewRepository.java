package com.smartrent.repository;

import com.smartrent.entity.PropertyReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyReviewRepository
        extends JpaRepository<PropertyReview, Long> {

    Optional<PropertyReview> findByPropertyIdAndTenantId(
            Long propertyId,
            Long tenantId
    );

    List<PropertyReview> findTop50ByPropertyIdOrderByUpdatedAtDescIdDesc(
            Long propertyId
    );

    long countByPropertyId(Long propertyId);

    @Query("""
            SELECT AVG(r.rating)
            FROM PropertyReview r
            WHERE r.propertyId = :propertyId
            """)
    Double averageRating(@Param("propertyId") Long propertyId);
}