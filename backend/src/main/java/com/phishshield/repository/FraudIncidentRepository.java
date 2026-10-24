package com.phishshield.repository;

import com.phishshield.domain.FraudIncident;
import com.phishshield.domain.IncidentSeverity;
import com.phishshield.domain.IncidentStatus;
import com.phishshield.domain.IncidentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudIncidentRepository extends JpaRepository<FraudIncident, Long> {

    Page<FraudIncident> findByUserId(Long userId, Pageable pageable);

    Optional<FraudIncident> findByIdAndUserId(Long id, Long userId);

    Page<FraudIncident> findByUserIdAndStatus(Long userId, IncidentStatus status, Pageable pageable);

    Page<FraudIncident> findByUserIdAndIncidentType(Long userId, IncidentType incidentType, Pageable pageable);

    Page<FraudIncident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<FraudIncident> findBySeverity(IncidentSeverity severity, Pageable pageable);

    Page<FraudIncident> findByStatusAndSeverity(IncidentStatus status, IncidentSeverity severity, Pageable pageable);

    @Query("SELECT COUNT(i) FROM FraudIncident i WHERE i.user.id = :userId AND i.status NOT IN ('RESOLVED', 'CLOSED')")
    long countActiveIncidentsByUserId(@Param("userId") Long userId);

    List<FraudIncident> findTop5ByUserIdOrderByIncidentDateTimeDesc(Long userId);
}
