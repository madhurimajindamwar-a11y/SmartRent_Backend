package com.smartrent.repository;

import com.smartrent.entity.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RentalRepository
        extends JpaRepository<Rental, Long> {

    List<Rental> findByTenantIdOrderByIdDesc(Long tenantId);

    boolean existsByPropertyIdAndStatus(
            Long propertyId,
            String status
    );

    boolean existsByTenantIdAndStatus(
            Long tenantId,
            String status
    );

    @Query("""
            SELECT r FROM Rental r
            WHERE r.propertyId IN (
                SELECT p.id FROM Property p
                WHERE p.ownerId = :ownerId
            )
            ORDER BY r.id DESC
            """)
    List<Rental> findRentalsForOwner(
            @Param("ownerId") Long ownerId
    );
}