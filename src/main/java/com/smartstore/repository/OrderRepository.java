package com.smartstore.repository;

import com.smartstore.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerIdOrderByIdDesc(Long customerId);
    List<Order> findByShopIdOrderByIdDesc(Long shopId);
    List<Order> findByDeliveryBoyIdOrderByIdDesc(Long deliveryBoyId);
    List<Order> findByStatusOrderByIdDesc(String status);
    List<Order> findByStatusInOrderByIdDesc(List<String> statuses);

    // Aliases for ChatController
    default List<Order> findByCustomerId(Long customerId) {
        return findByCustomerIdOrderByIdDesc(customerId);
    }
    default List<Order> findByShopId(Long shopId) {
        return findByShopIdOrderByIdDesc(shopId);
    }
    default List<Order> findByDeliveryBoyId(Long deliveryBoyId) {
        return findByDeliveryBoyIdOrderByIdDesc(deliveryBoyId);
    }
}