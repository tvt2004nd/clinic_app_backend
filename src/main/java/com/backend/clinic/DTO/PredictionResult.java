package com.backend.clinic.DTO;

import lombok.Data;
import java.util.List;

@Data
public class PredictionResult {
    private String predicted_class;
    private double confidence;
    private List<TopKItem> top_k;
}
