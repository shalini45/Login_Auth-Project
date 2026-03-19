package com.authservice.Service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log =
        LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Password Reset OTP - Auth Service");
            message.setText(
                "Hello,\n\n" +
                "Your OTP for password reset is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes only.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Regards,\nAuth Service"
            );

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email");
        }
    }

     // ─── Send OTP for Email Verification ─────────────────────
    public void sendVerificationEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Verify Your Email - Auth Service");
            message.setText(
                "Hello,\n\n" +
                "Welcome to Auth Service!\n\n" +
                "Your email verification OTP is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes only.\n\n" +
                "If you did not create an account, ignore this email.\n\n" +
                "Regards,\nAuth Service"
            );
            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email");
        }
    }
}