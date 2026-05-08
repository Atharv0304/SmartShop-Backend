package com.smartstore.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Customer Info
    private Long customerId;
    private String customerName;
    private String customerPhone;

    // Shop Info
    private Long shopId;
    private String shopName;

    // Delivery Boy Info
    private Long deliveryBoyId;
    private String deliveryBoyName;
    private String deliveryBoyPhone;
    private String deliveryBoyVehicle;

    // Order Details
    private double totalAmount;
    private double deliveryCharge;
    private double distanceKm;
    private String status;
    private String paymentMethod;
    private String razorpayOrderId;   // Razorpay order ID (e.g. order_XXXX)
    private String paymentStatus;     // PENDING | PAID | FAILED
    private String deliveryType;
    private String deliveryAddress;
    private double deliveryLatitude;
    private double deliveryLongitude;

    // OTP Verification
    private String deliveryOtp;
    private boolean otpVerified;
    private LocalDateTime otpExpiresAt;
    private int otpRetryCount;
    
 // Shop pickup OTP (given to delivery boy to collect from shop)
    private String shopOtp;
    private boolean shopOtpVerified;

    // Timestamps for each stage
    private LocalDateTime orderTime;
    private LocalDateTime confirmedTime;
    private LocalDateTime readyTime;
    private LocalDateTime assignedTime;
    private LocalDateTime pickedTime;
    private LocalDateTime outForDeliveryTime;
    private LocalDateTime deliveredTime;

    // Role
    private String role;

    // Cancellation
    private String cancelReason;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items;

    // Getters and Setters
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getShopOtp() { return shopOtp; }
    public void setShopOtp(String shopOtp) { this.shopOtp = shopOtp; }
    public boolean isShopOtpVerified() { return shopOtpVerified; }
    public void setShopOtpVerified(boolean shopOtpVerified) { 
        this.shopOtpVerified = shopOtpVerified; 
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public Long getDeliveryBoyId() { return deliveryBoyId; }
    public void setDeliveryBoyId(Long deliveryBoyId) { this.deliveryBoyId = deliveryBoyId; }
    public String getDeliveryBoyName() { return deliveryBoyName; }
    public void setDeliveryBoyName(String deliveryBoyName) { this.deliveryBoyName = deliveryBoyName; }
    public String getDeliveryBoyPhone() { return deliveryBoyPhone; }
    public void setDeliveryBoyPhone(String deliveryBoyPhone) { this.deliveryBoyPhone = deliveryBoyPhone; }
    public String getDeliveryBoyVehicle() { return deliveryBoyVehicle; }
    public void setDeliveryBoyVehicle(String deliveryBoyVehicle) { this.deliveryBoyVehicle = deliveryBoyVehicle; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getDeliveryCharge() { return deliveryCharge; }
    public void setDeliveryCharge(double deliveryCharge) { this.deliveryCharge = deliveryCharge; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public double getDeliveryLatitude() { return deliveryLatitude; }
    public void setDeliveryLatitude(double deliveryLatitude) { this.deliveryLatitude = deliveryLatitude; }
    public double getDeliveryLongitude() { return deliveryLongitude; }
    public void setDeliveryLongitude(double deliveryLongitude) { this.deliveryLongitude = deliveryLongitude; }
    public String getDeliveryOtp() { return deliveryOtp; }
    public void setDeliveryOtp(String deliveryOtp) { this.deliveryOtp = deliveryOtp; }
    public boolean isOtpVerified() { return otpVerified; }
    public void setOtpVerified(boolean otpVerified) { this.otpVerified = otpVerified; }
    public LocalDateTime getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }
    public int getOtpRetryCount() { return otpRetryCount; }
    public void setOtpRetryCount(int otpRetryCount) { this.otpRetryCount = otpRetryCount; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
    public LocalDateTime getConfirmedTime() { return confirmedTime; }
    public void setConfirmedTime(LocalDateTime confirmedTime) { this.confirmedTime = confirmedTime; }
    public LocalDateTime getReadyTime() { return readyTime; }
    public void setReadyTime(LocalDateTime readyTime) { this.readyTime = readyTime; }
    public LocalDateTime getAssignedTime() { return assignedTime; }
    public void setAssignedTime(LocalDateTime assignedTime) { this.assignedTime = assignedTime; }
    public LocalDateTime getPickedTime() { return pickedTime; }
    public void setPickedTime(LocalDateTime pickedTime) { this.pickedTime = pickedTime; }
    public LocalDateTime getOutForDeliveryTime() { return outForDeliveryTime; }
    public void setOutForDeliveryTime(LocalDateTime outForDeliveryTime) { this.outForDeliveryTime = outForDeliveryTime; }
    public LocalDateTime getDeliveredTime() { return deliveredTime; }
    public void setDeliveredTime(LocalDateTime deliveredTime) { this.deliveredTime = deliveredTime; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}