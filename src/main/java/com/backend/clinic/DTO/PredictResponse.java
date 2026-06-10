package com.backend.clinic.DTO;

import lombok.Data;

@Data
public class PredictResponse {
    private boolean success;
    private PredictionResult prediction;
    private String error;
    private Double inference_time_ms;
}
