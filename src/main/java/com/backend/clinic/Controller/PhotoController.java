package com.backend.clinic.Controller;

import com.backend.clinic.Entity.ExamPhoto;
import com.backend.clinic.Repository.ExamPhotoRepository;
import com.backend.clinic.Service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private static final Logger log = LoggerFactory.getLogger(PhotoController.class);
    private final CloudinaryService cloudinaryService;
    private final ExamPhotoRepository examPhotoRepository;

    @PostMapping("/{recordId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> uploadPhotos(@PathVariable Long recordId,
                                          @RequestParam("files") List<MultipartFile> files) {
        log.info("uploadPhotos: recordId={}, files={}", recordId, files.size());
        try {
            int sortOrder = examPhotoRepository.findByRecordIdOrderBySortOrderAsc(recordId).size();
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    log.warn("Empty file skipped");
                    continue;
                }
                log.info("Uploading file: size={} bytes", file.getSize());
                Map result = cloudinaryService.uploadPhoto(file, recordId);
                log.info("Cloudinary upload result: {}", result);
                ExamPhoto photo = ExamPhoto.builder()
                        .recordId(recordId)
                        .imageUrl((String) result.get("secure_url"))
                        .publicId((String) result.get("public_id"))
                        .sortOrder(sortOrder++)
                        .build();
                examPhotoRepository.save(photo);
                log.info("Saved ExamPhoto: {}", photo.getImageUrl());
            }
            return ResponseEntity.ok(Map.of("message", "Upload thành công"));
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<?> getPhotos(@PathVariable Long recordId) {
        List<ExamPhoto> photos = examPhotoRepository.findByRecordIdOrderBySortOrderAsc(recordId);
        List<Map<String, Object>> result = photos.stream()
                .map(p -> Map.<String, Object>of(
                        "photoId", p.getPhotoId(),
                        "imageUrl", p.getImageUrl(),
                        "sortOrder", p.getSortOrder()
                )).toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{photoId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> deletePhoto(@PathVariable Long photoId) {
        try {
            ExamPhoto photo = examPhotoRepository.findById(photoId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));
            cloudinaryService.deletePhoto(photo.getPublicId());
            examPhotoRepository.delete(photo);
            return ResponseEntity.ok(Map.of("message", "Xóa ảnh thành công"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }
}
