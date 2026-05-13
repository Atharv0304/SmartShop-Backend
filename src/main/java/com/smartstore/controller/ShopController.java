package com.smartstore.controller;

import com.smartstore.model.Shop;
import com.smartstore.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shops")
@CrossOrigin(origins = "*")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @PostMapping("/register")
    public Shop registerShop(@RequestBody Shop shop) {
        return shopService.registerShop(shop);
    }

    @GetMapping("/all")
    public List<Shop> getAllShops() {
        return shopService.getAllShops();
     // testing cicd
    }
}
