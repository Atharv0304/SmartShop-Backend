package com.smartstore.repository;

import com.smartstore.model.ShopDeliveryConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopDeliveryConnectionRepository extends JpaRepository<ShopDeliveryConnection, Long> {
    List<ShopDeliveryConnection> findByShopId(Long shopId);
    List<ShopDeliveryConnection> findByDeliveryBoyId(Long deliveryBoyId);
    List<ShopDeliveryConnection> findByShopIdAndStatus(Long shopId, String status);
    boolean existsByShopIdAndDeliveryBoyId(Long shopId, Long deliveryBoyId);
}
