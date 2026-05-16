package org.example.cloudapp.util.mapper;

import org.example.cloudapp.dto.StoredFileDto;
import org.example.cloudapp.entity.StoredFile;
import org.springframework.stereotype.Component;

@Component
public class StoredFileMapper {
    public StoredFileDto toDto(StoredFile file) {
        return new StoredFileDto(
                file.getId(),
                file.getDisplayName(),
                file.getOriginalName(),
                file.getContentType(),
                file.getExtension(),
                file.getSize(),
                file.getUpdatedAt()
        );
    }
}
