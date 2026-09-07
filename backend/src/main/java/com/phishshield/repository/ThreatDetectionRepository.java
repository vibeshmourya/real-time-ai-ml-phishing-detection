package com.phishshield.repository;

import com.phishshield.domain.ThreatDetection;
import com.phishshield.domain.URLScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThreatDetectionRepository extends JpaRepository<ThreatDetection, Long> {
    Optional<ThreatDetection> findByScan(URLScan scan);
    Optional<ThreatDetection> findByScanId(Long scanId);
}
