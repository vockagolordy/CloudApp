package org.example.cloudapp.util.mapper;

import org.example.cloudapp.dto.FolderDto;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.repository.FolderRepository;
import org.springframework.stereotype.Component;

@Component
public class FolderMapper {
    private final FolderRepository folderRepository;

    public FolderMapper(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }

    public FolderDto toDto(Folder folder) {
        Long parentId = folder.getParent() == null ? null : folder.getParent().getId();
        return new FolderDto(
                folder.getId(),
                folder.getName(),
                parentId,
                folder.isRoot(),
                folder.getUpdatedAt(),
                folderRepository.countByParent(folder)
        );
    }
}
