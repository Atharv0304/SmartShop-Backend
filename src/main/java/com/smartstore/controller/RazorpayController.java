package com.smartstore.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class RazorpayController {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /**
     * Step 1: Create a Razorpay order.
     * Called by frontend before opening the Razorpay Checkout modal.
     *
     * Request body: { "amount": 50000, "currency": "INR", "receipt": "order_rcpt_123" }
     * amount must be in PAISE (₹500 = 50000 paise)
     *
     * Returns: { "orderId": "order_XXXX", "amount": 50000, "currency": "INR", "keyId": "rzp_test_XXXX" }
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            int amountInPaise = ((Number) body.get("amount")).intValue();
            String currency = body.getOrDefault("currency", "INR").toString();
            String receipt = body.getOrDefault("receipt", "receipt_" + System.currentTimeMillis()).toString();

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", currency);
            options.put("receipt", receipt);
            options.put("payment_capture", 1); // auto-capture payment

            Order razorpayOrder = client.orders.create(options);

            // Use HashMap<String,Object> — Map.of() causes type inference to
            // pick String for V, then Razorpay's Integer 'amount' fails to cast
            Map<String, Object> response = new HashMap<>();
            response.put("orderId",  razorpayOrder.get("id").toString());
            response.put("amount",   ((Number) razorpayOrder.get("amount")).intValue());
            response.put("currency", razorpayOrder.get("currency").toString());
            response.put("keyId",    keyId);
            return ResponseEntity.ok(response);

        } catch (RazorpayException e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to create Razorpay order: " + e.getMessage()
            ));
        }
    }

    /**
     * Step 2: Verify the payment signature after Razorpay Checkout completes.
     * This is the critical security step — ensures the payment was not tampered with.
     *
     * Request body: {
     *   "razorpay_order_id":   "order_XXXX",
     *   "razorpay_payment_id": "pay_XXXX",
     *   "razorpay_signature":  "HMAC_SHA256_hash"
     * }
     *
     * Returns: { "success": true } or 400 Bad Request
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {
        try {
            String orderId   = body.get("razorpay_order_id");
            String paymentId = body.get("razorpay_payment_id");
            String signature = body.get("razorpay_signature");

            if (orderId == null || paymentId == null || signature == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields: razorpay_order_id, razorpay_payment_id, razorpay_signature"
                ));
            }

            // Razorpay signature = HMAC_SHA256(razorpay_order_id + "|" + razorpay_payment_id, keySecret)
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(attributes, keySecret);

            if (isValid) {
                return ResponseEntity.ok(Map.of(
                    "success",   true,
                    "paymentId", paymentId,
                    "orderId",   orderId
                ));
            } else {
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Payment signature verification failed. Possible tampered request."
                ));
            }

        } catch (RazorpayException e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", "Signature verification error: " + e.getMessage()
            ));
        }
    }
}
