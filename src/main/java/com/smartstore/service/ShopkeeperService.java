package com.smartstore.service;

import com.smartstore.model.Shopkeeper;
import com.smartstore.repository.NotificationRepository;
import com.smartstore.repository.OrderRepository;
import com.smartstore.repository.ProductRepository;
import com.smartstore.repository.ShopkeeperRepository;
import com.smartstore.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class ShopkeeperService {

    @Autowired
    private ShopkeeperRepository shopkeeperRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // Check if email exists
    public boolean emailExists(String email) {
        return shopkeeperRepository.findByEmail(email).isPresent();
    }

    // Register
    public Shopkeeper register(Shopkeeper shopkeeper) {
        return shopkeeperRepository.save(shopkeeper);
    }

    // Login
    public Shopkeeper login(String email, String password) {
        Optional<Shopkeeper> shopkeeper = shopkeeperRepository.findByEmail(email);
        if (shopkeeper.isPresent() && shopkeeper.get().getPassword().equals(password)) {
            return shopkeeper.get();
        }
        return null;
    }

    // Get Profile
    public Shopkeeper getProfile(String email) {
        return shopkeeperRepository.findByEmail(email).orElse(null);
    }

    // Update Profile
    public Shopkeeper updateShopkeeper(String email, Shopkeeper updated) {
        Optional<Shopkeeper> opt = shopkeeperRepository.findByEmail(email);
        if (opt.isPresent()) {
            Shopkeeper existing = opt.get();
            if (updated.getName() != null) existing.setName(updated.getName());
            if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
            if (updated.getShopName() != null) existing.setShopName(updated.getShopName());
            if (updated.getUpiId() != null) existing.setUpiId(updated.getUpiId());
            return shopkeeperRepository.save(existing);
        }
        return null;
    }

    // Delete Profile — cascade delete products, shop, orders, notifications
    @Transactional
    public boolean deleteShopkeeper(String email) {
        Optional<Shopkeeper> shopkeeperOpt = shopkeeperRepository.findByEmail(email);
        if (!shopkeeperOpt.isPresent()) {
            return false;
        }

        Shopkeeper shopkeeper = shopkeeperOpt.get();

        // 1. Delete all products belonging to this shopkeeper (linked by email)
        java.util.List<com.smartstore.model.Product> products = productRepository.findByShopkeeperEmail(email);
        if (!products.isEmpty()) {
            productRepository.deleteAll(products);
        }

        // 2. Delete the shop (DO NOT delete customer orders — they are the customer's history)
        shopRepository.findByEmail(email).ifPresent(shop -> shopRepository.delete(shop));

        // 3. Delete shopkeeper notifications
        notificationRepository.deleteByUserId(shopkeeper.getId());

        // 4. Delete the shopkeeper account
        shopkeeperRepository.delete(shopkeeper);

        return true;
    }
}