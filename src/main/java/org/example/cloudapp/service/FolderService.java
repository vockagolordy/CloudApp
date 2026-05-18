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
import org.example.cloudapp.repository.FolderAccessRepository;
import org.example.cloudapp.repository.FolderRepository;
import org.example.cloudapp.repository.StoredFileRepository;
import org.example.cloudapp.util.mapper.FolderMapper;
import org.example.cloudapp.util.mapper.StoredFileMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FolderService {
    private final FolderRepository folderRepository;
    private final FolderAccessRepository folderAccessRepository;
    private final FolderMapper folderMapper;
    private final StoredFileRepository storedFileRepository;
    private final StoredFileMapper storedFileMapper;
    private final StorageService storageService;
    private final AccessService accessService;
    private final UserService userService;

    public FolderService(FolderRepository folderRepository, FolderAccessRepository folderAccessRepository, FolderMapper folderMapper,
                         StoredFileRepository storedFileRepository, StoredFileMapper storedFileMapper,
                         StorageService storageService, AccessService accessService, UserService userService) {
        this.folderRepository = folderRepository;
        this.folderAccessRepository = folderAccessRepository;
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
    @Cacheable(value = "folderPage", key = "#p1.id + ':' + #p0", unless = "#result.current.root")
    public FolderPageDto getFolderPage(Long folderId, User user) {
        Folder current = getReadableFolder(folderId, user);
        List<FolderDto> children = folderRepository.findByParentOrderByNameAsc(current)
                .stream()
                .filter(child -> accessService.canRead(child, user))
                .map(folderMapper::toDto)
                .toList();

        FolderDto parent = readableParent(current, user);
        var files = storedFileRepository.findByFolderOrderByDisplayNameAsc(current).stream()
                .filter(file -> accessService.canRead(file, user))
                .map(storedFileMapper::toDto)
                .toList();
        boolean canEdit = accessService.canEdit(current, user);
        boolean canDelete = accessService.isOwner(current, user) && !current.isRoot();
        boolean canShare = accessService.isOwner(current, user);
        long storageUsedBytes = storedFileRepository.calculateStorageUsedByOwner(current.getOwner());
        long largerThanAverageFileCount = storedFileRepository.countOwnedFilesLargerThanAverage(current.getOwner());
        return new FolderPageDto(
                folderMapper.toDto(current),
                parent,
                breadcrumbs(current, user),
                children,
                sharedFolders(current, user),
                files,
                canEdit,
                canDelete,
                canShare,
                storageUsedBytes,
                largerThanAverageFileCount
        );
    }

    @Transactional
    @CacheEvict(value = "folderPage", allEntries = true)
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
    @CacheEvict(value = "folderPage", allEntries = true)
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
    @CacheEvict(value = "folderPage", allEntries = true)
    public Long deleteFolder(Long folderId, User user) {
        Folder folder = getReadableFolder(folderId, user);
        if (!accessService.isOwner(folder, user)) {
            throw new AppException("Удалять папку может только владелец");
        }
        if (folder.isRoot()) {
            throw new AppException("Корневую папку нельзя удалить");
        }

        Long parentId = folder.getParent().getId();
        deleteSubtree(folder);
        return parentId;
    }

    @Transactional
    @CacheEvict(value = "folderPage", allEntries = true)
    public void shareFolder(Long folderId, ShareForm form, User owner) {
        Folder folder = getReadableFolder(folderId, owner);
        if (folder.isRoot()) {
            throw new AppException("Корневую папку нельзя открыть для общего доступа");
        }
        User target = userService.findByEmail(form.email());
        AccessLevel level = form.accessLevel() == null ? AccessLevel.READ : form.accessLevel();
        accessService.grantFolder(folder, target, level, owner);
    }

    @Transactional(readOnly = true)
    public List<FolderDto> searchReadableFolders(String query, User user) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            return List.of();
        }
        return folderRepository.searchReadableByName(user, normalized, 20)
                .stream()
                .filter(folder -> accessService.canRead(folder, user))
                .map(folderMapper::toDto)
                .toList();
    }

    private void deleteSubtree(Folder folder) {
        List<Folder> children = folderRepository.findByParentOrderByNameAsc(folder);
        for (Folder child : children) {
            deleteSubtree(child);
        }
        var files = storedFileRepository.findByFolderOrderByDisplayNameAsc(folder);
        files.forEach(accessService::deleteFileAccesses);
        files.forEach(file -> storageService.delete(file.getOwner().getId(), file.getStorageKey()));
        storedFileRepository.deleteAll(files);
        accessService.deleteFolderAccesses(folder);
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

    private FolderDto readableParent(Folder folder, User user) {
        Folder parent = folder.getParent();
        if (parent == null || !accessService.canRead(parent, user)) {
            return null;
        }
        return folderMapper.toDto(parent);
    }

    private List<FolderDto> breadcrumbs(Folder folder, User user) {
        List<FolderDto> result = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            if (accessService.canRead(current, user)) {
                result.add(folderMapper.toDto(current));
            }
            current = current.getParent();
        }
        Collections.reverse(result);
        return result;
    }

    private List<FolderDto> sharedFolders(Folder current, User user) {
        if (!current.isRoot() || !accessService.isOwner(current, user)) {
            return List.of();
        }
        return folderAccessRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(access -> access.getFolder())
                .filter(folder -> !accessService.isOwner(folder, user))
                .filter(folder -> !folder.isRoot())
                .filter(folder -> accessService.canRead(folder, user))
                .map(folderMapper::toDto)
                .toList();
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
