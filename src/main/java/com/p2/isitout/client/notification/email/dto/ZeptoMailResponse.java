package com.p2.isitout.client.notification.email.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ZeptoMailResponse(
        List<Result> data,
        String message,
        @JsonProperty("request_id")
        String requestId,
        String object
) {

    public record Result(
            String code,
            @JsonProperty("additional_info")
            List<Object> additionalInfo,
            String message
    ) {
    }
}