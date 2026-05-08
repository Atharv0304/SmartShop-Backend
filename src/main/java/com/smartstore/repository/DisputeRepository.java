package com.smartstore.repository;

import com.smartstore.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    List<Dispute> findByStatusOrderByCreatedAtDesc(String status);
    List<Dispute> findByOrderId(Long orderId);
}
