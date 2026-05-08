package com.smartstore.controller;

import com.smartstore.model.DeliveryRequest;
import com.smartstore.model.Order;
import com.smartstore.repository.DeliveryRequestRepository;
import com.smartstore.service.DeliveryChargeService;
import com.smartstore.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired private OrderService orderService;
    @Autowired private DeliveryChargeService deliveryChargeService;
    @Autowired private DeliveryRequestRepository deliveryRequestRepository;

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(@RequestBody Order order) {
        try {
            return ResponseEntity.ok(orderService.placeOrder(order));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PutMapping("/confirm/{orderId}")
    public ResponseEntity<?> confirmOrder(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.confirmOrder(orderId));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Order Ready — broadcast to delivery boys
    @PutMapping("/ready/{orderId}")
    public ResponseEntity<?> orderReady(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.orderReady(orderId));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Get pending requests for delivery boy
    @GetMapping("/requests/{deliveryBoyId}")
    public ResponseEntity<?> getDeliveryRequests(
            @PathVariable Long deliveryBoyId) {
        List<DeliveryRequest> requests = deliveryRequestRepository
            .findByDeliveryBoyIdAndStatus(deliveryBoyId, "PENDING");
        return ResponseEntity.ok(requests);
    }

    // Accept delivery request
    @PutMapping("/accept/{orderId}/{deliveryBoyId}")
    public ResponseEntity<?> acceptRequest(
            @PathVariable Long orderId,
            @PathVariable Long deliveryBoyId) {
        try {
            return ResponseEntity.ok(
                orderService.acceptDeliveryRequest(orderId, deliveryBoyId));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Reject delivery request
    @PutMapping("/reject/{orderId}/{deliveryBoyId}")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long orderId,
            @PathVariable Long deliveryBoyId) {
        orderService.rejectDeliveryRequest(orderId, deliveryBoyId);
        return ResponseEntity.ok("Rejected");
    }

    // Verify shop OTP
    @PostMapping("/verify-shop-otp")
    public ResponseEntity<?> verifyShopOtp(
            @RequestBody Map<String, Object> body) {
        try {
            Long orderId = Long.valueOf(body.get("orderId").toString());
            String otp = body.get("otp").toString();
            return ResponseEntity.ok(
                orderService.verifyShopOtp(orderId, otp));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Verify delivery OTP
    @PostMapping("/verify-delivery-otp")
    public ResponseEntity<?> verifyDeliveryOtp(
            @RequestBody Map<String, Object> body) {
        try {
            Long orderId = Long.valueOf(body.get("orderId").toString());
            String otp = body.get("otp").toString();
            return ResponseEntity.ok(
                orderService.verifyOtpAndDeliver(orderId, otp));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/delivery-charge")
    public ResponseEntity<?> calculateCharge(
            @RequestBody Map<String, Double> body) {
        try {
            double distance = deliveryChargeService.calculateDistance(
                body.get("shopLat"), body.get("shopLng"),
                body.get("custLat"), body.get("custLng"));
            return ResponseEntity.ok(
                deliveryChargeService.getPricingBreakdown(distance));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PutMapping("/assign/{orderId}")
    public ResponseEntity<?> assignDeliveryBoy(
            @PathVariable Long orderId,
            @RequestBody Map<String, Long> body) {
        try {
            return ResponseEntity.ok(
                orderService.assignDeliveryBoy(
                    orderId, body.get("deliveryBoyId")));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(
                orderService.updateStatus(id, body.get("status")));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/customer/{customerId}")
    public List<Order> getCustomerOrders(@PathVariable Long customerId) {
        return orderService.getOrdersByCustomer(customerId);
    }

    @GetMapping("/shop/{shopId}")
    public List<Order> getShopOrders(@PathVariable Long shopId) {
        return orderService.getOrdersByShop(shopId);
    }

    @GetMapping("/delivery/{deliveryBoyId}")
    public List<Order> getDeliveryBoyOrders(
            @PathVariable Long deliveryBoyId) {
        return orderService.getOrdersByDeliveryBoy(deliveryBoyId);
    }

    @GetMapping("/available-delivery-boys")
    public ResponseEntity<?> getAvailableDeliveryBoys(
            @RequestParam double shopLat,
            @RequestParam double shopLng) {
        return ResponseEntity.ok(
            orderService.getAvailableDeliveryBoys(shopLat, shopLng));
    }

    // Get single order by ID (for chatbot tracking)
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        try {
            return orderService.getOrderById(orderId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // Cancel order — validates customer ownership and status
    @PutMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long customerId,
            @RequestParam(required = false) String reason) {
        try {
            return ResponseEntity.ok(orderService.cancelOrder(orderId, customerId, reason));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // Cancel order by shopkeeper — requires reason, notifies customer
    @PutMapping("/cancel-by-shop/{orderId}")
    public ResponseEntity<?> cancelOrderByShop(
            @PathVariable Long orderId,
            @RequestParam Long shopId,
            @RequestParam(required = false) String reason) {
        try {
            return ResponseEntity.ok(orderService.cancelOrderByShopkeeper(orderId, shopId, reason));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
