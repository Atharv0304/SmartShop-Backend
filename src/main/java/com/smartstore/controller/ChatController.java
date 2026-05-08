package com.smartstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstore.model.Order;
import com.smartstore.repository.OrderRepository;
import com.smartstore.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {

    @Value("${spring.ai.openai.api-key}")   private String apiKey;
    @Value("${spring.ai.openai.base-url}")  private String baseUrl;
    @Value("${spring.ai.openai.chat.options.model}") private String model;

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderService orderService;

    private final ObjectMapper mapper = new ObjectMapper();

    // ─── Intent classifier system prompt ────────────────────────────────────
    private static final String INTENT_PROMPT =
        "You are an intent classifier for SmartStore app. Return ONLY valid JSON, no markdown.\n" +
        "Intents:\n" +
        " TRACK_ORDER  – user wants to check status of their order\n" +
        " CANCEL_ORDER – user wants to cancel an order\n" +
        " LIST_ORDERS  – user wants to see their recent orders\n" +
        " GENERAL      – any other question\n" +
        "Examples:\n" +
        "{\"intent\":\"TRACK_ORDER\",\"orderId\":42}\n" +
        "{\"intent\":\"CANCEL_ORDER\",\"orderId\":null}\n" +
        "{\"intent\":\"LIST_ORDERS\"}\n" +
        "{\"intent\":\"GENERAL\"}";

    // ─── General chat (existing endpoint, unchanged) ─────────────────────────
    @PostMapping
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> payload) {
        try {
            String userMessage = (String) payload.get("message");
            String systemPrompt = payload.containsKey("systemPrompt")
                    ? (String) payload.get("systemPrompt") : "You are SmartBot for SmartStore.";
            List<Map<String, String>> messages = buildMessages(systemPrompt, payload.get("history"), userMessage);
            String reply = callLlm(messages);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("reply", "Sorry, error! 🙏"));
        }
    }

    // ─── Smart chat: LLM intent → real DB → natural response ────────────────
    @PostMapping("/smart")
    public ResponseEntity<?> smartChat(@RequestBody Map<String, Object> payload) {
        try {
            String userMessage  = (String) payload.get("message");
            String role         = (String) payload.getOrDefault("role", "CUSTOMER");
            String systemPrompt = (String) payload.getOrDefault("systemPrompt", "You are SmartBot for SmartStore.");
            String intentHint   = (String) payload.get("intentHint");

            Long customerId = toLong(payload.get("customerId"));
            Long shopId     = toLong(payload.get("shopId"));
            Long deliveryId = toLong(payload.get("deliveryBoyId"));

            // 1. Classify intent — handle CANCEL_REASON:orderId format from followUp
            String intent;
            Long orderId;
            Long pendingOrderId = toLong(payload.get("pendingOrderId"));

            if (intentHint != null && intentHint.startsWith("CANCEL_REASON:")) {
                intent         = "CANCEL_REASON";
                pendingOrderId = toLong(intentHint.split(":")[1]);
                orderId        = null;
            } else if (intentHint != null) {
                intent  = intentHint;
                orderId = extractNumber(userMessage);
            } else {
                Map<String, Object> cls = classifyIntent(userMessage);
                intent  = (String) cls.getOrDefault("intent", "GENERAL");
                orderId = cls.get("orderId") != null ? toLong(cls.get("orderId")) : null;
            }

            // 2. Execute real DB operation and build context
            String context  = "";
            String followUp = null;

            switch (intent) {
                case "TRACK_ORDER" -> {
                    if (orderId == null) {
                        followUp = "TRACK_ORDER";
                        context  = "User wants to track an order but did not provide an order ID. Ask for it.";
                    } else {
                        Optional<Order> opt = orderRepository.findById(orderId);
                        if (opt.isEmpty()) {
                            context = "Order #" + orderId + " NOT FOUND in database. Tell user it doesn't exist.";
                        } else {
                            Order o = opt.get();
                            boolean wrongOwner =
                                (customerId != null && !o.getCustomerId().equals(customerId)) ||
                                (shopId     != null && !o.getShopId().equals(shopId))         ||
                                (deliveryId != null && o.getDeliveryBoyId() != null && !o.getDeliveryBoyId().equals(deliveryId));
                            if (wrongOwner) {
                                context = "Order #" + orderId + " does not belong to this user. Tell them the ID is wrong.";
                            } else {
                                context = orderSummary(o);
                            }
                        }
                    }
                }
                case "CANCEL_ORDER" -> {
                    if (orderId == null) {
                        followUp = "CANCEL_ORDER";
                        context  = "User wants to cancel an order but did not provide the order ID. Ask for it.";
                    } else if (customerId == null) {
                        context = "Cannot cancel: customer not identified. Tell user to log in.";
                    } else {
                        // First verify the order is cancellable before asking for reason
                        Optional<Order> preCheck = orderRepository.findById(orderId);
                        if (preCheck.isEmpty()) {
                            context = "Order #" + orderId + " NOT FOUND. Tell user it doesn't exist.";
                        } else {
                            Order o = preCheck.get();
                            if (!o.getCustomerId().equals(customerId)) {
                                context = "Order #" + orderId + " does not belong to this customer. Tell them the ID is wrong.";
                            } else if (List.of("DELIVERY_ACCEPTED","PICKED","OUT_FOR_DELIVERY","DELIVERED","CANCELLED")
                                           .contains(o.getStatus())) {
                                context = "CANCEL NOT ALLOWED for Order #" + orderId + ". Status is: " + o.getStatus() +
                                          ". Explain why cancellation is not possible at this stage.";
                            } else {
                                // Order is cancellable - ask for reason
                                followUp = "CANCEL_REASON:" + orderId;
                                context = "Order #" + orderId + " from " + o.getShopName() + " (Status: " + o.getStatus() + ") CAN be cancelled. " +
                                          "Ask the customer to choose a cancellation reason from these 4 options: " +
                                          "1) Changed my mind  2) Wrong items ordered  3) Found better price elsewhere  4) Order taking too long  5) Other. " +
                                          "Present them as numbered options and ask them to pick one or type their own reason.";
                            }
                        }
                    }
                }
                case "CANCEL_REASON" -> {
                    // intentHint is "CANCEL_REASON:orderId"
                    Long cancelOrderId = toLong(payload.get("pendingOrderId"));
                    if (cancelOrderId == null) {
                        context = "Missing order ID for cancellation.";
                    } else if (customerId == null) {
                        context = "Cannot cancel: customer not identified.";
                    } else {
                        try {
                            String reason = userMessage; // the reason the user typed
                            Order cancelled = orderService.cancelOrder(cancelOrderId, customerId, reason);
                            context = "SUCCESS: Order #" + cancelOrderId + " from " + cancelled.getShopName() +
                                      " (₹" + cancelled.getTotalAmount() + ") has been CANCELLED. Reason recorded: '" + reason + "'. " +
                                      "Notifications sent to customer, shopkeeper" +
                                      (cancelled.getDeliveryBoyId() != null ? ", and delivery partner" : "") +
                                      ". Confirm this warmly and reassure the customer.";
                        } catch (Exception e) {
                            context = "CANCEL FAILED: " + e.getMessage() + ". Relay this to the user.";
                        }
                    }
                }
                case "LIST_ORDERS" -> {
                    List<Order> orders = new ArrayList<>();
                    if      (customerId != null) orders = orderRepository.findByCustomerIdOrderByIdDesc(customerId);
                    else if (shopId     != null) orders = orderRepository.findByShopIdOrderByIdDesc(shopId);
                    else if (deliveryId != null) orders = orderRepository.findByDeliveryBoyIdOrderByIdDesc(deliveryId);

                    if (orders.isEmpty()) {
                        context = "No orders found for this user.";
                    } else {
                        orders.sort((a, b) -> b.getId().compareTo(a.getId()));
                        StringBuilder sb = new StringBuilder("Recent orders from database:\n");
                        orders.stream().limit(5).forEach(o ->
                            sb.append("• Order #").append(o.getId())
                              .append(" | ").append(o.getShopName())
                              .append(" | Status: ").append(o.getStatus())
                              .append(" | ₹").append(o.getTotalAmount()).append("\n"));
                        context = sb.toString();
                    }
                }
                default -> { /* GENERAL — no DB needed */ }
            }

            // 3. Generate natural response with real data context
            String augmentedSystem = systemPrompt +
                (context.isEmpty() ? "" : "\n\n[REAL DATABASE DATA - use this to answer accurately]:\n" + context);
            List<Map<String, String>> messages = buildMessages(augmentedSystem, payload.get("history"), userMessage);
            String reply = callLlm(messages);

            Map<String, Object> resp = new HashMap<>();
            resp.put("reply", reply);
            if (followUp != null) resp.put("followUp", followUp);
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("reply", "Sorry, I ran into an error. Please try again! 🙏"));
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> classifyIntent(String userMessage) {
        try {
            List<Map<String, String>> msgs = List.of(
                Map.of("role", "system", "content", INTENT_PROMPT),
                Map.of("role", "user",   "content", userMessage)
            );
            String json = callLlm(msgs);
            // Strip any accidental markdown fences
            json = json.replaceAll("```json|```", "").trim();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of("intent", "GENERAL");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> buildMessages(String system, Object history, String userMsg) {
        List<Map<String, String>> msgs = new ArrayList<>();
        msgs.add(Map.of("role", "system", "content", system));
        if (history instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    String r = (String) m.get("role"), c = (String) m.get("content");
                    if (r != null && c != null) msgs.add(Map.of("role", r, "content", c));
                }
            }
        }
        msgs.add(Map.of("role", "user", "content", userMsg));
        return msgs;
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private String callLlm(List<Map<String, String>> messages) {
        RestClient client = RestClient.create();
        Map<String, Object> body = Map.of("model", model, "messages", messages, "temperature", 0.7, "max_tokens", 512);
        Map response = client.post()
                .uri(baseUrl + "/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private String orderSummary(Order o) {
        return "Order #" + o.getId() + " FOUND in database:\n" +
               "Status: " + o.getStatus() + "\n" +
               "Shop: " + o.getShopName() + "\n" +
               "Total: ₹" + o.getTotalAmount() +
               (o.getDeliveryCharge() > 0 ? " + ₹" + Math.round(o.getDeliveryCharge()) + " delivery" : "") + "\n" +
               "Type: " + o.getDeliveryType() + "\n" +
               "Payment: " + o.getPaymentMethod() + "\n" +
               (o.getDeliveryBoyName() != null ? "Rider: " + o.getDeliveryBoyName() + " (" + o.getDeliveryBoyPhone() + ")\n" : "") +
               (o.getDeliveryAddress() != null ? "Address: " + o.getDeliveryAddress() + "\n" : "") +
               "Ordered at: " + o.getOrderTime() + "\n" +
               "Present this clearly and tell user what status means.";
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.valueOf(v.toString().split("\\.")[0]); } catch (Exception e) { return null; }
    }

    private Long extractNumber(String text) {
        if (text == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d+)\\b").matcher(text);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }
}
