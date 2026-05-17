package org.example.cloudapp.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorDto(
        String message,
        String path,
        int status,
        Instant timestamp,
        List<String> details
) {
}
