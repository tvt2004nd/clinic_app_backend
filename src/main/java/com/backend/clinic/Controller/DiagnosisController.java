package com.backend.clinic.Controller;

import com.backend.clinic.DTO.PredictResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/diagnosis")
@CrossOrigin(origins = "*") // Adjust in production
public class DiagnosisController {

    @Autowired
    private RestTemplate restTemplate;

    // FastAPI service URL
    private final String AI_SERVICE_URL = "http://localhost:8000/predict";

    @PostMapping("/predict")
    public ResponseEntity<PredictResponse> predictDisease(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "top_k", defaultValue = "5") int topK) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            body.add("top_k", topK);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<PredictResponse> response = restTemplate.postForEntity(
                    AI_SERVICE_URL,
                    requestEntity,
                    PredictResponse.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            PredictResponse errorResponse = new PredictResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("Error connecting to AI service: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
