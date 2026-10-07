package com.smartrent.repository;

import com.smartrent.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository
        extends JpaRepository<Favorite, Long> {
List<Favorite> findByTenantIdOrderByIdDesc(Long tenantId);

Optional<Favorite> findByTenantIdAndPropertyId(
        Long tenantId,
        Long propertyId
);

boolean existsByTenantIdAndPropertyId(
        Long tenantId,
        Long propertyId
);
}