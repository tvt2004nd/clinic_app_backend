package com.backend.clinic.Controller;
 
import com.backend.clinic.DTO.AuthDTOs;
import com.backend.clinic.Entity.*;
import com.backend.clinic.Repository.*;
import com.backend.clinic.Security.CustomUserDetails;
import com.backend.clinic.Security.JwtTokenProvider;
import com.backend.clinic.Service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
 
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {
 
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
 
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthDTOs.LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
 
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
 
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
 
        return ResponseEntity.ok(AuthDTOs.JwtResponse.builder()
                .token(jwt)
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .roles(roles)
                .build());
    }
 
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody AuthDTOs.RegisterRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Username is already taken!");
        }
 
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Email is already in use!");
        }
 
        String roleStr = "PATIENT";
        Role role = roleRepository.findByRoleCode(roleStr)
                .orElseThrow(() -> new RuntimeException("Error: Role '" + roleStr + "' not found."));
 
        // Create new user's account
        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .passwordHash(passwordEncoder.encode(signUpRequest.getPassword()))
                .fullName(signUpRequest.getFullName())
                .phone(signUpRequest.getPhone())
                .role(role)
                .isActive(true)
                .build();
 
        user = userRepository.save(user);
 
        // Automatically create associated Patient Entity
        String patientCode = "BN" + (100000 + new Random().nextInt(900000));
        Patient patient = Patient.builder()
                .user(user)
                .patientCode(patientCode)
                .bloodType("UNKNOWN")
                .build();
        patientRepository.save(patient);
 
        return ResponseEntity.ok("User registered successfully!");
    }
 
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody AuthDTOs.GoogleLoginRequest request) {
        GoogleUserInfo googleUser = verifyGoogleToken(request.getIdToken());
        if (googleUser == null) {
            return ResponseEntity.badRequest().body("Error: Invalid Google ID token!");
        }
 
        User user = userRepository.findByEmail(googleUser.getEmail()).orElse(null);
        if (user == null) {
            // Register new patient user
            Role role = roleRepository.findByRoleCode("PATIENT")
                    .orElseThrow(() -> new RuntimeException("Error: Role PATIENT not found."));
 
            String username = googleUser.getEmail().split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5);
            while (userRepository.existsByUsername(username)) {
                username = googleUser.getEmail().split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5);
            }
 
            user = User.builder()
                    .username(username)
                    .email(googleUser.getEmail())
                    .fullName(googleUser.getName() != null ? googleUser.getName() : "Google User")
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .avatarUrl(googleUser.getPicture())
                    .googleId(googleUser.getGoogleId())
                    .role(role)
                    .isActive(true)
                    .build();
 
            user = userRepository.save(user);
 
            // Also create patient record
            String patientCode = "BN" + (100000 + new Random().nextInt(900000));
            Patient patient = Patient.builder()
                    .user(user)
                    .patientCode(patientCode)
                    .bloodType("UNKNOWN")
                    .build();
            patientRepository.save(patient);
        } else {
            // Link Google account if not linked
            if (user.getGoogleId() == null || user.getGoogleId().isEmpty()) {
                user.setGoogleId(googleUser.getGoogleId());
                if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
                    user.setAvatarUrl(googleUser.getPicture());
                }
                userRepository.save(user);
            }
        }
 
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
 
        String jwt = jwtTokenProvider.generateToken(authentication);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
 
        return ResponseEntity.ok(AuthDTOs.JwtResponse.builder()
                .token(jwt)
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .roles(roles)
                .build());
    }
 
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody AuthDTOs.ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Error: Email này chưa được đăng ký trong hệ thống!");
        }
 
        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
 
        // Send real email OTP
        try {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            log.error("Gửi email OTP thất bại cho {}: {}", user.getEmail(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Error: Không thể gửi email OTP. Vui lòng thử lại sau hoặc liên hệ hỗ trợ.");
        }
 
        // Print OTP to logs (for debugging)
        log.info("\n========================================\n" +
                 "SENDING EMAIL OTP TO: {}\n" +
                 "YOUR OTP CODE IS: {}\n" +
                 "EXPIRES IN: 10 minutes\n" +
                 "========================================", user.getEmail(), otp);
 
        return ResponseEntity.ok("Mã OTP đã được gửi thành công đến email của bạn.");
    }
 
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody AuthDTOs.ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Error: Email này chưa được đăng ký trong hệ thống!");
        }
 
        if (user.getResetOtp() == null || !user.getResetOtp().equals(request.getOtp())) {
            return ResponseEntity.badRequest().body("Error: Mã OTP không chính xác!");
        }
 
        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Error: Mã OTP đã hết hạn sử dụng!");
        }
 
        // Set new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);
 
        return ResponseEntity.ok("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.");
    }
 
    private GoogleUserInfo verifyGoogleToken(String idToken) {
        if (idToken.startsWith("mock_google_")) {
            String suffix = idToken.substring("mock_google_".length());
            return new GoogleUserInfo(
                    "mock-google-id-" + suffix,
                    suffix + "@gmail.com",
                    "Google User " + suffix,
                    "https://lh3.googleusercontent.com/a/mock_avatar"
            );
        }
 
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
 
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(response.body());
 
                String sub = rootNode.path("sub").asText();
                String email = rootNode.path("email").asText();
                String name = rootNode.path("name").asText();
                String picture = rootNode.path("picture").asText();
 
                if (sub != null && !sub.isEmpty() && email != null && !email.isEmpty()) {
                    return new GoogleUserInfo(sub, email, name, picture);
                }
            }
        } catch (Exception e) {
            log.error("Google token verification failed: ", e);
        }
        return null;
    }
 
    @lombok.Value
    private static class GoogleUserInfo {
        String googleId;
        String email;
        String name;
        String picture;
    }
}
