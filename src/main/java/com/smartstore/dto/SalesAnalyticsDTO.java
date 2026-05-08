package com.smartstore.dto;

import java.util.List;
import java.util.Map;

public class SalesAnalyticsDTO {

    private Map<String, Double> dailyRevenue;
    private List<Map<String, Object>> bestSellingProducts;
    private Map<Integer, Long> peakOrderHours;
    private Map<String, Long> customerRetention;
    private double totalRevenue;
    private long totalOrders;

    public Map<String, Double> getDailyRevenue() {
        return dailyRevenue;
    }

    public void setDailyRevenue(Map<String, Double> dailyRevenue) {
        this.dailyRevenue = dailyRevenue;
    }

    public List<Map<String, Object>> getBestSellingProducts() {
        return bestSellingProducts;
    }

    public void setBestSellingProducts(List<Map<String, Object>> bestSellingProducts) {
        this.bestSellingProducts = bestSellingProducts;
    }

    public Map<Integer, Long> getPeakOrderHours() {
        return peakOrderHours;
    }

    public void setPeakOrderHours(Map<Integer, Long> peakOrderHours) {
        this.peakOrderHours = peakOrderHours;
    }

    public Map<String, Long> getCustomerRetention() {
        return customerRetention;
    }

    public void setCustomerRetention(Map<String, Long> customerRetention) {
        this.customerRetention = customerRetention;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }
}
