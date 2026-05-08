package com.smartstore.repository;

import com.smartstore.model.Shopkeeper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShopkeeperRepository extends JpaRepository<Shopkeeper, Long> {
    Optional<Shopkeeper> findByEmail(String email);
}