package org.example.cloudapp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.cloudapp.dto.FolderDto;
import org.example.cloudapp.dto.FolderPageDto;
import org.example.cloudapp.entity.AccessLevel;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.User;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.form.ShareForm;
import org.example.cloudapp.repository.FolderRepository;
import org.example.cloudapp.repository.StoredFileRepository;
import org.example.cloudapp.util.mapper.FolderMapper;
import org.example.cloudapp.util.mapper.StoredFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FolderService {
    private final FolderRepository folderRepository;
    private final FolderMapper folderMapper;
    private final StoredFileRepository storedFileRepository;
    private final StoredFileMapper storedFileMapper;
    private final StorageService storageService;
    private final AccessService accessService;
    private final UserService userService;

    public FolderService(FolderRepository folderRepository, FolderMapper folderMapper,
                         StoredFileRepository storedFileRepository, StoredFileMapper storedFileMapper,
                         StorageService storageService, AccessService accessService, UserService userService) {
        this.folderRepository = folderRepository;
        this.folderMapper = folderMapper;
        this.storedFileRepository = storedFileRepository;
        this.storedFileMapper = storedFileMapper;
        this.storageService = storageService;
        this.accessService = accessService;
        this.userService = userService;
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
        Folder current = getReadableFolder(folderId, user);
        List<FolderDto> children = folderRepository.findByParentOrderByNameAsc(current)
                .stream()
                .filter(child -> accessService.canRead(child, user))
                .map(folderMapper::toDto)
                .toList();

        FolderDto parent = current.getParent() == null ? null : folderMapper.toDto(current.getParent());
        var files = storedFileRepository.findByFolderOrderByDisplayNameAsc(current).stream()
                .filter(file -> accessService.canRead(file, user))
                .map(storedFileMapper::toDto)
                .toList();
        boolean canEdit = accessService.canEdit(current, user);
        boolean canDelete = accessService.isOwner(current, user) && !current.isRoot();
        return new FolderPageDto(folderMapper.toDto(current), parent, breadcrumbs(current), children, files, canEdit, canDelete);
    }

    @Transactional
    public FolderDto createFolder(Long parentId, String name, User user) {
        Folder parent = getEditableFolder(parentId, user);
        String normalizedName = normalizeName(name);
        ensureUniqueName(parent, normalizedName, null);

        Folder folder = new Folder();
        folder.setName(normalizedName);
        folder.setOwner(parent.getOwner());
        folder.setParent(parent);
        folder.setRoot(false);
        Folder saved = folderRepository.save(folder);
        if (!accessService.isOwner(parent, user)) {
            accessService.grantFolder(saved, user, AccessLevel.EDIT, parent.getOwner());
        }
        return folderMapper.toDto(saved);
    }

    @Transactional
    public FolderDto renameFolder(Long folderId, String name, User user) {
        Folder folder = getEditableFolder(folderId, user);
        if (folder.isRoot()) {
            throw new AppException("Корневую папку нельзя переименовать");
        }

        String normalizedName = normalizeName(name);
        ensureUniqueName(folder.getParent(), normalizedName, folder.getId());
        folder.setName(normalizedName);
        return folderMapper.toDto(folder);
    }

    @Transactional
    public Long deleteFolder(Long folderId, User user) {
        Folder folder = getReadableFolder(folderId, user);
        if (!accessService.isOwner(folder, user)) {
            throw new AppException("Удалять папку может только владелец");
        }
        if (folder.isRoot()) {
            throw new AppException("Корневую папку нельзя удалить");
        }

        Long parentId = folder.getParent().getId();
        deleteSubtree(folder, user);
        return parentId;
    }

    @Transactional
    public void shareFolder(Long folderId, ShareForm form, User owner) {
        Folder folder = getReadableFolder(folderId, owner);
        User target = userService.findByEmail(form.email());
        AccessLevel level = form.accessLevel() == null ? AccessLevel.READ : form.accessLevel();
        accessService.grantFolder(folder, target, level, owner);
    }

    private void deleteSubtree(Folder folder, User user) {
        List<Folder> children = folderRepository.findByParentOrderByNameAsc(folder);
        for (Folder child : children) {
            deleteSubtree(child, user);
        }
        var files = storedFileRepository.findByFolderOrderByDisplayNameAsc(folder);
        files.forEach(file -> storageService.delete(file.getOwner().getId(), file.getStorageKey()));
        storedFileRepository.deleteAll(files);
        folderRepository.delete(folder);
    }

    @Transactional(readOnly = true)
    public Folder getReadableFolder(Long folderId, User user) {
        Folder folder = findFolder(folderId);
        accessService.requireFolderRead(folder, user);
        return folder;
    }

    @Transactional(readOnly = true)
    public Folder getEditableFolder(Long folderId, User user) {
        Folder folder = findFolder(folderId);
        accessService.requireFolderEdit(folder, user);
        return folder;
    }

    private Folder findFolder(Long folderId) {
        return folderRepository.findById(folderId)
                .orElseThrow(() -> new AppException("Папка не найдена"));
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
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw new AppException("Название папки не может быть пустым");
        }
        return normalized;
    }

    private void ensureUniqueName(Folder parent, String name, Long currentFolderId) {
        boolean duplicate = currentFolderId == null
                ? folderRepository.existsByParentAndNameIgnoreCase(parent, name)
                : folderRepository.existsByParentAndNameIgnoreCaseAndIdNot(parent, name, currentFolderId);
        if (duplicate) {
            throw new AppException("Папка с таким названием уже есть");
        }
    }
}
