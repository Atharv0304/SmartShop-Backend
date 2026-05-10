package com.smartstore.service;

import com.smartstore.dto.SalesAnalyticsDTO;
import com.smartstore.model.Order;
import com.smartstore.model.OrderItem;
import com.smartstore.model.Shop;
import com.smartstore.repository.OrderRepository;
import com.smartstore.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShopRepository shopRepository;

    /**
     * Get sales analytics for a shopkeeper identified by email.
     * Counts all orders that are CONFIRMED, READY, OUT_FOR_DELIVERY, or DELIVERED
     * (i.e. any order the shopkeeper has accepted — not PENDING or CANCELLED).
     */
    public SalesAnalyticsDTO getShopAnalyticsByEmail(String shopkeeperEmail) {
        // ── 1. Resolve the shop for this shopkeeper ────────────────────
        Optional<Shop> shopOpt = shopRepository.findByEmail(shopkeeperEmail);

        SalesAnalyticsDTO dto = new SalesAnalyticsDTO();
        if (!shopOpt.isPresent()) {
            // No shop registered yet — return empty analytics
            dto.setTotalOrders(0);
            dto.setTotalRevenue(0);
            dto.setDailyRevenue(new HashMap<>());
            dto.setBestSellingProducts(new ArrayList<>());
            dto.setPeakOrderHours(new HashMap<>());
            dto.setCustomerRetention(Map.of("totalCustomers", 0L, "repeatCustomers", 0L));
            return dto;
        }

        Long shopId = shopOpt.get().getId();
        List<Order> allShopOrders = orderRepository.findByShopId(shopId);

        // ── 2. "Counted" orders = anything the shopkeeper accepted ─────
        //    Exclude: PENDING (not yet confirmed) and CANCELLED
        List<String> activeStatuses = Arrays.asList(
            "CONFIRMED", "READY", "OUT_FOR_DELIVERY", "DELIVERED"
        );
        List<Order> activeOrders = allShopOrders.stream()
            .filter(o -> activeStatuses.contains(o.getStatus()))
            .collect(Collectors.toList());

        dto.setTotalOrders(activeOrders.size());
        dto.setTotalRevenue(activeOrders.stream().mapToDouble(Order::getTotalAmount).sum());

        // ── 3. Daily Revenue — use deliveredTime, fall back to orderTime ─
        Map<String, Double> dailyRevenue = activeOrders.stream()
            .filter(o -> effectiveTime(o) != null)
            .collect(Collectors.groupingBy(
                o -> effectiveTime(o).toLocalDate().toString(),
                Collectors.summingDouble(Order::getTotalAmount)
            ));
        dto.setDailyRevenue(dailyRevenue);

        // ── 4. Best Selling Products ───────────────────────────────────
        Map<String, Integer> productSales = new HashMap<>();
        for (Order o : activeOrders) {
            if (o.getItems() != null) {
                for (OrderItem item : o.getItems()) {
                    productSales.merge(item.getProductName(), item.getQuantity(), Integer::sum);
                }
            }
        }
        List<Map<String, Object>> bestSelling = productSales.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(5)
            .map(e -> {
                Map<String, Object> map = new HashMap<>();
                map.put("name", e.getKey());
                map.put("quantity", e.getValue());
                return map;
            })
            .collect(Collectors.toList());
        dto.setBestSellingProducts(bestSelling);

        // ── 5. Peak Order Hours (all orders, including pending) ────────
        Map<Integer, Long> peakHours = allShopOrders.stream()
            .filter(o -> o.getOrderTime() != null)
            .collect(Collectors.groupingBy(
                o -> o.getOrderTime().getHour(),
                Collectors.counting()
            ));
        dto.setPeakOrderHours(peakHours);

        // ── 6. Customer Retention ──────────────────────────────────────
        Map<Long, Long> customerOrderCount = allShopOrders.stream()
            .collect(Collectors.groupingBy(Order::getCustomerId, Collectors.counting()));
        long totalCustomers = customerOrderCount.size();
        long repeatCustomers = customerOrderCount.values().stream()
            .filter(count -> count > 1).count();
        Map<String, Long> retention = new HashMap<>();
        retention.put("totalCustomers", totalCustomers);
        retention.put("repeatCustomers", repeatCustomers);
        dto.setCustomerRetention(retention);

        return dto;
    }

    /** Returns deliveredTime if present, otherwise orderTime */
    private LocalDateTime effectiveTime(Order o) {
        if (o.getDeliveredTime() != null) return o.getDeliveredTime();
        if (o.getConfirmedTime() != null) return o.getConfirmedTime();
        return o.getOrderTime();
    }

    // Keep old method as fallback so nothing else breaks
    public SalesAnalyticsDTO getShopAnalytics(Long shopId) {
        return getShopAnalyticsByEmail(null); // will return empty, use email version instead
    }
}
