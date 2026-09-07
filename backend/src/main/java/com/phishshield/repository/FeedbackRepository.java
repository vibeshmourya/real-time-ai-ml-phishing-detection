package com.phishshield.repository;

import com.phishshield.domain.Feedback;
import com.phishshield.domain.URLScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByScan(URLScan scan);
    List<Feedback> findByScanId(Long scanId);
}
