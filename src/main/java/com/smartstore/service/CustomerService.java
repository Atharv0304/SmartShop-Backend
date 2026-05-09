package com.smartstore.service;

import com.smartstore.model.Customer;
import com.smartstore.model.Order;
import com.smartstore.repository.CustomerRepository;
import com.smartstore.repository.NotificationRepository;
import com.smartstore.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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

    @Transactional
    public boolean deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            return false;
        }

        // 1. Delete all orders belonging to this customer
        //    OrderItems are cascade-deleted automatically via @OneToMany(cascade = CascadeType.ALL)
        List<Order> orders = orderRepository.findByCustomerIdOrderByIdDesc(id);
        if (!orders.isEmpty()) {
            orderRepository.deleteAll(orders);
        }

        // 2. Delete all notifications for this customer
        notificationRepository.deleteByUserId(id);

        // 3. Delete the customer account itself
        customerRepository.deleteById(id);

        return true;
    }
}