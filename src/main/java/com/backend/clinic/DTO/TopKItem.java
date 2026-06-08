package com.backend.clinic.DTO;

import lombok.Data;

@Data
public class TopKItem {
    private String class_name;
    private double probability;
}
