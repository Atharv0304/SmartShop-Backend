package com.smartstore.service;

import com.smartstore.model.Shopkeeper;
import com.smartstore.repository.ShopkeeperRepository;
import com.smartstore.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ShopkeeperService {

    @Autowired
    private ShopkeeperRepository shopkeeperRepository;

    @Autowired
    private ShopRepository shopRepository;

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

    // Delete Profile
    public boolean deleteShopkeeper(String email) {
        Optional<Shopkeeper> shopkeeperOpt = shopkeeperRepository.findByEmail(email);
        if (shopkeeperOpt.isPresent()) {
            shopkeeperRepository.delete(shopkeeperOpt.get());
            // Also delete the shop if it exists
            shopRepository.findByEmail(email).ifPresent(shop -> shopRepository.delete(shop));
            return true;
        }
        return false;
    }
}