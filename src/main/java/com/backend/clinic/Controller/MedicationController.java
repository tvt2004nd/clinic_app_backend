package com.backend.clinic.Controller;

import com.backend.clinic.DTO.AdminDTOs;
import com.backend.clinic.Entity.MedCategory;
import com.backend.clinic.Entity.Medication;
import com.backend.clinic.Repository.MedCategoryRepository;
import com.backend.clinic.Repository.MedicationRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/medications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MedicationController {

    private final MedicationRepository medicationRepository;
    private final MedCategoryRepository medCategoryRepository;

    @GetMapping
    public ResponseEntity<AdminDTOs.PagedResponse<AdminDTOs.MedicationResponse>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("medName").ascending());
        Page<Medication> medPage;
        if (keyword != null && !keyword.isBlank()) {
            medPage = medicationRepository.searchMedications(keyword, false, pageable);
        } else {
            medPage = medicationRepository.findAll(pageable);
        }
        AdminDTOs.PagedResponse<AdminDTOs.MedicationResponse> response = AdminDTOs.PagedResponse.<AdminDTOs.MedicationResponse>builder()
                .content(medPage.getContent().stream().map(this::toResponse).toList())
                .page(medPage.getNumber())
                .size(medPage.getSize())
                .totalElements(medPage.getTotalElements())
                .totalPages(medPage.getTotalPages())
                .first(medPage.isFirst())
                .last(medPage.isLast())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminDTOs.MedicationResponse> getById(@PathVariable Integer id) {
        return medicationRepository.findById(id)
                .map(m -> ResponseEntity.ok(toResponse(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AdminDTOs.MedicationRequest request) {
        if (medicationRepository.findByMedCode(request.getMedCode()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Mã thuốc đã tồn tại: " + request.getMedCode()));
        }

        MedCategory category = null;
        if (request.getCategoryId() != null) {
            category = medCategoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        Medication medication = Medication.builder()
                .medCode(request.getMedCode().trim())
                .medName(request.getMedName().trim())
                .category(category)
                .activeIngredient(request.getActiveIngredient())
                .dosageForm(request.getDosageForm())
                .unit(request.getUnit())
                .imageUrl(request.getImageUrl())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .medicationType(request.getMedicationType())
                .build();

        medication = medicationRepository.save(medication);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(medication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id,
                                    @Valid @RequestBody AdminDTOs.MedicationRequest request) {
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc"));

        medicationRepository.findByMedCode(request.getMedCode()).ifPresent(existing -> {
            if (!existing.getMedicationId().equals(id)) {
                throw new RuntimeException("Mã thuốc đã tồn tại: " + request.getMedCode());
            }
        });

        MedCategory category = null;
        if (request.getCategoryId() != null) {
            category = medCategoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        medication.setMedCode(request.getMedCode().trim());
        medication.setMedName(request.getMedName().trim());
        medication.setCategory(category);
        medication.setActiveIngredient(request.getActiveIngredient());
        medication.setDosageForm(request.getDosageForm());
        medication.setUnit(request.getUnit());
        medication.setImageUrl(request.getImageUrl());
        medication.setPrice(request.getPrice());
        medication.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        medication.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        medication.setMedicationType(request.getMedicationType());

        medicationRepository.save(medication);
        return ResponseEntity.ok(toResponse(medication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        if (!medicationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        medicationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa thuốc"));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<AdminDTOs.CategoryResponse>> getCategories() {
        List<MedCategory> categories = medCategoryRepository.findAll();
        return ResponseEntity.ok(categories.stream().map(c -> AdminDTOs.CategoryResponse.builder()
                .categoryId(c.getCategoryId())
                .categoryCode(c.getCategoryCode())
                .categoryName(c.getCategoryName())
                .build()).toList());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File không được để trống"));
        }

        List<Map<String, String>> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "File CSV không có dữ liệu"));
            }
            // Strip BOM if present
            if (headerLine.charAt(0) == '\uFEFF') {
                headerLine = headerLine.substring(1);
            }

            String[] headers = parseCsvLine(headerLine);
            Map<String, Integer> colIndex = buildColIndex(headers);

            if (!colIndex.containsKey("medcode") || !colIndex.containsKey("medname")) {
                return ResponseEntity.badRequest().body(Map.of("message",
                        "File CSV phải có cột 'medCode' và 'medName'"));
            }

            if (!colIndex.containsKey("price")) {
                return ResponseEntity.badRequest().body(Map.of("message",
                        "File CSV phải có cột 'price'"));
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                totalRows++;
                String[] fields = parseCsvLine(line);

                try {
                    String medCode = getField(fields, colIndex, "medcode");
                    String medName = getField(fields, colIndex, "medname");
                    String priceStr = getField(fields, colIndex, "price");

                    List<String> rowErrors = new ArrayList<>();
                    if (medCode == null || medCode.isBlank()) rowErrors.add("Thiếu medCode");
                    if (medName == null || medName.isBlank()) rowErrors.add("Thiếu medName");
                    if (priceStr == null || priceStr.isBlank()) rowErrors.add("Thiếu price");

                    if (!rowErrors.isEmpty()) {
                        errors.add(Map.of(
                                "row", String.valueOf(lineNum),
                                "error", String.join("; ", rowErrors)
                        ));
                        continue;
                    }

                    BigDecimal price;
                    try {
                        price = new BigDecimal(priceStr.trim());
                        if (price.compareTo(BigDecimal.ZERO) < 0) {
                            errors.add(Map.of("row", String.valueOf(lineNum), "error", "Price phải >= 0"));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        errors.add(Map.of("row", String.valueOf(lineNum), "error", "Price không hợp lệ: " + priceStr));
                        continue;
                    }

                    if (medicationRepository.findByMedCode(medCode.trim()).isPresent()) {
                        errors.add(Map.of("row", String.valueOf(lineNum), "error", "Mã thuốc đã tồn tại: " + medCode));
                        continue;
                    }

                    String categoryName = getField(fields, colIndex, "categoryname");
                    MedCategory category = null;
                    if (categoryName != null && !categoryName.isBlank()) {
                        category = medCategoryRepository.findAll().stream()
                                .filter(c -> c.getCategoryName().equalsIgnoreCase(categoryName.trim()))
                                .findFirst().orElse(null);
                    }

                    String stockStr = getField(fields, colIndex, "stockquantity");
                    Integer stockQuantity = 0;
                    if (stockStr != null && !stockStr.isBlank()) {
                        try {
                            stockQuantity = Integer.parseInt(stockStr.trim());
                            if (stockQuantity < 0) stockQuantity = 0;
                        } catch (NumberFormatException ignored) {}
                    }

                    String isActiveStr = getField(fields, colIndex, "isactive");
                    boolean isActive = isActiveStr == null || isActiveStr.isBlank()
                            || "true".equalsIgnoreCase(isActiveStr.trim())
                            || "1".equals(isActiveStr.trim())
                            || "yes".equalsIgnoreCase(isActiveStr.trim());

                    Medication medication = Medication.builder()
                            .medCode(medCode.trim())
                            .medName(medName.trim())
                            .category(category)
                            .activeIngredient(trimToNull(getField(fields, colIndex, "activeingredient")))
                            .dosageForm(trimToNull(getField(fields, colIndex, "dosageform")))
                            .unit(trimToNull(getField(fields, colIndex, "unit")))
                            .imageUrl(trimToNull(getField(fields, colIndex, "imageurl")))
                            .price(price)
                            .stockQuantity(stockQuantity)
                            .isActive(isActive)
                            .medicationType(trimToNull(getField(fields, colIndex, "medicationtype")))
                            .build();

                    medicationRepository.save(medication);
                    successCount++;

                } catch (Exception e) {
                    errors.add(Map.of("row", String.valueOf(lineNum), "error", e.getMessage()));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi đọc file: " + e.getMessage()));
        }

        return ResponseEntity.ok(Map.of(
                "successCount", successCount,
                "errorCount", errors.size(),
                "totalRows", totalRows,
                "errors", errors
        ));
    }

    private String[] parseCsvLine(String line) {
        if (line.contains("\t")) {
            return line.split("\t", -1);
        }
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    private String getField(String[] fields, Map<String, Integer> colIndex, String colName) {
        Integer idx = colIndex.get(colName);
        if (idx == null || idx >= fields.length) return null;
        return fields[idx];
    }

    private String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private Map<String, Integer> buildColIndex(String[] headers) {
        Map<String, java.util.List<String>> aliasMap = Map.ofEntries(
                Map.entry("medcode", List.of("mã thuốc", "ma thuoc", "code", "mã", "ma", "med_code")),
                Map.entry("medname", List.of("tên thuốc", "ten thuoc", "name", "tên", "ten", "med_name")),
                Map.entry("price", List.of("giá", "gia", "giá bán", "gia ban")),
                Map.entry("categoryname", List.of("danh mục", "danh muc", "category", "nhóm", "nhom")),
                Map.entry("activeingredient", List.of("hoạt chất", "hoat chat", "hoatchat", "active ingredient", "ingredient", "active_ingredient")),
                Map.entry("dosageform", List.of("dạng bào chế", "dang bao che", "dangbaoche", "dạng", "dang", "dosage_form")),
                Map.entry("unit", List.of("đơn vị", "don vi", "donvi", "đvt")),
                Map.entry("stockquantity", List.of("tồn kho", "ton kho", "tonkho", "số lượng", "so luong", "quantity", "stock", "stock_quantity")),
                Map.entry("isactive", List.of("kích hoạt", "kich hoat", "kichhoat", "trạng thái", "trang thai", "active", "status", "is_active")),
                Map.entry("imageurl", List.of("image_url", "image url", "image", "hình ảnh", "hinh anh", "hinhanh", "url")),
                Map.entry("medicationtype", List.of("medication_type", "medication type", "loại thuốc", "loai thuoc", "type", "drug type"))
        );

        Map<String, Integer> colIndex = new java.util.HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String raw = headers[i].trim().toLowerCase()
                    .replaceAll("[\\s\\-–—/]+", " ").trim();
            colIndex.put(raw, i);
            for (var entry : aliasMap.entrySet()) {
                if (entry.getValue().contains(raw)) {
                    colIndex.putIfAbsent(entry.getKey(), i);
                }
            }
        }
        return colIndex;
    }

    private AdminDTOs.MedicationResponse toResponse(Medication m) {
        return AdminDTOs.MedicationResponse.builder()
                .medicationId(m.getMedicationId())
                .medCode(m.getMedCode())
                .medName(m.getMedName())
                .categoryId(m.getCategory() != null ? m.getCategory().getCategoryId() : null)
                .categoryName(m.getCategory() != null ? m.getCategory().getCategoryName() : null)
                .activeIngredient(m.getActiveIngredient())
                .dosageForm(m.getDosageForm())
                .unit(m.getUnit())
                .imageUrl(m.getImageUrl())
                .price(m.getPrice())
                .stockQuantity(m.getStockQuantity())
                .isActive(m.getIsActive())
                .medicationType(m.getMedicationType())
                .build();
    }
}
