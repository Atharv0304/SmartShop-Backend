package com.smartstore.service;

import com.smartstore.model.Product;
import com.smartstore.repository.ProductRepository;
import com.smartstore.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopRepository shopRepository;

    // Add product
    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    // Get all products (filtered by registered shops)
    public List<Product> getAllProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getShopkeeperEmail() != null && 
                             shopRepository.findByEmail(p.getShopkeeperEmail()).isPresent())
                .toList();
    }

    // Get products by shopkeeper
    public List<Product> getProductsByShopkeeper(String email) {
        return productRepository.findByShopkeeperEmail(email);
    }

    // Delete product
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
    public List<Product> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCase(query).stream()
                .filter(p -> p.getShopkeeperEmail() != null && 
                             shopRepository.findByEmail(p.getShopkeeperEmail()).isPresent())
                .toList();
    }

    // Update product
    public Product updateProduct(Long id, Product updatedProduct) {
        Product existing = productRepository.findById(id).orElseThrow();
        existing.setName(updatedProduct.getName());
        existing.setBarcode(updatedProduct.getBarcode());
        existing.setCategory(updatedProduct.getCategory());
        existing.setExpiryDate(updatedProduct.getExpiryDate());
        existing.setQuantity(updatedProduct.getQuantity());
        existing.setPrice(updatedProduct.getPrice());
        return productRepository.save(existing);
    }
}