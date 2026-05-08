package com.smartstore.service;

import com.smartstore.model.Customer;
import com.smartstore.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public boolean emailExists(String email) {
        return customerRepository.findByEmail(email).isPresent();
    }

    public Customer register(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer login(String email, String password) {
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent() && customer.get().getPassword().equals(password)) {
            return customer.get();
        }
        return null;
    }

    public boolean deleteCustomer(Long id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return true;
        }
        return false;
    }
}