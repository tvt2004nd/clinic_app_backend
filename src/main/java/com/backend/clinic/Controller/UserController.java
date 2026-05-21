package com.backend.clinic.Controller;
 
import com.backend.clinic.DTO.AuthDTOs;
import com.backend.clinic.Entity.User;
import com.backend.clinic.Repository.UserRepository;
import com.backend.clinic.Security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

 
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
 
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
 
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
            // Create uploads directory if not exists
            java.io.File uploadDir = new java.io.File("uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Save file
            String extension = "";
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                extension = ".jpg"; // fallback
            }

            String filename = "avatar_" + userDetails.getUserId() + "_" + System.currentTimeMillis() + extension;
            java.io.File destinationFile = new java.io.File(uploadDir, filename);
            file.transferTo(destinationFile);

            String avatarUrl = "/uploads/" + filename;

            // Update user record
            User user = userRepository.findById(userDetails.getUserId())
                    .orElseThrow(() -> new RuntimeException("Error: User not found"));
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            return ResponseEntity.ok(java.util.Map.of("avatarUrl", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error uploading file: " + e.getMessage());
        }
    }
}
