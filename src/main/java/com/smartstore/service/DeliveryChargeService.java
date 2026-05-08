package com.smartstore.service;

import org.springframework.stereotype.Service;

@Service
public class DeliveryChargeService {

    private static final double BASE_FARE = 0.0;
    private static final double PRICE_PER_KM = 5.0;
    private static final double FREE_DISTANCE = 0.0;

    // Haversine formula
    public double calculateDistance(double lat1, double lon1,
                                    double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return Math.round(R * c * 10.0) / 10.0;
    }

    // Calculate delivery charge
    public double calculateCharge(double distanceKm) {
        if (distanceKm <= FREE_DISTANCE) return BASE_FARE;
        double extraKm = distanceKm - FREE_DISTANCE;
        return BASE_FARE + (extraKm * PRICE_PER_KM);
    }

    // Get pricing breakdown
    public java.util.Map<String, Object> getPricingBreakdown(
            double distanceKm) {
        double charge = calculateCharge(distanceKm);
        java.util.Map<String, Object> breakdown = new java.util.HashMap<>();
        breakdown.put("distanceKm", distanceKm);
        breakdown.put("baseFare", BASE_FARE);
        breakdown.put("pricePerKm", PRICE_PER_KM);
        breakdown.put("deliveryCharge", Math.round(charge));
        breakdown.put("freeDistance", FREE_DISTANCE);
        return breakdown;
    }
}