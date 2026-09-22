package com.example.paymentdemo;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Demo-only control panel for {@link PaymentGatewayClient}. This is what lets
 * you reproduce the presentation's scenario live: flip the gateway into a
 * slow/failing state, fire a few payments, and watch the circuit breaker trip.
 *
 * Not something you'd ship — in a real system this class doesn't exist and
 * the "outage" is whatever is actually wrong with the third-party provider.
 */
@RestController
@RequestMapping("/api/simulate")
public class GatewaySimulatorController {

    private final PaymentGatewayClient paymentGatewayClient;

    public GatewaySimulatorController(PaymentGatewayClient paymentGatewayClient) {
        this.paymentGatewayClient = paymentGatewayClient;
    }

    // POST /api/simulate/outage
    //   { "enabled": true, "delayMs": 4000, "failurePercentage": 100 }
    // delayMs models the gateway hanging; failurePercentage models how many
    // calls actually error out once the delay is over.
    @PostMapping("/outage")
    public Map<String, Object> simulateOutage(@RequestBody OutageRequest request) {
        paymentGatewayClient.simulateOutage(
                request.enabled(), request.delayMs(), request.failurePercentage());
        return Map.of(
                "outageEnabled", paymentGatewayClient.isOutageEnabled(),
                "delayMs", request.delayMs(),
                "failurePercentage", request.failurePercentage()
        );
    }

    // POST /api/simulate/recover  -> instantly restores the gateway to healthy
    @PostMapping("/recover")
    public Map<String, Object> recover() {
        paymentGatewayClient.simulateOutage(false, 0, 0);
        return Map.of("outageEnabled", false);
    }

    public record OutageRequest(boolean enabled, int delayMs, int failurePercentage) {
    }
}
