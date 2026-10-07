package com.smartrent.repository;

import com.smartrent.entity.RentalRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentalRequestRepository
        extends JpaRepository<RentalRequest, Long> {

    List<RentalRequest> findByTenantIdOrderByIdDesc(Long tenantId);

    boolean existsByTenantIdAndPropertyIdAndStatus(
            Long tenantId,
            Long propertyId,
            String status
    );

    List<RentalRequest> findByPropertyIdAndStatus(
            Long propertyId,
            String status
    );

    @Query("""
            SELECT r FROM RentalRequest r
            WHERE r.propertyId IN (
                SELECT p.id FROM Property p
                WHERE p.ownerId = :ownerId
            )
            ORDER BY r.id DESC
            """)
    List<RentalRequest> findRequestsForOwner(
            @Param("ownerId") Long ownerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RentalRequest r WHERE r.id = :id")
    Optional<RentalRequest> findByIdForUpdate(@Param("id") Long id);
}