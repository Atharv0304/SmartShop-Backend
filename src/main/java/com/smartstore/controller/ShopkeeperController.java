package com.smartstore.controller;

import com.smartstore.config.JwtUtil;
import com.smartstore.model.Shopkeeper;
import com.smartstore.service.OtpService;
import com.smartstore.service.ShopkeeperService;
import com.smartstore.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/shopkeeper")
@CrossOrigin(origins = "*")
public class ShopkeeperController {

    @Autowired
    private ShopkeeperService shopkeeperService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AnalyticsService analyticsService;

    // Send OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (shopkeeperService.emailExists(email)) {
            return ResponseEntity.status(400).body("Email already registered!");
        }
        otpService.sendOtp(email);
        return ResponseEntity.ok("OTP sent to " + email);
    }

    // Verify OTP and Register
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        if (otpService.verifyOtp(email, otp)) {
            return ResponseEntity.ok("OTP verified successfully!");
        }
        return ResponseEntity.status(400).body("Invalid OTP!");
    }

    // Register after OTP verified
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Shopkeeper shopkeeper) {
        if (shopkeeperService.emailExists(shopkeeper.getEmail())) {
            return ResponseEntity.status(400).body("Email already registered!");
        }
        Shopkeeper saved = shopkeeperService.register(shopkeeper);
        return ResponseEntity.ok(saved);
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        Shopkeeper shopkeeper = shopkeeperService.login(email, password);
        if (shopkeeper != null) {
            String token = jwtUtil.generateToken(email);
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("id", shopkeeper.getId());
            response.put("name", shopkeeper.getName());
            response.put("shopName", shopkeeper.getShopName());
            response.put("email", shopkeeper.getEmail());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body("Invalid email or password");
    }

    // Get Profile
    @GetMapping("/profile/{email:.+}")
    public ResponseEntity<?> getProfile(@PathVariable String email) {
        Shopkeeper shopkeeper = shopkeeperService.getProfile(email);
        if (shopkeeper != null)
            return ResponseEntity.ok(shopkeeper);
        return ResponseEntity.status(404).body("Profile not found");
    }

    // Update Profile
    @PutMapping("/profile/{email:.+}")
    public ResponseEntity<?> updateProfile(@PathVariable String email, @RequestBody Shopkeeper shopkeeper) {
        Shopkeeper updated = shopkeeperService.updateShopkeeper(email, shopkeeper);
        if (updated != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("name", updated.getName());
            response.put("shopName", updated.getShopName());
            response.put("email", updated.getEmail());
            response.put("phone", updated.getPhone());
            response.put("upiId", updated.getUpiId());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(400).body("Failed to update profile");
    }

    // Delete Profile
    @DeleteMapping("/profile/{email:.+}")
    public ResponseEntity<?> deleteProfile(@PathVariable String email, @RequestParam String otp) {
        if (!otpService.verifyOtp(email, otp)) {
            return ResponseEntity.status(400).body("Invalid or expired OTP!");
        }
        boolean deleted = shopkeeperService.deleteShopkeeper(email);
        if (deleted) {
            return ResponseEntity.ok("Account deleted successfully");
        }
        return ResponseEntity.status(404).body("Account not found");
    }

    // Send OTP for deletion
    @PostMapping("/send-delete-otp")
    public ResponseEntity<?> sendDeleteOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (!shopkeeperService.emailExists(email)) {
            return ResponseEntity.status(404).body("Account not found!");
        }
        otpService.sendDeleteOtp(email);
        return ResponseEntity.ok("OTP sent to " + email);
    }

    // Get Analytics
    @GetMapping("/analytics/{email:.+}")
    public ResponseEntity<?> getAnalytics(@PathVariable String email) {
        Shopkeeper shopkeeper = shopkeeperService.getProfile(email);
        if (shopkeeper == null) {
            return ResponseEntity.status(404).body("Shopkeeper not found");
        }
        return ResponseEntity.ok(analyticsService.getShopAnalyticsByEmail(shopkeeper.getEmail()));
    }
}
