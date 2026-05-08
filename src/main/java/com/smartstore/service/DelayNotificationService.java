package com.smartstore.service;

import com.smartstore.model.Order;
import com.smartstore.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Periodically scans active orders to detect delays and push smart
 * notifications to the relevant parties (customer, delivery boy).
 *
 * Delay thresholds:
 *  - DELIVERY_ACCEPTED  → delivery boy accepted but hasn't picked up from shop yet → 30 min
 *  - PICKED / OUT_FOR_DELIVERY → picked up but hasn't delivered yet            → 45 min
 */
@Service
public class DelayNotificationService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private NotificationService notificationService;

    // Runs every 5 minutes
    @Scheduled(fixedDelay = 300_000)
    public void checkDelays() {
        LocalDateTime now = LocalDateTime.now();

        // --- Case 1: Delivery boy accepted but hasn't reached shop in 30 min ---
        List<Order> acceptedOrders = orderRepository.findByStatusOrderByIdDesc("DELIVERY_ACCEPTED");
        for (Order order : acceptedOrders) {
            if (order.getAssignedTime() == null) continue;
            long minutesSinceAssigned = ChronoUnit.MINUTES.between(order.getAssignedTime(), now);

            if (minutesSinceAssigned >= 30) {
                // Notify customer
                String customerMsg = String.format(
                    "Your delivery partner %s (📞 %s) accepted your order #%d from %s but seems delayed in reaching the shop. " +
                    "You can contact them directly at %s. We apologize for the delay!",
                    order.getDeliveryBoyName(), order.getDeliveryBoyPhone(),
                    order.getId(), order.getShopName(), order.getDeliveryBoyPhone()
                );
                pushUniqueNotification(order.getCustomerId(), "CUSTOMER",
                    "DELIVERY_DELAY", "⏰ Delivery Delay Alert — Order #" + order.getId(),
                    customerMsg, order.getId());

                // Notify delivery boy
                String dbMsg = String.format(
                    "Reminder: Order #%d from %s is waiting for pickup! Please proceed to the shop immediately. " +
                    "The customer has been notified of the delay.",
                    order.getId(), order.getShopName()
                );
                pushUniqueNotification(order.getDeliveryBoyId(), "DELIVERY",
                    "DELIVERY_DELAY", "⚠️ Pickup Reminder — Order #" + order.getId(),
                    dbMsg, order.getId());
            }
        }

        // --- Case 2: Order picked but not delivered in 45 min ---
        List<Order> pickedOrders = orderRepository.findByStatusInOrderByIdDesc(List.of("PICKED", "OUT_FOR_DELIVERY"));
        for (Order order : pickedOrders) {
            LocalDateTime ref = order.getPickedTime() != null ? order.getPickedTime()
                              : order.getOutForDeliveryTime();
            if (ref == null) continue;
            long minutesSincePicked = ChronoUnit.MINUTES.between(ref, now);

            if (minutesSincePicked >= 45) {
                // Notify customer: delivery boy contact info
                String customerMsg = String.format(
                    "Your order #%d was picked up by %s (📞 %s) but hasn't been delivered yet. " +
                    "This might be due to traffic or route issues. " +
                    "You can reach them directly at: %s. We're monitoring your delivery!",
                    order.getId(), order.getDeliveryBoyName(), order.getDeliveryBoyPhone(),
                    order.getDeliveryBoyPhone()
                );
                pushUniqueNotification(order.getCustomerId(), "CUSTOMER",
                    "DELIVERY_DELAY", "📦 Order #" + order.getId() + " — Delivery Taking Longer",
                    customerMsg, order.getId());

                // Notify delivery boy: gentle reminder
                String dbMsg = String.format(
                    "Order #%d has been picked up for over %d minutes. " +
                    "Please complete the delivery to %s as soon as possible. " +
                    "The customer is waiting!",
                    order.getId(), minutesSincePicked, order.getCustomerName()
                );
                pushUniqueNotification(order.getDeliveryBoyId(), "DELIVERY",
                    "DELIVERY_DELAY", "⚡ Hurry! Customer Waiting — Order #" + order.getId(),
                    dbMsg, order.getId());
            }
        }
    }

    /**
     * Only pushes a notification if a delay notification for this exact order
     * hasn't been sent to this user in the last 30 minutes (to avoid spam).
     */
    private void pushUniqueNotification(Long userId, String userType,
            String type, String title,
            String message, Long orderId) {
if (userId == null) return;
try {
var existing = notificationService.getNotifications(userId, userType);
boolean alreadySent = existing.stream().anyMatch(n ->
orderId.equals(n.getOrderId()) &&
"DELIVERY_DELAY".equals(n.getType()) &&
n.getCreatedAt() != null &&
ChronoUnit.MINUTES.between(n.getCreatedAt(), LocalDateTime.now()) < 120  // 👈 change 30 to 120 here
);
if (!alreadySent) {
notificationService.createNotification(userId, userType, type, title, message, orderId, null);
}
} catch (Exception e) {
System.err.println("[DelayNotificationService] Error sending notification: " + e.getMessage());
}
}
}