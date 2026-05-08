package com.smartstore.controller;

import com.smartstore.model.Dispute;
import com.smartstore.repository.DisputeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disputes")
@CrossOrigin(origins = "http://localhost:3000")
public class DisputeController {

    @Autowired
    private DisputeRepository disputeRepository;

    @PostMapping
    public ResponseEntity<?> createDispute(@RequestBody Dispute dispute) {
        dispute.setStatus("OPEN");
        Dispute saved = disputeRepository.save(dispute);
        return ResponseEntity.ok(saved);
    }
}
