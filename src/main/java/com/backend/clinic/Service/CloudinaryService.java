package com.backend.clinic.Service;

import com.backend.clinic.Config.CloudinaryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final CloudinaryConfig config;
    private final RestTemplate restTemplate;

    public Map uploadPhoto(MultipartFile file, Long recordId) throws IOException {
        long timestamp = System.currentTimeMillis() / 1000;
        String publicId = "record_" + recordId + "_" + timestamp;

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("public_id", publicId);
        params.put("folder", config.getUploadFolder());
        params.put("timestamp", timestamp);

        String signature = sign(params);

        // Build multipart body manually
        String boundary = "Boundary-" + UUID.randomUUID();
        byte[] fileBytes = file.getBytes();
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg";

        StringBuilder textPart = new StringBuilder();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            textPart.append("--").append(boundary).append("\r\n");
            textPart.append("Content-Disposition: form-data; name=\"").append(e.getKey()).append("\"\r\n\r\n");
            textPart.append(e.getValue()).append("\r\n");
        }
        textPart.append("--").append(boundary).append("\r\n");
        textPart.append("Content-Disposition: form-data; name=\"signature\"\r\n\r\n").append(signature).append("\r\n");
        textPart.append("--").append(boundary).append("\r\n");
        textPart.append("Content-Disposition: form-data; name=\"api_key\"\r\n\r\n").append(config.getApiKey()).append("\r\n");
        textPart.append("--").append(boundary).append("\r\n");
        textPart.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        textPart.append("Content-Type: image/jpeg\r\n\r\n");

        byte[] textBytes = textPart.toString().getBytes("UTF-8");
        byte[] endBytes = ("\r\n--" + boundary + "--\r\n").getBytes("UTF-8");

        byte[] body = new byte[textBytes.length + fileBytes.length + endBytes.length];
        System.arraycopy(textBytes, 0, body, 0, textBytes.length);
        System.arraycopy(fileBytes, 0, body, textBytes.length, fileBytes.length);
        System.arraycopy(endBytes, 0, body, textBytes.length + fileBytes.length, endBytes.length);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("multipart/form-data; boundary=" + boundary));

        org.springframework.http.HttpEntity<byte[]> request = new org.springframework.http.HttpEntity<>(body, headers);
        String url = "https://api.cloudinary.com/v1_1/" + config.getCloudName() + "/image/upload";

        org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        return response.getBody();
    }

    public void deletePhoto(String publicId) throws IOException {
        long timestamp = System.currentTimeMillis() / 1000;
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("public_id", publicId);
        params.put("timestamp", timestamp);
        String signature = sign(params);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            body.append(e.getKey()).append("=").append(e.getValue()).append("&");
        }
        body.append("signature=").append(signature).append("&");
        body.append("api_key=").append(config.getApiKey());

        org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(body.toString(), headers);
        String url = "https://api.cloudinary.com/v1_1/" + config.getCloudName() + "/image/destroy";
        restTemplate.postForEntity(url, request, Map.class);
    }

    private String sign(Map<String, Object> params) {
        try {
            List<String> sortedKeys = new ArrayList<>(params.keySet());
            Collections.sort(sortedKeys);
            StringBuilder sb = new StringBuilder();
            for (String key : sortedKeys) {
                if (sb.length() > 0) sb.append("&");
                sb.append(key).append("=").append(params.get(key));
            }
            sb.append(config.getApiSecret());
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(sb.toString().getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary signature error", e);
        }
    }
}
