package com.smartstore.service;

import com.smartstore.model.*;
import com.smartstore.repository.*;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private DeliveryBoyRepository deliveryBoyRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private DeliveryChargeService deliveryChargeService;
    @Autowired private DeliveryRequestRepository deliveryRequestRepository;
    @Autowired private ShopDeliveryConnectionRepository connectionRepository;

    // Place order
    public Order placeOrder(Order order) throws Exception {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new Exception(
                    "Product not found: " + item.getProductName()));
            if (product.getQuantity() < item.getQuantity()) {
                throw new Exception("Insufficient stock for: " +
                    product.getName() + ". Available: " +
                    product.getQuantity());
            }
        }
        for (OrderItem item : order.getItems()) {
            Product product = productRepository
                .findById(item.getProductId()).orElseThrow();
            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
        }
        if (order.getDeliveryLatitude() != 0 &&
            order.getDeliveryLongitude() != 0) {
            double shopLat = 18.5204, shopLng = 73.8567;
            double distance = deliveryChargeService.calculateDistance(
                shopLat, shopLng,
                order.getDeliveryLatitude(), order.getDeliveryLongitude());
            order.setDistanceKm(distance);
            order.setDeliveryCharge(
                deliveryChargeService.calculateCharge(distance));
        }
        order.setStatus("PENDING");
        order.setOrderTime(LocalDateTime.now());
        order.setOtpVerified(false);
        order.setShopOtpVerified(false);
        order.setOtpRetryCount(0);
        Order saved = orderRepository.save(order);

        // Notify customer
        notificationService.createNotification(
            saved.getCustomerId(), "CUSTOMER", "ORDER_PLACED",
            "🛒 Order Placed!",
            "Your order #" + saved.getId() +
            " placed! Total: ₹" + saved.getTotalAmount(),
            saved.getId(), null);

        // Always notify shopkeeper for ALL order types (PICKUP and HOME_DELIVERY)
        String paymentNote = ("RAZORPAY".equals(saved.getPaymentMethod()) || "ONLINE".equals(saved.getPaymentMethod()))
            ? " | ✅ Payment received online (₹" + saved.getTotalAmount() + ")"
            : " | 💵 Customer will pay cash";

        String deliveryNote = "PICKUP".equals(saved.getDeliveryType())
            ? " | 🏪 PICKUP order — customer will come to shop."
            : " | 🚚 HOME DELIVERY to: " + saved.getDeliveryAddress();

        notificationService.createNotification(
            saved.getShopId(), "SHOPKEEPER", "NEW_ORDER",
            "🛒 New Order #" + saved.getId() + "!",
            "Customer " + saved.getCustomerName() + " (📞 " + saved.getCustomerPhone() + ") placed an order." +
            paymentNote + deliveryNote + " | Total: ₹" + saved.getTotalAmount(),
            saved.getId(), null);

        return saved;
    }

    // Confirm order — send customer delivery OTP
    public Order confirmOrder(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new Exception("Order not found"));

        // Generate delivery OTP for customer
        String deliveryOtp = String.valueOf(
            new Random().nextInt(9000) + 1000);

        order.setDeliveryOtp(deliveryOtp);
        order.setOtpExpiresAt(LocalDateTime.now().plusHours(24));
        order.setStatus("CONFIRMED");
        order.setConfirmedTime(LocalDateTime.now());
        order.setOtpRetryCount(0);
        Order saved = orderRepository.save(order);

        // Notify customer with OTP — message differs for pickup vs home delivery
        String otpMessage = "PICKUP".equals(order.getDeliveryType())
            ? "Order #" + saved.getId() + " confirmed! 🏪 Your Pickup OTP: " + deliveryOtp
                + " — Show this to the shopkeeper when you come to collect your order."
            : "Order #" + saved.getId() + " confirmed! 🚚 Your Delivery OTP: " + deliveryOtp
                + " — Give this to delivery boy ONLY after checking all products.";

        notificationService.createNotification(
            saved.getCustomerId(), "CUSTOMER", "ORDER_CONFIRMED",
            "PICKUP".equals(order.getDeliveryType()) ? "🏪 Pickup Order Confirmed!" : "✅ Order Confirmed!",
            otpMessage,
            saved.getId(), deliveryOtp);

        System.out.println("Pickup/Delivery OTP for Order #" + orderId + ": " + deliveryOtp);
        return saved;
    }

    // Order Ready — broadcast to delivery boys + generate shop OTP
    // For PICKUP orders, just notify shopkeeper to prepare
    public Order orderReady(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new Exception("Order not found"));

        // PICKUP flow: mark ready for pickup, no delivery boy needed
        if ("PICKUP".equals(order.getDeliveryType())) {
            order.setStatus("READY_FOR_PICKUP");
            order.setReadyTime(LocalDateTime.now());
            Order saved = orderRepository.save(order);
            // Notify customer to come collect
            notificationService.createNotification(
                saved.getCustomerId(), "CUSTOMER", "STATUS_UPDATE",
                "✅ Your Order is Ready for Pickup!",
                "Order #" + orderId + " from " + saved.getShopName() +
                " is ready! Please come to the shop to collect it." +
                ("CASH".equals(saved.getPaymentMethod())
                    ? " Remember to bring ₹" + saved.getTotalAmount() + " cash."
                    : " Your online payment has been confirmed."),
                orderId, null);
            return saved;
        }

        // HOME_DELIVERY flow: generate shop OTP and broadcast to delivery boys
        String shopOtp = String.valueOf(
            new Random().nextInt(9000) + 1000);

        order.setShopOtp(shopOtp);
        order.setStatus("LOOKING_FOR_DELIVERY");
        order.setReadyTime(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        // Clear previous delivery requests for this order to avoid duplicates on retry
        List<DeliveryRequest> existingRequests = deliveryRequestRepository.findByOrderId(orderId);
        if (!existingRequests.isEmpty()) {
            deliveryRequestRepository.deleteAll(existingRequests);
        }

        // Send requests to connected and approved delivery boys
        List<ShopDeliveryConnection> connections = connectionRepository.findByShopIdAndStatus(order.getShopId(), "APPROVED");
        int requestsSent = 0;
        for (ShopDeliveryConnection conn : connections) {
            DeliveryBoy boy = deliveryBoyRepository.findById(conn.getDeliveryBoyId()).orElse(null);
            if (boy == null || !boy.isAvailable()) continue;

            // Create delivery request
            DeliveryRequest req = new DeliveryRequest();
            req.setOrderId(orderId);
            req.setDeliveryBoyId(boy.getId());
            req.setStatus("PENDING");
            req.setRequestedAt(LocalDateTime.now());
            req.setShopName(saved.getShopName());
            req.setCustomerArea(saved.getDeliveryAddress());
            req.setTotalAmount(saved.getTotalAmount());
            req.setDeliveryCharge(saved.getDeliveryCharge());
            req.setDistanceKm(saved.getDistanceKm());
            deliveryRequestRepository.save(req);

            // Notify delivery boy
            notificationService.createNotification(
                boy.getId(), "DELIVERY", "NEW_ORDER_REQUEST",
                "🔔 New Delivery Request!",
                "Order #" + orderId + " from " + saved.getShopName() +
                " | Earn: ₹" + Math.round(saved.getDeliveryCharge()) +
                " | Distance: " + saved.getDistanceKm() + " km" +
                " | Area: " + saved.getDeliveryAddress(),
                orderId, null);
            requestsSent++;
        }

        System.out.println("Shop OTP for Order #" +
            orderId + ": " + shopOtp);
        System.out.println("Requests sent to " +
            requestsSent + " delivery boys");
        return saved;
    }

    // Delivery boy accepts request
    public Order acceptDeliveryRequest(Long orderId, Long deliveryBoyId)
            throws Exception {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new Exception("Order not found"));

        if (!"LOOKING_FOR_DELIVERY".equals(order.getStatus())) {
            throw new Exception(
                "Order already accepted by another delivery boy!");
        }

        DeliveryBoy boy = deliveryBoyRepository.findById(deliveryBoyId)
            .orElseThrow(() -> new Exception("Delivery boy not found"));

        // Update all requests for this order
        List<DeliveryRequest> requests = deliveryRequestRepository
            .findByOrderId(orderId);
        for (DeliveryRequest req : requests) {
            if (req.getDeliveryBoyId().equals(deliveryBoyId)) {
                req.setStatus("ACCEPTED");
            } else {
                req.setStatus("EXPIRED");
            }
            req.setRespondedAt(LocalDateTime.now());
        }
        deliveryRequestRepository.saveAll(requests);

        // Assign to order
        order.setDeliveryBoyId(deliveryBoyId);
        order.setDeliveryBoyName(boy.getName());
        order.setDeliveryBoyPhone(boy.getPhone());
        order.setDeliveryBoyVehicle(boy.getVehicleNumber());
        order.setStatus("DELIVERY_ACCEPTED");
        order.setAssignedTime(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        // Get shop OTP for delivery boy
        String shopOtpNotif = "Go to " + saved.getShopName() +
            " and give the Shop OTP to collect your order.";

        // Notify delivery boy with shop OTP notification
        notificationService.createNotification(
            deliveryBoyId, "DELIVERY", "ORDER_ACCEPTED",
            "✅ Order Accepted!",
            "Order #" + orderId + " accepted! " + shopOtpNotif +
            " Shop: " + saved.getShopName(),
            orderId, null);

        // Notify shopkeeper via customer notification
        notificationService.createNotification(
            saved.getCustomerId(), "CUSTOMER", "DELIVERY_ASSIGNED",
            "🚴 Delivery Partner Found!",
            "Delivery partner " + boy.getName() +
            " (" + boy.getPhone() + ") accepted your order #" +
            orderId + " and is heading to the shop.",
            orderId, null);

        return saved;
    }

    // Verify shop OTP — delivery boy gives OTP to shopkeeper
    public Order verifyShopOtp(Long orderId, String otp) throws Exception {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new Exception("Order not found"));

        if (!otp.equals(order.getShopOtp())) {
            throw new Exception("Invalid Shop OTP!");
        }

        order.setShopOtpVerified(true);
        order.setStatus("PICKED");
        order.setPickedTime(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        // Notify customer — order picked up
        notificationService.createNotification(
            saved.getCustomerId(), "CUSTOMER", "STATUS_UPDATE",
            "📦 Order Picked Up!",
            "Your order #" + orderId +
            " has been picked up by " + saved.getDeliveryBoyName() +
            " and is on the way to you!",
            orderId, null);

        // Notify delivery boy
        notificationService.createNotification(
            saved.getDeliveryBoyId(), "DELIVERY", "SHOP_OTP_VERIFIED",
            "✅ Shop OTP Verified!",
            "Order collected! Now deliver to: " +
            saved.getDeliveryAddress() +
            " | Customer: " + saved.getCustomerName() +
            " | Phone: " + saved.getCustomerPhone(),
            orderId, null);

        return saved;
    }

    // Verify delivery OTP — customer verifies after checking products
    public Order verifyOtpAndDeliver(Long orderId, String otp)
            throws Exception {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new Exception("Order not found"));

        if (order.getOtpRetryCount() >= 3) {
            throw new Exception("Too many failed attempts!");
        }
        if (order.getOtpExpiresAt() != null &&
            LocalDateTime.now().isAfter(order.getOtpExpiresAt())) {
            throw new Exception("OTP expired!");
        }

        if (!otp.equals(order.getDeliveryOtp())) {
            order.setOtpRetryCount(order.getOtpRetryCount() + 1);
            orderRepository.save(order);
            int remaining = 3 - order.getOtpRetryCount();
            throw new Exception("Invalid OTP! " +
                remaining + " attempts remaining.");
        }

        order.setOtpVerified(true);
        order.setStatus("DELIVERED");
        order.setDeliveredTime(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        // Notify customer
        notificationService.createNotification(
            saved.getCustomerId(), "CUSTOMER", "ORDER_DELIVERED",
            "🎉 Order Delivered!",
            "Order #" + orderId +
            " delivered successfully! Thank you for shopping.",
            orderId, null);

        // Notify delivery boy
        if (saved.getDeliveryBoyId() != null) {
            notificationService.createNotification(
                saved.getDeliveryBoyId(), "DELIVERY", "DELIVERY_COMPLETE",
                "✅ Delivery Complete!",
                "Order #" + orderId + " delivered! " +
                "Earnings: ₹" + Math.round(saved.getDeliveryCharge()),
                orderId, null);
        }
        return saved;
    }

    // Reject delivery request
    public void rejectDeliveryRequest(Long orderId, Long deliveryBoyId) {
        List<DeliveryRequest> requests = deliveryRequestRepository
            .findByOrderId(orderId);
        requests.stream()
            .filter(r -> r.getDeliveryBoyId().equals(deliveryBoyId))
            .findFirst()
            .ifPresent(r -> {
                r.setStatus("REJECTED");
                r.setRespondedAt(LocalDateTime.now());
                deliveryRequestRepository.save(r);
            });
    }

    public Order updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        switch (status) {
            case "CONFIRMED" -> order.setConfirmedTime(LocalDateTime.now());
            case "ASSIGNED" -> order.setAssignedTime(LocalDateTime.now());
            case "PICKED" -> order.setPickedTime(LocalDateTime.now());
            case "OUT_FOR_DELIVERY" ->
                order.setOutForDeliveryTime(LocalDateTime.now());
            case "DELIVERED" -> order.setDeliveredTime(LocalDateTime.now());
        }
        Order saved = orderRepository.save(order);
        String msg = switch (status) {
            case "PREPARING" -> "👨‍🍳 Shopkeeper is preparing your order.";
            case "PICKED" -> "📦 Order picked up by delivery partner.";
            case "OUT_FOR_DELIVERY" -> "🛵 Order is out for delivery!";
            case "DELIVERED" -> "🎉 Order delivered!";
            case "CANCELLED" -> "❌ Order cancelled.";
            default -> "Order status: " + status;
        };
        notificationService.createNotification(
            saved.getCustomerId(), "CUSTOMER", "STATUS_UPDATE",
            "📍 Order #" + saved.getId() + " Update", msg,
            saved.getId(), null);
        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
    }
    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByIdDesc(customerId);
    }
    public List<Order> getOrdersByShop(Long shopId) {
        return orderRepository.findByShopIdOrderByIdDesc(shopId);
    }
    public List<Order> getOrdersByDeliveryBoy(Long deliveryBoyId) {
        return orderRepository.findByDeliveryBoyIdOrderByIdDesc(deliveryBoyId);
    }
    public List<Map<String, Object>> getAvailableDeliveryBoys(
            double shopLat, double shopLng) {
        List<DeliveryBoy> boys = deliveryBoyRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (DeliveryBoy boy : boys) {
            if (!"APPROVED".equals(boy.getStatus())) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", boy.getId());
            map.put("name", boy.getName());
            map.put("phone", boy.getPhone());
            map.put("vehicleNumber", boy.getVehicleNumber());
            map.put("vehicleType", boy.getVehicleType());
            map.put("rating", boy.getRating());
            result.add(map);
        }
        return result;
    }

    // Get single order by ID — for chatbot tracking
    public java.util.Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    // Cancel order — validates ownership, reason, and cancellable status
    public Order cancelOrder(Long orderId, Long customerId, String reason) throws Exception {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new Exception(
                "❌ Order #" + orderId + " was not found. Please check the order ID."));

        if (!order.getCustomerId().equals(customerId)) {
            throw new Exception(
                "❌ Order #" + orderId + " does not belong to your account. Please check the ID.");
        }

        if ("CANCELLED".equals(order.getStatus())) {
            throw new Exception("Order #" + orderId + " is already cancelled.");
        }
        if ("DELIVERED".equals(order.getStatus())) {
            throw new Exception(
                "❌ Order #" + orderId + " has already been delivered and cannot be cancelled.");
        }
        // Block once delivery boy has accepted
        if ("DELIVERY_ACCEPTED".equals(order.getStatus()) ||
            "PICKED".equals(order.getStatus()) ||
            "OUT_FOR_DELIVERY".equals(order.getStatus())) {
            throw new Exception(
                "❌ Cannot cancel Order #" + orderId +
                ". A delivery partner has already accepted and is on the way to the shop. " +
                "Please contact the shopkeeper directly.");
        }

        String reasonNote = (reason != null && !reason.isBlank())
            ? " | Reason: " + reason : "";

        // If delivery requests were already sent, cancel them and notify each delivery boy
        if ("LOOKING_FOR_DELIVERY".equals(order.getStatus())) {
            List<DeliveryRequest> requests = deliveryRequestRepository.findByOrderId(orderId);
            for (DeliveryRequest req : requests) {
                if ("PENDING".equals(req.getStatus())) {
                    req.setStatus("CANCELLED");
                    notificationService.createNotification(
                        req.getDeliveryBoyId(), "DELIVERY", "ORDER_CANCELLED",
                        "❌ Order #" + orderId + " Cancelled",
                        "Order #" + orderId + " from " + order.getShopName() +
                        " was cancelled by the customer before you accepted it." + reasonNote,
                        orderId, null);
                }
            }
            deliveryRequestRepository.saveAll(requests);
        }

        if (reason != null && !reason.isBlank()) {
            order.setCancelReason(reason);
        }
        order.setStatus("CANCELLED");
        Order saved = orderRepository.save(order);

        // Notify customer
        notificationService.createNotification(
            customerId, "CUSTOMER", "ORDER_CANCELLED",
            "❌ Order Cancelled",
            "Your order #" + orderId + " from " + saved.getShopName() +
            " has been cancelled." + reasonNote,
            orderId, null);

        // Notify shopkeeper with reason
        notificationService.createNotification(
            saved.getShopId(), "SHOPKEEPER", "ORDER_CANCELLED",
            "❌ Order #" + orderId + " Cancelled by Customer",
            "Customer " + saved.getCustomerName() + " cancelled order #" +
            orderId + "." + reasonNote + " Please stop preparing this order.",
            orderId, null);

        return saved;
    }

    // Overload for backward compat (no reason)
    public Order cancelOrder(Long orderId, Long customerId) throws Exception {
        return cancelOrder(orderId, customerId, null);
    }

    // Cancel order by SHOPKEEPER — saves reason and notifies customer
    public Order cancelOrderByShopkeeper(Long orderId, Long shopId, String reason) throws Exception {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new Exception("Order #" + orderId + " not found."));

        if (!order.getShopId().equals(shopId)) {
            throw new Exception("This order does not belong to your shop.");
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw new Exception("Order #" + orderId + " is already cancelled.");
        }
        if ("DELIVERED".equals(order.getStatus())) {
            throw new Exception("Cannot cancel a delivered order.");
        }

        String reasonNote = (reason != null && !reason.isBlank()) ? reason : "No reason provided";

        // Cancel any pending delivery requests
        if ("LOOKING_FOR_DELIVERY".equals(order.getStatus())) {
            List<DeliveryRequest> requests = deliveryRequestRepository.findByOrderId(orderId);
            for (DeliveryRequest req : requests) {
                if ("PENDING".equals(req.getStatus())) {
                    req.setStatus("CANCELLED");
                    notificationService.createNotification(
                        req.getDeliveryBoyId(), "DELIVERY", "ORDER_CANCELLED",
                        "❌ Order #" + orderId + " Cancelled",
                        "Order #" + orderId + " was cancelled by the shopkeeper before you accepted.",
                        orderId, null);
                }
            }
            deliveryRequestRepository.saveAll(requests);
        }

        // Prefix with SHOPKEEPER: so frontend can distinguish customer vs shopkeeper cancellation
        order.setCancelReason("SHOPKEEPER:" + reasonNote);
        order.setStatus("CANCELLED");
        Order saved = orderRepository.save(order);

        // Notify customer with the reason
        notificationService.createNotification(
            saved.getCustomerId(), "CUSTOMER", "ORDER_CANCELLED",
            "❌ Order #" + orderId + " Cancelled by Shop",
            "Sorry! " + saved.getShopName() + " cancelled your order #" + orderId +
            ". Reason: " + reasonNote + ". You will receive a refund if you paid online.",
            orderId, null);

        return saved;
    }

	public @org.jspecify.annotations.Nullable Object assignDeliveryBoy(Long orderId, Long long1) {
		// TODO Auto-generated method stub
		return null;
	}
}