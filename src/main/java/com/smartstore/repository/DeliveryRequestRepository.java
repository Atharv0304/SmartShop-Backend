package com.smartstore.repository;

import com.smartstore.model.DeliveryRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeliveryRequestRepository 
        extends JpaRepository<DeliveryRequest, Long> {
    List<DeliveryRequest> findByDeliveryBoyIdAndStatus(
        Long deliveryBoyId, String status);
    List<DeliveryRequest> findByOrderId(Long orderId);
    List<DeliveryRequest> findByDeliveryBoyId(Long deliveryBoyId);
    void deleteByDeliveryBoyId(Long deliveryBoyId);
    boolean existsByOrderIdAndStatus(Long orderId, String status);
}