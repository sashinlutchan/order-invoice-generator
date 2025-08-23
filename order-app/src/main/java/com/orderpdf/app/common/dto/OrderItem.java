package com.orderpdf.app.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderItem(
        @JsonProperty("pk") String pk,
        @JsonProperty("sk") String sk,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("oldPdfKey") String oldPdfKey) {
}