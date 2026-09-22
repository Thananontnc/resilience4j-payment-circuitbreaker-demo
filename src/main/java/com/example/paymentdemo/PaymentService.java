package com.example.paymentdemo;

import com.example.paymentdemo.dto.PaymentRequest;
import com.example.paymentdemo.dto.PaymentResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    public static final String CIRCUIT_BREAKER_NAME = "paymentService";

    private final PaymentGatewayClient paymentGatewayClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public PaymentService(PaymentGatewayClient paymentGatewayClient,
                           CircuitBreakerRegistry circuitBreakerRegistry) {
        this.paymentGatewayClient = paymentGatewayClient;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    // Logs every CLOSED -> OPEN -> HALF_OPEN -> CLOSED transition to the console,
    // so the breaker's behaviour is visible while you demo it live.
    @PostConstruct
    void logStateTransitions() {
        io.github.resilience4j.circuitbreaker.CircuitBreaker breaker =
                circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        breaker.getEventPublisher().onStateTransition(event ->
                log.warn(">>> Circuit breaker '{}' changed state: {} -> {}",
                        CIRCUIT_BREAKER_NAME,
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackPayment")
    public PaymentResult charge(PaymentRequest request) {
        return paymentGatewayClient.charge(request);
    }

    // Called instantly when the breaker is OPEN, or whenever charge() throws.
    // Same method signature as the protected method, plus the Throwable.
    private PaymentResult fallbackPayment(PaymentRequest request, Throwable ex) {
        log.info("Falling back for order {} ({}: {})",
                request.getOrderId(), ex.getClass().getSimpleName(), ex.getMessage());
        return PaymentResult.queuedForRetry(request.getOrderId());
    }

    public String currentState() {
        return circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME).getState().name();
    }
}
