package com.orderpdf.app.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderLine(
    @JsonProperty("sku") String sku,
    @JsonProperty("qty") int quantity,
    @JsonProperty("priceMinor") long priceMinor
) {
}