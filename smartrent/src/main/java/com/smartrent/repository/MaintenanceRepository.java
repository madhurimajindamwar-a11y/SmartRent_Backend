package com.smartrent.repository;

import com.smartrent.entity.MaintenanceRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRepository
        extends JpaRepository<MaintenanceRequest, Long> {

    @Query("""
        SELECT m FROM MaintenanceRequest m
        WHERE m.rentalId IN (
            SELECT r.id FROM Rental r
            WHERE r.tenantId = :tenantId
        )
        ORDER BY m.id DESC
        """)
    List<MaintenanceRequest> findForTenant(
            @Param("tenantId") Long tenantId);

    @Query("""
        SELECT m FROM MaintenanceRequest m
        WHERE m.rentalId IN (
            SELECT r.id FROM Rental r
            WHERE r.propertyId IN (
                SELECT p.id FROM Property p
                WHERE p.ownerId = :ownerId
            )
        )
        ORDER BY m.id DESC
        """)
    List<MaintenanceRequest> findForOwner(
            @Param("ownerId") Long ownerId);

    boolean existsByRentalIdAndTitleAndStatusIn(
            Long rentalId,
            String title,
            List<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MaintenanceRequest m WHERE m.id = :id")
    Optional<MaintenanceRequest> findByIdForUpdate(
            @Param("id") Long id);
}