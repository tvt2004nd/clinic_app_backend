package com.backend.clinic.Controller;
 
import com.backend.clinic.DTO.AuthDTOs;
import com.backend.clinic.Entity.User;
import com.backend.clinic.Repository.UserRepository;
import com.backend.clinic.Security.CustomUserDetails;
import com.backend.clinic.Service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

 
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
 
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
 
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("Error: User not found with ID: " + userDetails.getUserId()));
 
        return ResponseEntity.ok(AuthDTOs.UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().getRoleCode())
                .build());
    }
 
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AuthDTOs.ProfileUpdateRequest request
    ) {
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("Error: User not found with ID: " + userDetails.getUserId()));
 
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());
        user.setAvatarUrl(request.getAvatarUrl());
 
        userRepository.save(user);
 
        return ResponseEntity.ok(AuthDTOs.UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().getRoleCode())
                .build());
    }
 
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AuthDTOs.ChangePasswordRequest request
    ) {
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("Error: User not found with ID: " + userDetails.getUserId()));
 
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body("Error: Mật khẩu hiện tại không chính xác!");
        }
 
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
 
        return ResponseEntity.ok("Đổi mật khẩu thành công!");
    }

    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: File is empty!");
        }

        try {
            Map result = cloudinaryService.uploadAvatar(file, userDetails.getUserId());
            String avatarUrl = (String) result.get("secure_url");

            if (avatarUrl == null || avatarUrl.isBlank()) {
                return ResponseEntity.internalServerError().body("Error uploading file: Cloudinary did not return secure_url");
            }

            User user = userRepository.findById(userDetails.getUserId())
                    .orElseThrow(() -> new RuntimeException("Error: User not found"));
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("avatarUrl", avatarUrl);
            response.put("publicId", result.get("public_id"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error uploading file: " + e.getMessage());
        }
    }
}
