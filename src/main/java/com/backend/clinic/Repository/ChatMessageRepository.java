package com.backend.clinic.Repository;

import com.backend.clinic.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionUuidOrderByCreatedAtAsc(String sessionUuid);
    List<ChatMessage> findByUser_UserId(Long userId);
}
