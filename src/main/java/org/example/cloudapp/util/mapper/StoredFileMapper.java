package org.example.cloudapp.util.mapper;

import org.example.cloudapp.dto.StoredFileDto;
import org.example.cloudapp.entity.FileScanResult;
import org.example.cloudapp.entity.FileScanStatus;
import org.example.cloudapp.entity.StoredFile;
import org.springframework.stereotype.Component;

@Component
public class StoredFileMapper {
    public StoredFileDto toDto(StoredFile file) {
        FileScanResult scanResult = file.getScanResult();
        FileScanStatus status = scanResult == null ? FileScanStatus.PENDING : scanResult.getStatus();
        return new StoredFileDto(
                file.getId(),
                file.getDisplayName(),
                file.getOriginalName(),
                file.getContentType(),
                file.getExtension(),
                file.getSize(),
                file.getCreatedAt(),
                file.getUpdatedAt(),
                status.name(),
                scanResult == null ? "Проверка еще не выполнена" : scanResult.getMessage(),
                scanResult == null ? null : scanResult.getThreats(),
                status == FileScanStatus.CLEAN
        );
    }
}
