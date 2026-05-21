package com.backend.clinic.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        if (mailSender == null) {
            System.err.println("WARNING: JavaMailSender is not configured. Cannot send email to " + toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("tvt2004nd@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Mã OTP khôi phục mật khẩu - DermaCare");
            message.setText("Xin chào,\n\n"
                    + "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản ứng dụng DermaCare.\n"
                    + "Mã OTP xác nhận của bạn là: " + otp + "\n"
                    + "Mã này có hiệu lực trong vòng 10 phút.\n\n"
                    + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ DermaCare");

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error sending email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
