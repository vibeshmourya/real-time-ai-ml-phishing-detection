package com.phishshield.repository;

import com.phishshield.domain.ChatConversation;
import com.phishshield.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Page<ChatConversation> findByUserOrderByUpdatedAtDesc(User user, Pageable pageable);

    Optional<ChatConversation> findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);

    long countByUser(User user);
}
