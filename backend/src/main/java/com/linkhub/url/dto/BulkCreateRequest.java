package com.linkhub.url.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkCreateRequest(

        @NotEmpty(message = "URL list must not be empty")
        @Size(max = 20, message = "Maximum 20 URLs per bulk request")
        List<@Valid CreateUrlRequest> urls
) {}
