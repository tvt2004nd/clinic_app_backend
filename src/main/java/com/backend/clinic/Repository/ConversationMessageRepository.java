package com.backend.clinic.Repository;

import com.backend.clinic.Entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    List<ConversationMessage> findByConversation_ConversationIdOrderByCreatedAtAsc(Long conversationId);
    Optional<ConversationMessage> findFirstByConversation_ConversationIdOrderByCreatedAtDesc(Long conversationId);
}
