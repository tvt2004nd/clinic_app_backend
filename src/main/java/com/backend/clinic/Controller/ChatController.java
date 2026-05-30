package com.backend.clinic.Controller;

import com.backend.clinic.Entity.*;
import com.backend.clinic.Repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    @Data
    public static class ChatMessagePayload {
        private Long conversationId;
        private Long senderId;
        private String content;
    }

    // 1. WebSocket Endpoint for sending messages
    @Transactional
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessagePayload payload) {
        Conversation conv = conversationRepository.findById(payload.getConversationId()).orElse(null);
        User sender = userRepository.findById(payload.getSenderId()).orElse(null);

        if (conv != null && sender != null) {
            ConversationMessage message = ConversationMessage.builder()
                    .conversation(conv)
                    .sender(sender)
                    .content(payload.getContent())
                    .build();

            messageRepository.save(message);

            // Broadcast to the conversation topic
            String destination = "/topic/conversation/" + conv.getConversationId();
            Object payloadObj = Map.<String, Object>of(
                    "messageId", message.getMessageId(),
                    "conversationId", conv.getConversationId(),
                    "senderId", sender.getUserId(),
                    "senderName", sender.getFullName(),
                    "content", message.getContent(),
                    "createdAt", message.getCreatedAt().toString()
            );
            messagingTemplate.convertAndSend(destination, payloadObj);
        }
    }

    // 2. REST Endpoint to get or create a conversation
    @Transactional
    @GetMapping("/api/chat/conversation")
    public ResponseEntity<?> getOrCreateConversation(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            Authentication auth) {

        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow();
        
        // If one of the IDs is missing, infer it from the logged in user
        if (doctorId == null) {
            Doctor d = doctorRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);
            if (d != null) doctorId = d.getDoctorId();
        }
        if (patientId == null) {
            Patient p = patientRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);
            if (p != null) patientId = p.getPatientId();
        }

        if (doctorId == null || patientId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing doctorId or patientId"));
        }

        if (!appointmentRepository.existsByDoctor_DoctorIdAndPatient_PatientIdAndStatus(doctorId, patientId, "CONFIRMED")) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ có thể chat sau khi lịch hẹn được bác sĩ xác nhận"));
        }

        Long finalDoctorId = doctorId;
        Long finalPatientId = patientId;

        Conversation conversation = conversationRepository.findByDoctor_DoctorIdAndPatient_PatientId(doctorId, patientId)
                .orElseGet(() -> {
                    Doctor d = doctorRepository.findById(finalDoctorId).orElseThrow();
                    Patient p = patientRepository.findById(finalPatientId).orElseThrow();
                    Conversation newConv = Conversation.builder()
                            .doctor(d)
                            .patient(p)
                            .build();
                    return conversationRepository.save(newConv);
                });

        return ResponseEntity.ok(Map.<String, Object>of(
                "conversationId", conversation.getConversationId(),
                "doctorId", conversation.getDoctor().getDoctorId(),
                "patientId", conversation.getPatient().getPatientId(),
                "doctorName", conversation.getDoctor().getUser().getFullName(),
                "patientName", conversation.getPatient().getUser().getFullName()
        ));
    }

    @Transactional
    @PostMapping("/api/chat/send")
    public ResponseEntity<?> sendMessageRest(@RequestBody ChatMessagePayload payload) {
        Conversation conv = conversationRepository.findById(payload.getConversationId()).orElse(null);
        User sender = userRepository.findById(payload.getSenderId()).orElse(null);

        if (conv == null || sender == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Conversation or sender not found"));
        }

        ConversationMessage message = ConversationMessage.builder()
                .conversation(conv)
                .sender(sender)
                .content(payload.getContent())
                .build();

        messageRepository.save(message);

        String destination = "/topic/conversation/" + conv.getConversationId();
        Object broadcastPayload = Map.<String, Object>of(
                "messageId", message.getMessageId(),
                "conversationId", conv.getConversationId(),
                "senderId", sender.getUserId(),
                "senderName", sender.getFullName(),
                "content", message.getContent(),
                "createdAt", message.getCreatedAt().toString()
        );
        messagingTemplate.convertAndSend(destination, broadcastPayload);

        return ResponseEntity.ok(Map.<String, Object>of(
                "messageId", message.getMessageId(),
                "conversationId", conv.getConversationId(),
                "senderId", sender.getUserId(),
                "senderName", sender.getFullName(),
                "content", message.getContent(),
                "createdAt", message.getCreatedAt().toString()
        ));
    }

    @GetMapping("/api/chat/conversations")
    @Transactional
    public ResponseEntity<?> getMyConversations(Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow();
        Doctor doctor = doctorRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);
        Patient patient = patientRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);

        List<Appointment> appointments;
        if (doctor != null) {
            appointments = appointmentRepository.findByDoctor_DoctorIdAndStatus(doctor.getDoctorId(), "CONFIRMED");
        } else if (patient != null) {
            appointments = appointmentRepository.findByPatient_PatientIdAndStatus(patient.getPatientId(), "CONFIRMED");
        } else {
            return ResponseEntity.ok(List.of());
        }

        Map<String, Conversation> conversations = new LinkedHashMap<>();
        for (Appointment appointment : appointments) {
            Long doctorId = appointment.getDoctor().getDoctorId();
            Long patientId = appointment.getPatient().getPatientId();
            String key = doctorId + ":" + patientId;
            conversations.putIfAbsent(key, conversationRepository.findByDoctor_DoctorIdAndPatient_PatientId(doctorId, patientId)
                    .orElseGet(() -> conversationRepository.save(Conversation.builder()
                            .doctor(appointment.getDoctor())
                            .patient(appointment.getPatient())
                            .build())));
        }

        return ResponseEntity.ok(conversations.values().stream()
                .map(c -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("conversationId", c.getConversationId());
                    map.put("doctorId", c.getDoctor().getDoctorId());
                    map.put("patientId", c.getPatient().getPatientId());
                    map.put("doctorName", c.getDoctor().getUser().getFullName());
                    map.put("patientName", c.getPatient().getUser().getFullName());

                    messageRepository.findFirstByConversation_ConversationIdOrderByCreatedAtDesc(c.getConversationId())
                            .ifPresent(last -> {
                                map.put("lastMessage", last.getContent());
                                map.put("lastMessageTime", last.getCreatedAt().toString());
                            });

                    return map;
                })
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    @GetMapping("/api/chat/conversation/{conversationId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long conversationId) {
        List<ConversationMessage> messages = messageRepository.findByConversation_ConversationIdOrderByCreatedAtAsc(conversationId);
        
        List<Map<String, Object>> response = messages.stream().map(m -> Map.<String, Object>of(
                "messageId", m.getMessageId(),
                "conversationId", m.getConversation().getConversationId(),
                "senderId", m.getSender().getUserId(),
                "senderName", m.getSender().getFullName(),
                "content", m.getContent(),
                "createdAt", m.getCreatedAt().toString()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
