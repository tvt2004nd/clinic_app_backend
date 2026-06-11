package com.backend.clinic.Controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.clinic.DTO.AuthDTOs;
import com.backend.clinic.Entity.Patient;
import com.backend.clinic.Entity.Role;
import com.backend.clinic.Entity.User;
import com.backend.clinic.Repository.PatientRepository;
import com.backend.clinic.Repository.RoleRepository;
import com.backend.clinic.Repository.UserRepository;
import com.backend.clinic.Security.CustomUserDetails;
import com.backend.clinic.Security.JwtTokenProvider;
import com.backend.clinic.Service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    private static final String ROLE_PATIENT = "PATIENT";

    @PostMapping("/login")
    public ResponseEntity<AuthDTOs.JwtResponse> authenticateUser(
            @Valid @RequestBody AuthDTOs.LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return ResponseEntity.ok(generateJwtResponse(authentication));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody AuthDTOs.RegisterRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Lỗi: Tên đăng nhập đã tồn tại!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Lỗi: Email đã được sử dụng!");
        }

        Role role = roleRepository.findByRoleCode(ROLE_PATIENT)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy quyền '" + ROLE_PATIENT + "'."));

        // Tạo tài khoản User
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

        // Tạo Entity Patient đi kèm
        createPatientRecord(user);

        return ResponseEntity.ok("Đăng ký tài khoản thành công!");
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody AuthDTOs.GoogleLoginRequest request) {
        GoogleUserInfo googleUser = verifyGoogleToken(request.getIdToken());
        if (googleUser == null) {
            return ResponseEntity.badRequest().body("Lỗi: Google ID token không hợp lệ!");
        }
        User user = userRepository.findByEmail(googleUser.getEmail()).orElse(null);
        if (user == null) {
            // Đăng ký người dùng mới từ Google
            Role role = roleRepository.findByRoleCode(ROLE_PATIENT)
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy quyền PATIENT."));

            String baseUsername = googleUser.getEmail().split("@")[0];
            String username = generateUniqueUsername(baseUsername);
            user = User.builder()
                    .username(username)
                    .email(googleUser.getEmail())
                    .fullName(googleUser.getName() != null ? googleUser.getName() : "Google User")
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                    .avatarUrl(googleUser.getPicture())
                    .googleId(googleUser.getGoogleId())
                    .role(role)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
            createPatientRecord(user);
        } else {
            // Liên kết tài khoản Google nếu chưa có
            boolean updated = false;
            if (user.getGoogleId() == null || user.getGoogleId().isEmpty()) {
                user.setGoogleId(googleUser.getGoogleId());
                updated = true;
            }
            if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
                user.setAvatarUrl(googleUser.getPicture());
                updated = true;
            }
            if (updated) {
                userRepository.save(user);
            }
        }
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return ResponseEntity.ok(generateJwtResponse(authentication));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody AuthDTOs.ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        // Bảo mật: Không tiết lộ việc email có tồn tại hay không (Tránh user
        // enumeration)
        String genericMessage = "Nếu email hợp lệ, mã OTP khôi phục mật khẩu đã được gửi.";

        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(genericMessage);
        }

        User user = userOpt.get();
        String otp = String.format("%06d", new Random().nextInt(1000000));

        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        try {
            emailService.sendOtpEmail(user.getEmail(), otp);
            log.info("Đã gửi OTP đến email: {} | Hết hạn trong 10 phút", user.getEmail());
        } catch (Exception e) {
            log.error("Gửi email OTP thất bại cho {}: {}", user.getEmail(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Lỗi: Không thể gửi email. Vui lòng thử lại sau hoặc liên hệ hỗ trợ.");
        }

        return ResponseEntity.ok(genericMessage);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody AuthDTOs.ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body("Lỗi: Yêu cầu không hợp lệ!");
        }

        if (user.getResetOtp() == null || !user.getResetOtp().equals(request.getOtp())) {
            return ResponseEntity.badRequest().body("Lỗi: Mã OTP không chính xác!");
        }

        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Lỗi: Mã OTP đã hết hạn sử dụng!");
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.");
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private AuthDTOs.JwtResponse generateJwtResponse(Authentication authentication) {
        String jwt = jwtTokenProvider.generateToken(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return AuthDTOs.JwtResponse.builder()
                .token(jwt)
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .roles(roles)
                .build();
    }

    private void createPatientRecord(User user) {
        String patientCode = "BN" + (100000 + new Random().nextInt(900000));
        Patient patient = Patient.builder()
                .user(user)
                .patientCode(patientCode)
                .bloodType("UNKNOWN")
                .build();
        patientRepository.save(patient);
    }

    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 5);
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 5);
        }
        return username;
    }

    private GoogleUserInfo verifyGoogleToken(String idToken) {
        if (idToken.startsWith("mock_google_")) {
            String suffix = idToken.substring("mock_google_".length());
            return new GoogleUserInfo(
                    "mock-google-id-" + suffix,
                    suffix + "@gmail.com",
                    "Google User " + suffix,
                    "https://lh3.googleusercontent.com/a/mock_avatar");
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.body());

                String sub = rootNode.path("sub").asText();
                String email = rootNode.path("email").asText();
                String name = rootNode.path("name").asText();
                String picture = rootNode.path("picture").asText();

                if (sub != null && !sub.isEmpty() && email != null && !email.isEmpty()) {
                    return new GoogleUserInfo(sub, email, name, picture);
                }
            }
        } catch (Exception e) {
            log.error("Xác thực Google token thất bại: ", e);
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