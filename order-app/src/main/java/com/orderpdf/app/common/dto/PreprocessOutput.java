package com.orderpdf.app.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record PreprocessOutput(
    @JsonProperty("items") List<OrderItem> items,
    @JsonProperty("ts") Instant timestamp
) {
}