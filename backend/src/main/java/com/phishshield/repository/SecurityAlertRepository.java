package com.phishshield.repository;

import com.phishshield.domain.SecurityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
    List<SecurityAlert> findByIsActiveTrueOrderByCreatedAtDesc();
    List<SecurityAlert> findTop5ByIsActiveTrueOrderByCreatedAtDesc();
}
