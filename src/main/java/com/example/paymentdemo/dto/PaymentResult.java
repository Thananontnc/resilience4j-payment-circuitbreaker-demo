package com.example.paymentdemo.dto;

public class PaymentResult {

    private String orderId;
    private String status;   // CONFIRMED | QUEUED_FOR_RETRY
    private String message;

    public PaymentResult() {
    }

    public PaymentResult(String orderId, String status, String message) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
    }

    public static PaymentResult confirmed(String orderId) {
        return new PaymentResult(orderId, "CONFIRMED", "Payment processed successfully.");
    }

    public static PaymentResult queuedForRetry(String orderId) {
        return new PaymentResult(orderId, "QUEUED_FOR_RETRY",
                "Payment gateway is unavailable right now — your order is saved and will be "
                        + "charged automatically once the gateway recovers.");
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
