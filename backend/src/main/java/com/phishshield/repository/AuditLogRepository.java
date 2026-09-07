package com.phishshield.repository;

import com.phishshield.domain.AuditLog;
import com.phishshield.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<AuditLog> findTop10ByOrderByCreatedAtDesc();
}
