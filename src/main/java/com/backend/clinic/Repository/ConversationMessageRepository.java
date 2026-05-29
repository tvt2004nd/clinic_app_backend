package com.backend.clinic.Repository;

import com.backend.clinic.Entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    List<ConversationMessage> findByConversation_ConversationIdOrderByCreatedAtAsc(Long conversationId);
}
