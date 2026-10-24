package com.phishshield.repository;

import com.phishshield.domain.ChatConversation;
import com.phishshield.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationOrderByCreatedAtAsc(ChatConversation conversation, Pageable pageable);

    List<ChatMessage> findTop20ByConversationOrderByCreatedAtAsc(ChatConversation conversation);

    long countByConversation(ChatConversation conversation);
}
