package com.phishshield.repository;

import com.phishshield.domain.ModelPrediction;
import com.phishshield.domain.URLScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModelPredictionRepository extends JpaRepository<ModelPrediction, Long> {
    Optional<ModelPrediction> findByScan(URLScan scan);
    Optional<ModelPrediction> findByScanId(Long scanId);
}
