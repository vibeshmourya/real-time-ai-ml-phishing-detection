package com.phishshield.repository;

import com.phishshield.domain.FinancialTransaction;
import com.phishshield.domain.TransactionCategory;
import com.phishshield.domain.TransactionDirection;
import com.phishshield.domain.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    Page<FinancialTransaction> findByUserId(Long userId, Pageable pageable);

    Optional<FinancialTransaction> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndDuplicateFingerprint(Long userId, String duplicateFingerprint);

    List<FinancialTransaction> findByUserIdAndTransactionDateTimeBetween(
            Long userId, LocalDateTime startDate, LocalDateTime endDate);

    Page<FinancialTransaction> findByUserIdAndStatus(Long userId, TransactionStatus status, Pageable pageable);

    Page<FinancialTransaction> findByUserIdAndCategory(Long userId, TransactionCategory category, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t WHERE t.user.id = :userId AND t.direction = :direction")
    BigDecimal sumAmountByUserIdAndDirection(@Param("userId") Long userId, @Param("direction") TransactionDirection direction);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t WHERE t.user.id = :userId AND t.direction = :direction AND t.transactionDateTime >= :since")
    BigDecimal sumAmountByUserIdAndDirectionSince(@Param("userId") Long userId, @Param("direction") TransactionDirection direction, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM FinancialTransaction t WHERE t.user.id = :userId AND t.status = 'FLAGGED_SUSPICIOUS'")
    long countFlaggedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM FinancialTransaction t WHERE t.user.id = :userId AND t.status = 'NEEDS_REVIEW'")
    long countNeedsReviewByUserId(@Param("userId") Long userId);

    @Query("SELECT t.category, COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t WHERE t.user.id = :userId AND t.direction = 'EXPENSE' GROUP BY t.category")
    List<Object[]> getCategoryExpenseBreakdownByUserId(@Param("userId") Long userId);

    List<FinancialTransaction> findTop10ByUserIdOrderByTransactionDateTimeDesc(Long userId);
}
