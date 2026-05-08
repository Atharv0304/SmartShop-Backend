package com.smartstore.service;

import com.smartstore.model.DeliveryBoy;
import com.smartstore.repository.DeliveryBoyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryBoyService {

    @Autowired
    private DeliveryBoyRepository deliveryBoyRepository;

    public String validateRegistration(DeliveryBoy boy) {
        if (deliveryBoyRepository.existsByEmail(boy.getEmail()))
            return "Email already registered!";
        if (deliveryBoyRepository.existsByAadharNumber(boy.getAadharNumber()))
            return "Aadhar number already registered!";
        if (deliveryBoyRepository.existsByLicenseNumber(boy.getLicenseNumber()))
            return "License number already registered!";
        if (deliveryBoyRepository.existsByVehicleNumber(boy.getVehicleNumber()))
            return "Vehicle number already registered!";
        if (boy.getAge() < 18)
            return "Must be at least 18 years old!";
        return null;
    }

    public DeliveryBoy register(DeliveryBoy boy) {
        boy.setStatus("PENDING");
        boy.setEmailVerified(true);
        boy.setApproved(false);
        boy.setAvailable(false);
        boy.setTotalDeliveries(0);
        boy.setRating(0.0);
        return deliveryBoyRepository.save(boy);
    }

    public DeliveryBoy login(String email, String password) {
        Optional<DeliveryBoy> boy = deliveryBoyRepository.findByEmail(email);
        if (boy.isPresent() && boy.get().getPassword().equals(password)) {
            return boy.get();
        }
        return null;
    }

    public void verifyEmail(String email) {
        Optional<DeliveryBoy> boy = deliveryBoyRepository.findByEmail(email);
        boy.ifPresent(b -> {
            b.setEmailVerified(true);
            deliveryBoyRepository.save(b);
        });
    }

    public List<DeliveryBoy> getAllDeliveryBoys() {
        return deliveryBoyRepository.findAll();
    }

    public DeliveryBoy approve(Long id) {
        DeliveryBoy boy = deliveryBoyRepository.findById(id).orElseThrow();
        boy.setStatus("APPROVED");
        boy.setApproved(true);
        boy.setAvailable(true);
        return deliveryBoyRepository.save(boy);
    }

    public DeliveryBoy updateAvailability(Long id, boolean available) {
        DeliveryBoy boy = deliveryBoyRepository.findById(id).orElseThrow();
        boy.setAvailable(available);
        return deliveryBoyRepository.save(boy);
    }

    public DeliveryBoy reject(Long id) {
        DeliveryBoy boy = deliveryBoyRepository.findById(id).orElseThrow();
        boy.setStatus("REJECTED");
        return deliveryBoyRepository.save(boy);
    }

    public boolean deleteDeliveryBoy(Long id) {
        if (deliveryBoyRepository.existsById(id)) {
            deliveryBoyRepository.deleteById(id);
            return true;
        }
        return false;
    }
}