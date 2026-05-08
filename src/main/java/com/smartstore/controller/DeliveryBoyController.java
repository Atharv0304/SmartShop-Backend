package com.smartstore.controller;

import com.smartstore.config.JwtUtil;
import com.smartstore.model.DeliveryBoy;
import com.smartstore.service.DeliveryBoyService;
import com.smartstore.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery")
@CrossOrigin(origins = "*")
public class DeliveryBoyController {

    @Autowired
    private DeliveryBoyService deliveryBoyService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtUtil jwtUtil;

    // Send OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        otpService.sendOtp(email);
        return ResponseEntity.ok("OTP sent to " + email);
    }

    // Verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        if (otpService.verifyOtp(email, otp)) {
            deliveryBoyService.verifyEmail(email);
            return ResponseEntity.ok("Email verified!");
        }
        return ResponseEntity.status(400).body("Invalid OTP!");
    }

    // Register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody DeliveryBoy boy) {
        String error = deliveryBoyService.validateRegistration(boy);
        if (error != null) {
            return ResponseEntity.status(400).body(error);
        }
        DeliveryBoy saved = deliveryBoyService.register(boy);
        return ResponseEntity.ok(saved);
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        DeliveryBoy boy = deliveryBoyService.login(email, password);

        if (boy != null) {
            // Debug log
            System.out.println("Email verified: " + boy.isEmailVerified());
            System.out.println("Is approved: " + boy.isApproved());

            if (!boy.isEmailVerified()) {
                return ResponseEntity.status(403).body("Please verify your email first!");
            }
            String token = jwtUtil.generateToken(email);
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("id", boy.getId());
            response.put("name", boy.getName());
            response.put("email", boy.getEmail());
            response.put("status", boy.getStatus());
            response.put("isApproved", boy.isApproved());
            response.put("isAvailable", boy.isAvailable());
            response.put("rating", boy.getRating());
            response.put("totalDeliveries", boy.getTotalDeliveries());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body("Invalid email or password!");
    }

    // Get all delivery boys (for admin)
    @GetMapping("/all")
    public List<DeliveryBoy> getAllDeliveryBoys() {
        return deliveryBoyService.getAllDeliveryBoys();
    }

    // Approve
    @PutMapping("/approve/{id}")
    public DeliveryBoy approve(@PathVariable Long id) {
        return deliveryBoyService.approve(id);
    }

    // Update Availability
    @PutMapping("/availability/{id}")
    public ResponseEntity<?> updateAvailability(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean available = body.get("available");
        if (available == null) return ResponseEntity.badRequest().body("Missing 'available' field");
        DeliveryBoy boy = deliveryBoyService.updateAvailability(id, available);
        return ResponseEntity.ok(boy);
    }

    // Reject
    @PutMapping("/reject/{id}")
    public DeliveryBoy reject(@PathVariable Long id) {
        return deliveryBoyService.reject(id);
    }

    // Delete
    @DeleteMapping("/profile/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        boolean deleted = deliveryBoyService.deleteDeliveryBoy(id);
        if (deleted) {
            return ResponseEntity.ok("Account deleted successfully");
        }
        return ResponseEntity.status(404).body("Account not found");
    }
}
