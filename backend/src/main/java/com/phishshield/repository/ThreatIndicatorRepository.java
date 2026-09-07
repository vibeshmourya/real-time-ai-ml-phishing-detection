package com.phishshield.repository;

import com.phishshield.domain.ThreatDetection;
import com.phishshield.domain.ThreatIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThreatIndicatorRepository extends JpaRepository<ThreatIndicator, Long> {
    List<ThreatIndicator> findByDetection(ThreatDetection detection);
    List<ThreatIndicator> findByDetectionId(Long detectionId);
}
