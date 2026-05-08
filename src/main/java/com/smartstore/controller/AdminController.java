package com.smartstore.controller;

import com.smartstore.model.Admin;
import com.smartstore.model.Dispute;
import com.smartstore.model.Order;
import com.smartstore.model.Shop;
import com.smartstore.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeliveryBoyRepository deliveryBoyRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    @PostConstruct
    public void init() {
        if (adminRepository.findByEmail("admin@smartstore.com").isEmpty()) {
            Admin admin = new Admin();
            admin.setEmail("admin@smartstore.com");
            admin.setPassword("admin123");
            admin.setName("Super Admin");
            adminRepository.save(admin);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        Optional<Admin> adminOpt = adminRepository.findByEmail(email);
        if (adminOpt.isPresent() && adminOpt.get().getPassword().equals(password)) {
            Admin admin = adminOpt.get();
            admin.setPassword(null); // Hide password
            return ResponseEntity.ok(admin);
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalShops = shopRepository.count();
        long totalCustomers = customerRepository.count();
        long totalDeliveryPartners = deliveryBoyRepository.count();
        List<Order> orders = orderRepository.findAll();
        long totalOrders = orders.size();
        
        double totalRevenue = orders.stream()
                .filter(o -> "DELIVERED".equals(o.getStatus()))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalShops", totalShops);
        stats.put("totalCustomers", totalCustomers);
        stats.put("totalDeliveryPartners", totalDeliveryPartners);
        stats.put("totalOrders", totalOrders);
        stats.put("totalRevenue", totalRevenue);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/shops")
    public ResponseEntity<List<Shop>> getAllShops() {
        return ResponseEntity.ok(shopRepository.findAll());
    }

    @DeleteMapping("/shops/{id}")
    public ResponseEntity<?> deleteShop(@PathVariable Long id) {
        if (shopRepository.existsById(id)) {
            shopRepository.deleteById(id);
            return ResponseEntity.ok("Shop deleted successfully");
        }
        return ResponseEntity.badRequest().body("Shop not found");
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @GetMapping("/delivery-boys")
    public ResponseEntity<List<com.smartstore.model.DeliveryBoy>> getAllDeliveryBoys() {
        List<com.smartstore.model.DeliveryBoy> deliveryBoys = deliveryBoyRepository.findAll();
        for (com.smartstore.model.DeliveryBoy db : deliveryBoys) {
            double earnings = orderRepository.findByDeliveryBoyId(db.getId()).stream()
                    .filter(o -> "DELIVERED".equals(o.getStatus()))
                    .mapToDouble(Order::getDeliveryCharge)
                    .sum();
            db.setTotalEarnings(earnings);
        }
        return ResponseEntity.ok(deliveryBoys);
    }

    @DeleteMapping("/delivery-boys/{id}")
    public ResponseEntity<?> deleteDeliveryBoy(@PathVariable Long id) {
        if (deliveryBoyRepository.existsById(id)) {
            deliveryBoyRepository.deleteById(id);
            return ResponseEntity.ok("Delivery partner deleted successfully");
        }
        return ResponseEntity.badRequest().body("Delivery partner not found");
    }

    @PutMapping("/delivery-boys/{id}/approve")
    public ResponseEntity<?> approveDeliveryBoy(@PathVariable Long id) {
        Optional<com.smartstore.model.DeliveryBoy> dbOpt = deliveryBoyRepository.findById(id);
        if (dbOpt.isPresent()) {
            com.smartstore.model.DeliveryBoy db = dbOpt.get();
            db.setApproved(true);
            db.setStatus("APPROVED");
            deliveryBoyRepository.save(db);
            return ResponseEntity.ok(db);
        }
        return ResponseEntity.badRequest().body("Delivery partner not found");
    }

    @GetMapping("/disputes")
    public ResponseEntity<List<Dispute>> getAllDisputes() {
        return ResponseEntity.ok(disputeRepository.findAll());
    }

    @PutMapping("/disputes/{id}/resolve")
    public ResponseEntity<?> resolveDispute(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isPresent()) {
            Dispute dispute = disputeOpt.get();
            dispute.setStatus("RESOLVED");
            dispute.setResolutionNotes(payload.get("resolutionNotes"));
            dispute.setResolvedAt(LocalDateTime.now());
            disputeRepository.save(dispute);
            return ResponseEntity.ok(dispute);
        }
        return ResponseEntity.badRequest().body("Dispute not found");
    }
}
