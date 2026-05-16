package org.example.cloudapp.dto;

import java.util.List;

public record FolderPageDto(
        FolderDto current,
        FolderDto parent,
        List<FolderDto> breadcrumbs,
        List<FolderDto> children,
        List<StoredFileDto> files,
        boolean canEditCurrent,
        boolean canDeleteCurrent
) {
}
