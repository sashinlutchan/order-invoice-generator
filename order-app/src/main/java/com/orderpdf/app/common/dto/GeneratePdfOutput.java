package com.orderpdf.app.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeneratePdfOutput(
    @JsonProperty("tempPdfKey") String tempPdfKey
) {
}