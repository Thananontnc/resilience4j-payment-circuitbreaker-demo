package com.example.paymentdemo;

import com.example.paymentdemo.dto.PaymentRequest;
import com.example.paymentdemo.dto.PaymentResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stand-in for the real third-party payment gateway.
 *
 * In real life this would be an HTTP call to an external provider (Stripe,
 * Omise, a bank's payment API, etc). For the demo it is a plain Java class
 * whose behaviour you can flip at runtime through {@link GatewaySimulatorController},
 * so you can reproduce the "gateway hangs for 30+ seconds" scenario from the
 * presentation on demand, live, without touching any external service.
 */
@Component
public class PaymentGatewayClient {

    // Toggle these through POST /api/simulate/outage to trigger the circuit breaker.
    private final AtomicBoolean outageEnabled = new AtomicBoolean(false);
    private final AtomicInteger delayMs = new AtomicInteger(0);
    private final AtomicInteger failurePercentage = new AtomicInteger(0);

    public PaymentResult charge(PaymentRequest request) {
        if (outageEnabled.get()) {
            sleep(delayMs.get());
            if (shouldFail()) {
                throw new PaymentGatewayException(
                        "Payment gateway timed out after " + delayMs.get() + " ms");
            }
        }
        return PaymentResult.confirmed(request.getOrderId());
    }

    public void simulateOutage(boolean enabled, int delayMs, int failurePercentage) {
        this.outageEnabled.set(enabled);
        this.delayMs.set(Math.max(0, delayMs));
        this.failurePercentage.set(Math.min(100, Math.max(0, failurePercentage)));
    }

    public boolean isOutageEnabled() {
        return outageEnabled.get();
    }

    private boolean shouldFail() {
        return Math.random() * 100 < failurePercentage.get();
    }

    private void sleep(int ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class PaymentGatewayException extends RuntimeException {
        public PaymentGatewayException(String message) {
            super(message);
        }
    }
}
