package com.phishshield.repository;

import com.phishshield.domain.ReportStatus;
import com.phishshield.domain.User;
import com.phishshield.domain.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    Page<UserReport> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<UserReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
    List<UserReport> findTop10ByOrderByCreatedAtDesc();
    long countByStatus(ReportStatus status);
}
