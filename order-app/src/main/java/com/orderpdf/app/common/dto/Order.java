package com.orderpdf.app.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record Order(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("currency") String currency,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("customer") Customer customer,
        @JsonProperty("lines") List<OrderLine> lines,
        @JsonProperty("status") String status,
        @JsonProperty("notes") String notes,
        @JsonProperty("source") String source,
        @JsonProperty("priority") String priority,
        @JsonProperty("region") String region,
        @JsonProperty("totalAmount") Double totalAmount,
        @JsonProperty("orderDate") String orderDate,
        @JsonProperty("processingTime") Integer processingTime) {
}