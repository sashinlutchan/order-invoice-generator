package com.orderpdf.app.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Customer(
                @JsonProperty("name") String name,
                @JsonProperty("email") String email,
                @JsonProperty("phone") String phone,
                @JsonProperty("address") String address) {
}