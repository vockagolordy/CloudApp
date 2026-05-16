package org.example.cloudapp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.cloudapp.dto.FolderDto;
import org.example.cloudapp.dto.FolderPageDto;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.User;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.repository.FolderRepository;
import org.example.cloudapp.util.mapper.FolderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FolderService {
    private final FolderRepository folderRepository;
    private final FolderMapper folderMapper;

    public FolderService(FolderRepository folderRepository, FolderMapper folderMapper) {
        this.folderRepository = folderRepository;
        this.folderMapper = folderMapper;
    }

    @Transactional
    public Folder getOrCreateRoot(User owner) {
        return folderRepository.findByOwnerAndRootTrue(owner)
                .orElseGet(() -> {
                    Folder root = new Folder();
                    root.setName("Мои файлы");
                    root.setOwner(owner);
                    root.setRoot(true);
                    return folderRepository.save(root);
                });
    }

    @Transactional(readOnly = true)
    public FolderPageDto getFolderPage(Long folderId, User user) {
        Folder current = getOwnedFolder(folderId, user);
        List<FolderDto> children = folderRepository.findByOwnerAndParentOrderByNameAsc(user, current)
                .stream()
                .map(folderMapper::toDto)
                .toList();

        FolderDto parent = current.getParent() == null ? null : folderMapper.toDto(current.getParent());
        return new FolderPageDto(folderMapper.toDto(current), parent, breadcrumbs(current), children);
    }

    @Transactional
    public FolderDto createFolder(Long parentId, String name, User user) {
        Folder parent = getOwnedFolder(parentId, user);
        String normalizedName = normalizeName(name);
        ensureUniqueName(user, parent, normalizedName, null);

        Folder folder = new Folder();
        folder.setName(normalizedName);
        folder.setOwner(user);
        folder.setParent(parent);
        folder.setRoot(false);
        return folderMapper.toDto(folderRepository.save(folder));
    }

    @Transactional
    public FolderDto renameFolder(Long folderId, String name, User user) {
        Folder folder = getOwnedFolder(folderId, user);
        if (folder.isRoot()) {
            throw new AppException("Корневую папку нельзя переименовать");
        }

        String normalizedName = normalizeName(name);
        ensureUniqueName(user, folder.getParent(), normalizedName, folder.getId());
        folder.setName(normalizedName);
        return folderMapper.toDto(folder);
    }

    @Transactional
    public Long deleteFolder(Long folderId, User user) {
        Folder folder = getOwnedFolder(folderId, user);
        if (folder.isRoot()) {
            throw new AppException("Корневую папку нельзя удалить");
        }

        Long parentId = folder.getParent().getId();
        deleteSubtree(folder, user);
        return parentId;
    }

    private void deleteSubtree(Folder folder, User user) {
        List<Folder> children = folderRepository.findByOwnerAndParentOrderByNameAsc(user, folder);
        for (Folder child : children) {
            deleteSubtree(child, user);
        }
        folderRepository.delete(folder);
    }

    private Folder getOwnedFolder(Long folderId, User user) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new AppException("Папка не найдена"));
        if (!folder.getOwner().getId().equals(user.getId())) {
            throw new AppException("Нет доступа к этой папке");
        }
        return folder;
    }

    private List<FolderDto> breadcrumbs(Folder folder) {
        List<FolderDto> result = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            result.add(folderMapper.toDto(current));
            current = current.getParent();
        }
        Collections.reverse(result);
        return result;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private void ensureUniqueName(User owner, Folder parent, String name, Long currentFolderId) {
        boolean duplicate = folderRepository.existsByOwnerAndParentAndNameIgnoreCase(owner, parent, name);
        if (!duplicate) {
            return;
        }
        if (currentFolderId == null) {
            throw new AppException("Папка с таким названием уже есть");
        }
        folderRepository.findByOwnerAndParentOrderByNameAsc(owner, parent).stream()
                .filter(folder -> folder.getName().equalsIgnoreCase(name))
                .filter(folder -> !folder.getId().equals(currentFolderId))
                .findAny()
                .ifPresent(folder -> {
                    throw new AppException("Папка с таким названием уже есть");
                });
    }
}
