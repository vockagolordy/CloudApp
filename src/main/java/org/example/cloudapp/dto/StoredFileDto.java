package org.example.cloudapp.dto;

import java.io.Serializable;
import java.time.Instant;

public record StoredFileDto(
        Long id,
        String displayName,
        String originalName,
        String contentType,
        String extension,
        long size,
        Instant updatedAt,
        String scanStatus,
        String scanMessage
) implements Serializable {
}
