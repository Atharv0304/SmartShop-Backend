package com.smartstore.service;

import com.smartstore.dto.SalesAnalyticsDTO;
import com.smartstore.model.Order;
import com.smartstore.model.OrderItem;
import com.smartstore.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private OrderRepository orderRepository;

    public SalesAnalyticsDTO getShopAnalytics(Long shopId) {
        List<Order> orders = orderRepository.findByShopId(shopId).stream()
                .filter(o -> "DELIVERED".equals(o.getStatus()))
                .collect(Collectors.toList());

        SalesAnalyticsDTO dto = new SalesAnalyticsDTO();
        dto.setTotalOrders(orders.size());
        dto.setTotalRevenue(orders.stream().mapToDouble(Order::getTotalAmount).sum());

        // Daily Revenue
        Map<String, Double> dailyRevenue = orders.stream()
                .filter(o -> o.getDeliveredTime() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getDeliveredTime().toLocalDate().toString(),
                        Collectors.summingDouble(Order::getTotalAmount)
                ));
        dto.setDailyRevenue(dailyRevenue);

        // Best Selling Products
        Map<String, Integer> productSales = new HashMap<>();
        for (Order o : orders) {
            if (o.getItems() != null) {
                for (OrderItem item : o.getItems()) {
                    productSales.put(item.getProductName(),
                            productSales.getOrDefault(item.getProductName(), 0) + item.getQuantity());
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

        // Peak Order Hours (Using orderTime)
        List<Order> allShopOrders = orderRepository.findByShopId(shopId);
        Map<Integer, Long> peakHours = allShopOrders.stream()
                .filter(o -> o.getOrderTime() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getOrderTime().getHour(),
                        Collectors.counting()
                ));
        dto.setPeakOrderHours(peakHours);

        // Customer Retention
        Map<Long, Long> customerOrderCount = allShopOrders.stream()
                .collect(Collectors.groupingBy(Order::getCustomerId, Collectors.counting()));
        
        long totalCustomers = customerOrderCount.size();
        long repeatCustomers = customerOrderCount.values().stream().filter(count -> count > 1).count();
        
        Map<String, Long> retention = new HashMap<>();
        retention.put("totalCustomers", totalCustomers);
        retention.put("repeatCustomers", repeatCustomers);
        dto.setCustomerRetention(retention);

        return dto;
    }
}
