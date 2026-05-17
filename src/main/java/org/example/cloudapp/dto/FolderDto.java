package org.example.cloudapp.dto;

import java.io.Serializable;
import java.time.Instant;

public record FolderDto(
        Long id,
        String name,
        Long parentId,
        boolean root,
        Instant updatedAt,
        long childCount
) implements Serializable {
}
