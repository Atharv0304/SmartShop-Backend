package com.smartstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${app.mail.from}")
    private String fromEmail;

    private Map<String, String> otpStorage = new HashMap<>();

    // Generate and send OTP for registration
    public void sendOtp(String email) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        otpStorage.put(email, otp);
        System.out.println("Registration OTP for " + email + ": " + otp);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Smart Store - Email Verification OTP");
            message.setText(
                "Hello!\n\n" +
                "Your OTP for Smart Store registration is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "Smart Store Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Warning: Failed to send Registration OTP email to " + email + ". Error: " + e.getMessage());
        }
    }

    // Generate and send OTP for deletion
    public void sendDeleteOtp(String email) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        otpStorage.put(email, otp);
        System.out.println("Deletion OTP for " + email + ": " + otp);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Smart Store - Account Deletion OTP");
            message.setText(
                "Hello!\n\n" +
                "Your OTP to DELETE your Smart Store account is: " + otp + "\n\n" +
                "If you didn't request this, please ignore this email.\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "Smart Store Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Warning: Failed to send Deletion OTP email to " + email + ". Error: " + e.getMessage());
        }
    }

    // Verify OTP
    public boolean verifyOtp(String email, String otp) {
        String stored = otpStorage.get(email);
        if (stored != null && stored.equals(otp)) {
            otpStorage.remove(email);
            return true;
        }
        return false;
    }

    // Send delivery OTP to customer email
    public void sendDeliveryOtp(String customerEmail,
                                 String customerName,
                                 String otp,
                                 Long orderId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(customerEmail);
            message.setSubject("Smart Store - Delivery OTP for Order #" + orderId);
            message.setText(
                "Hello " + customerName + "!\n\n" +
                "Your order #" + orderId + " is confirmed!\n\n" +
                "Your Delivery OTP: " + otp + "\n\n" +
                "Share this OTP with the delivery boy ONLY when you " +
                "receive your order at your doorstep.\n" +
                "DO NOT share this OTP before receiving your order.\n\n" +
                "This OTP is valid for 30 minutes.\n\n" +
                "Thank you for shopping with Smart Store!\n" +
                "Smart Store Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Delivery OTP email failed: " + e.getMessage());
        }
    }
}