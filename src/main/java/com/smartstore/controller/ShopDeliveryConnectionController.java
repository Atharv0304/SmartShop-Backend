package com.smartstore.controller;

import com.smartstore.model.ShopDeliveryConnection;
import com.smartstore.repository.ShopDeliveryConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/connections")
@CrossOrigin(origins = "*")
public class ShopDeliveryConnectionController {

    @Autowired
    private ShopDeliveryConnectionRepository connectionRepository;

    @PostMapping("/request")
    public ResponseEntity<?> requestConnection(@RequestBody ShopDeliveryConnection connection) {
        if (connectionRepository.existsByShopIdAndDeliveryBoyId(connection.getShopId(), connection.getDeliveryBoyId())) {
            return ResponseEntity.badRequest().body("Request already exists");
        }
        connection.setStatus("PENDING");
        connection.setRequestedAt(LocalDateTime.now());
        ShopDeliveryConnection saved = connectionRepository.save(connection);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/shop/{shopId}")
    public List<ShopDeliveryConnection> getConnectionsForShop(@PathVariable Long shopId) {
        return connectionRepository.findByShopId(shopId);
    }

    @GetMapping("/delivery/{deliveryBoyId}")
    public List<ShopDeliveryConnection> getConnectionsForDeliveryBoy(@PathVariable Long deliveryBoyId) {
        return connectionRepository.findByDeliveryBoyId(deliveryBoyId);
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveConnection(@PathVariable Long id) {
        ShopDeliveryConnection conn = connectionRepository.findById(id).orElseThrow();
        conn.setStatus("APPROVED");
        connectionRepository.save(conn);
        return ResponseEntity.ok(conn);
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectConnection(@PathVariable Long id) {
        ShopDeliveryConnection conn = connectionRepository.findById(id).orElseThrow();
        conn.setStatus("REJECTED");
        connectionRepository.save(conn);
        return ResponseEntity.ok(conn);
    }
}
