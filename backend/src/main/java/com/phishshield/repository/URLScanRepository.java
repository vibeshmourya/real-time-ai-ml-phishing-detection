package com.phishshield.repository;

import com.phishshield.domain.RiskLevel;
import com.phishshield.domain.ThreatCategory;
import com.phishshield.domain.URLScan;
import com.phishshield.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface URLScanRepository extends JpaRepository<URLScan, Long> {
    Page<URLScan> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    List<URLScan> findTop10ByUserOrderByCreatedAtDesc(User user);
    List<URLScan> findTop10ByOrderByCreatedAtDesc();
    Optional<URLScan> findFirstByNormalizedUrlOrderByCreatedAtDesc(String normalizedUrl);

    long countByUser(User user);
    long countByUserAndRiskLevel(User user, RiskLevel riskLevel);
    long countByRiskLevel(RiskLevel riskLevel);
    long countByThreatCategory(ThreatCategory threatCategory);

    @Query("SELECT s.threatCategory, COUNT(s) FROM URLScan s GROUP BY s.threatCategory")
    List<Object[]> countGroupedByThreatCategory();

    @Query("SELECT s.riskLevel, COUNT(s) FROM URLScan s GROUP BY s.riskLevel")
    List<Object[]> countGroupedByRiskLevel();

    @Query("SELECT s.riskLevel, COUNT(s) FROM URLScan s WHERE s.user = :user GROUP BY s.riskLevel")
    List<Object[]> countGroupedByRiskLevelForUser(User user);

    @Query("SELECT FUNCTION('DATE', s.createdAt), COUNT(s) FROM URLScan s WHERE s.createdAt >= :startDate GROUP BY FUNCTION('DATE', s.createdAt) ORDER BY FUNCTION('DATE', s.createdAt) ASC")
    List<Object[]> countScansPerDaySince(LocalDateTime startDate);
}
