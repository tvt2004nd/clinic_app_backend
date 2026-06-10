package com.backend.clinic.Controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.clinic.DTO.ChatDTOs.ChatRequest;
import com.backend.clinic.DTO.ChatDTOs.ChatResponse;
import com.backend.clinic.Entity.Appointment;
import com.backend.clinic.Entity.Conversation;
import com.backend.clinic.Entity.ConversationMessage;
import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.Patient;
import com.backend.clinic.Entity.User;
import com.backend.clinic.Repository.AppointmentRepository;
import com.backend.clinic.Repository.ConversationMessageRepository;
import com.backend.clinic.Repository.ConversationRepository;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.PatientRepository;
import com.backend.clinic.Repository.UserRepository;
import com.backend.clinic.Service.GeminiService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final GeminiService geminiService;
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

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendAiMessage(@RequestBody ChatRequest request, Authentication authentication) {
        String username = authentication.getName();
        ChatResponse response = geminiService.processChat(request, username);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessagePayload payload) {
        Conversation conversation = conversationRepository.findById(payload.getConversationId()).orElse(null);
        User sender = userRepository.findById(payload.getSenderId()).orElse(null);

        if (conversation == null || sender == null) {
            return;
        }

        ConversationMessage message = ConversationMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(payload.getContent())
                .build();

        messageRepository.save(message);
        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversation.getConversationId(),
                (Object) toMessageResponse(message)
        );
    }

    @Transactional
    @GetMapping("/conversation")
    public ResponseEntity<?> getOrCreateConversation(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            Authentication auth) {

        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow();

        if (doctorId == null) {
            Doctor doctor = doctorRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);
            if (doctor != null) {
                doctorId = doctor.getDoctorId();
            }
        }
        if (patientId == null) {
            Patient patient = patientRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);
            if (patient != null) {
                patientId = patient.getPatientId();
            }
        }

        if (doctorId == null || patientId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing doctorId or patientId"));
        }

        if (!appointmentRepository.existsByDoctor_DoctorIdAndPatient_PatientIdAndStatusNotIn(doctorId, patientId,
                List.of("CANCELLED", "NO_SHOW"))) {
            return ResponseEntity.status(403).body(Map.of("error", "Ban can dat lich kham truoc khi chat voi bac si nay"));
        }

        Long finalDoctorId = doctorId;
        Long finalPatientId = patientId;

        Conversation conversation = conversationRepository.findByDoctor_DoctorIdAndPatient_PatientId(doctorId, patientId)
                .orElseGet(() -> {
                    Doctor doctor = doctorRepository.findById(finalDoctorId).orElseThrow();
                    Patient patient = patientRepository.findById(finalPatientId).orElseThrow();
                    return conversationRepository.save(Conversation.builder()
                            .doctor(doctor)
                            .patient(patient)
                            .build());
                });

        return ResponseEntity.ok(toConversationResponse(conversation));
    }

    @Transactional
    @PostMapping("/send")
    public ResponseEntity<?> sendMessageRest(@RequestBody ChatMessagePayload payload) {
        Conversation conversation = conversationRepository.findById(payload.getConversationId()).orElse(null);
        User sender = userRepository.findById(payload.getSenderId()).orElse(null);

        if (conversation == null || sender == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Conversation or sender not found"));
        }

        ConversationMessage message = ConversationMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(payload.getContent())
                .build();

        messageRepository.save(message);
        Map<String, Object> response = toMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/conversation/" + conversation.getConversationId(), (Object) response);

        return ResponseEntity.ok(response);
    }

    @Transactional
    @GetMapping("/conversations")
    public ResponseEntity<?> getMyConversations(Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElseThrow();
        Doctor doctor = doctorRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);
        Patient patient = patientRepository.findByUser_UserId(currentUser.getUserId()).orElse(null);

        List<Appointment> appointments;
        if (doctor != null) {
            appointments = appointmentRepository.findByDoctor_DoctorIdAndStatusNotIn(doctor.getDoctorId(),
                    List.of("CANCELLED", "NO_SHOW"));
        } else if (patient != null) {
            appointments = appointmentRepository.findByPatient_PatientIdAndStatusNotIn(patient.getPatientId(),
                    List.of("CANCELLED", "NO_SHOW"));
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
                .map(conversation -> {
                    Map<String, Object> response = toConversationResponse(conversation);
                    messageRepository.findFirstByConversation_ConversationIdOrderByCreatedAtDesc(conversation.getConversationId())
                            .ifPresent(last -> {
                                response.put("lastMessage", last.getContent());
                                response.put("lastMessageTime", last.getCreatedAt().toString());
                            });
                    return response;
                })
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    @GetMapping("/conversation/{conversationId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long conversationId) {
        List<ConversationMessage> messages = messageRepository.findByConversation_ConversationIdOrderByCreatedAtAsc(conversationId);

        return ResponseEntity.ok(messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList()));
    }

    private Map<String, Object> toConversationResponse(Conversation conversation) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("conversationId", conversation.getConversationId());
        response.put("doctorId", conversation.getDoctor().getDoctorId());
        response.put("patientId", conversation.getPatient().getPatientId());
        response.put("doctorName", conversation.getDoctor().getUser().getFullName());
        response.put("patientName", conversation.getPatient().getUser().getFullName());
                response.put("doctorAvatar", conversation.getDoctor().getUser().getAvatarUrl());
        response.put("patientAvatar", conversation.getPatient().getUser().getAvatarUrl());
        return response;
    }

    private Map<String, Object> toMessageResponse(ConversationMessage message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("messageId", message.getMessageId());
        response.put("conversationId", message.getConversation().getConversationId());
        response.put("senderId", message.getSender().getUserId());
        response.put("senderName", message.getSender().getFullName());
        response.put("content", message.getContent());
        response.put("createdAt", message.getCreatedAt().toString());
        response.put("avatarUrl", message.getSender().getAvatarUrl());
        return response;
    }
}
