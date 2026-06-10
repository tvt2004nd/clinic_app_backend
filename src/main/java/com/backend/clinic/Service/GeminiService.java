package com.backend.clinic.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.clinic.DTO.ChatDTOs.ChatRequest;
import com.backend.clinic.DTO.ChatDTOs.ChatResponse;
import com.backend.clinic.DTO.GeminiDTOs.*;
import com.backend.clinic.DTO.GeminiDTOs.Content;
import com.backend.clinic.DTO.GeminiDTOs.GeminiRequest;
import com.backend.clinic.DTO.GeminiDTOs.GeminiResponse;
import com.backend.clinic.DTO.GeminiDTOs.Part;
import com.backend.clinic.Entity.AiDiagnosis;
import com.backend.clinic.Entity.ChatMessage;
import com.backend.clinic.Entity.MedicalRecord;
import com.backend.clinic.Entity.Patient;
import com.backend.clinic.Entity.User;
import com.backend.clinic.Repository.AiDiagnosisRepository;
import com.backend.clinic.Repository.ChatMessageRepository;
import com.backend.clinic.Repository.MedicalRecordRepository;
import com.backend.clinic.Repository.PatientRepository;
import com.backend.clinic.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeminiService {

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private final RestTemplate restTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final PatientRepository patientRepository;
    private final AiDiagnosisRepository aiDiagnosisRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;

    public GeminiService(RestTemplate restTemplate,
            ChatMessageRepository chatMessageRepository,
            PatientRepository patientRepository,
            AiDiagnosisRepository aiDiagnosisRepository,
            MedicalRecordRepository medicalRecordRepository,
            UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.chatMessageRepository = chatMessageRepository;
        this.patientRepository = patientRepository;
        this.aiDiagnosisRepository = aiDiagnosisRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.userRepository = userRepository;
    }

    public ChatResponse processChat(ChatRequest request, String username) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            // Gemini API key not configured — return a friendly error and avoid calling
            // external API
            String msg = "AI service is not configured. Please set the GEMINI_API_KEY environment variable to enable AI chat.";
            log.warn("Gemini API key is missing — incoming chat will not be processed by Gemini.");
            return new ChatResponse(msg, "ERROR");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save USER message to DB
        ChatMessage userMessage = ChatMessage.builder()
                .user(user)
                .sessionUuid(request.getSessionUuid())
                .sender("USER")
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMessage);

        // Retrieve Patient Data for Context (RAG)
        String patientContext = buildPatientContext(user);

        // Build Gemini Request
        GeminiRequest geminiReq = new GeminiRequest();
        List<Content> contents = new ArrayList<>();

        // System Instruction + Context
        String systemInstruction = "Bạn là Trợ lý AI y tế chuyên về Da Liễu của phòng khám DermaCare. " +
                "Nhiệm vụ của bạn là tư vấn, giải thích kết quả chẩn đoán bệnh qua ảnh và đưa ra phương án xử lý sơ bộ. "
                +
                "Tuyệt đối không kê đơn thuốc cụ thể (như liều lượng kháng sinh), luôn khuyên bệnh nhân đến gặp bác sĩ để xác nhận. "
                +
                "Dưới đây là thông tin bệnh án và kết quả AI chẩn đoán mới nhất của bệnh nhân (nếu có):\n"
                + patientContext;

        contents.add(new Content("user", java.util.Arrays.asList(new Part(systemInstruction))));
        contents.add(new Content("model", java.util.Arrays.asList(new Part(
                "Vâng, tôi đã hiểu. Tôi sẽ đóng vai bác sĩ da liễu DermaCare và hỗ trợ bệnh nhân dựa trên dữ liệu này."))));

        // Append recent chat history (last 5 messages)
        List<ChatMessage> history = chatMessageRepository
                .findTop5BySessionUuidOrderByCreatedAtDesc(request.getSessionUuid());
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            // Skip the one we just saved
            if (msg.getMessageId().equals(userMessage.getMessageId()))
                continue;
            String role = msg.getSender().equals("USER") ? "user" : "model";
            contents.add(new Content(role, java.util.Arrays.asList(new Part(msg.getContent()))));
        }

        // Add the current user message
        contents.add(new Content("user", java.util.Arrays.asList(new Part(request.getMessage()))));
        geminiReq.setContents(contents);

        // Call Gemini API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(geminiReq, headers);

        try {
            GeminiResponse geminiRes = restTemplate.postForObject(GEMINI_URL + geminiApiKey, entity,
                    GeminiResponse.class);

            String replyText = "Xin lỗi, tôi không thể trả lời lúc này.";
            if (geminiRes != null && geminiRes.getCandidates() != null && !geminiRes.getCandidates().isEmpty()) {
                replyText = geminiRes.getCandidates().get(0).getContent().getParts().get(0).getText();
            }

            // Save BOT message to DB
            ChatMessage botMessage = ChatMessage.builder()
                    .user(user)
                    .sessionUuid(request.getSessionUuid())
                    .sender("BOT")
                    .content(replyText)
                    .intent("ADVICE")
                    .build();
            chatMessageRepository.save(botMessage);

            return new ChatResponse(replyText, "ADVICE");

        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse("Đã có lỗi xảy ra khi kết nối tới hệ thống AI. Vui lòng thử lại sau.", "ERROR");
        }
    }

    private String buildPatientContext(User user) {
        StringBuilder context = new StringBuilder();

        Patient patient = patientRepository.findByUser(user).orElse(null);
        if (patient == null)
            return "Không có dữ liệu bệnh nhân.";

        context.append("- Tiền sử bệnh: ")
                .append(patient.getMedicalHistory() != null ? patient.getMedicalHistory() : "Không có").append("\n");

        // Get latest AI Diagnosis
        AiDiagnosis latestAi = aiDiagnosisRepository.findTop1ByPatientOrderByCreatedAtDesc(patient).orElse(null);
        if (latestAi != null) {
            context.append("- Kết quả AI phân tích ảnh gần nhất (").append(latestAi.getCreatedAt().toString())
                    .append("):\n");
            context.append("  + Bệnh dự đoán 1: ")
                    .append(latestAi.getTop1Disease() != null ? latestAi.getTop1Disease().getDiseaseNameVi() : "N/A")
                    .append(" (Tỷ lệ: ").append(latestAi.getTop1Confidence()).append(")\n");
            context.append("  + Cảnh báo ác tính: ").append(latestAi.getHasCancerWarning() ? "CÓ" : "KHÔNG")
                    .append("\n");
        }

        // Get latest Medical Record
        MedicalRecord latestRecord = medicalRecordRepository.findTop1ByPatientOrderByExaminedAtDesc(patient)
                .orElse(null);
        if (latestRecord != null) {
            context.append("- Bệnh án khám gần nhất (").append(latestRecord.getExaminedAt().toString()).append("):\n");
            context.append("  + Triệu chứng: ").append(latestRecord.getSymptoms()).append("\n");
            context.append("  + Chẩn đoán cuối cùng: ").append(latestRecord.getFinalDiagnosis()).append("\n");
            context.append("  + Hướng điều trị: ").append(latestRecord.getTreatmentPlan()).append("\n");
        }

        if (latestAi == null && latestRecord == null) {
            context.append("Bệnh nhân chưa có dữ liệu khám bệnh hay chụp ảnh da liễu nào trên hệ thống.");
        }

        return context.toString();
    }
}
