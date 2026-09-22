package com.example.paymentdemo;

import com.example.paymentdemo.dto.PaymentRequest;
import com.example.paymentdemo.dto.PaymentResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // POST /api/payments  { "orderId": "ORD-1", "amount": 49.90 }
    @PostMapping
    public PaymentResult charge(@RequestBody PaymentRequest request) {
        return paymentService.charge(request);
    }

    // GET /api/payments/circuit-status  -> { "state": "CLOSED" }
    // Poll this while you demo to watch CLOSED -> OPEN -> HALF_OPEN -> CLOSED.
    @GetMapping("/circuit-status")
    public Map<String, String> circuitStatus() {
        return Map.of("state", paymentService.currentState());
    }
}
