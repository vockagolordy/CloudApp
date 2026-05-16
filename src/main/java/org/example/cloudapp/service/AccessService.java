package org.example.cloudapp.service;

import org.example.cloudapp.entity.AccessLevel;
import org.example.cloudapp.entity.FileAccess;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.FolderAccess;
import org.example.cloudapp.entity.StoredFile;
import org.example.cloudapp.entity.User;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.repository.FileAccessRepository;
import org.example.cloudapp.repository.FolderAccessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessService {
    private final FileAccessRepository fileAccessRepository;
    private final FolderAccessRepository folderAccessRepository;

    public AccessService(FileAccessRepository fileAccessRepository, FolderAccessRepository folderAccessRepository) {
        this.fileAccessRepository = fileAccessRepository;
        this.folderAccessRepository = folderAccessRepository;
    }

    @Transactional(readOnly = true)
    public boolean canRead(Folder folder, User user) {
        if (isOwner(folder, user) || folderAccessRepository.findByFolderAndUser(folder, user).isPresent()) {
            return true;
        }
        return folder.getParent() != null && canRead(folder.getParent(), user);
    }

    @Transactional(readOnly = true)
    public boolean canEdit(Folder folder, User user) {
        if (isOwner(folder, user)) {
            return true;
        }
        boolean directEdit = folderAccessRepository.findByFolderAndUser(folder, user)
                .map(access -> access.getAccessLevel() == AccessLevel.EDIT)
                .orElse(false);
        return directEdit || (folder.getParent() != null && canEdit(folder.getParent(), user));
    }

    @Transactional(readOnly = true)
    public boolean canRead(StoredFile file, User user) {
        return isOwner(file, user)
                || fileAccessRepository.findByFileAndUser(file, user).isPresent()
                || canRead(file.getFolder(), user);
    }

    @Transactional(readOnly = true)
    public boolean canEdit(StoredFile file, User user) {
        if (isOwner(file, user)) {
            return true;
        }
        return fileAccessRepository.findByFileAndUser(file, user)
                .map(access -> access.getAccessLevel() == AccessLevel.EDIT)
                .orElse(false)
                || canEdit(file.getFolder(), user);
    }

    @Transactional(readOnly = true)
    public void requireFolderRead(Folder folder, User user) {
        if (!canRead(folder, user)) {
            throw new AppException("Нет доступа к этой папке");
        }
    }

    @Transactional(readOnly = true)
    public void requireFolderEdit(Folder folder, User user) {
        if (!canEdit(folder, user)) {
            throw new AppException("Нет прав на изменение этой папки");
        }
    }

    @Transactional(readOnly = true)
    public void requireFileRead(StoredFile file, User user) {
        if (!canRead(file, user)) {
            throw new AppException("Нет доступа к этому файлу");
        }
    }

    @Transactional(readOnly = true)
    public void requireFileEdit(StoredFile file, User user) {
        if (!canEdit(file, user)) {
            throw new AppException("Нет прав на изменение этого файла");
        }
    }

    @Transactional
    public void grantFolder(Folder folder, User target, AccessLevel level, User grantedBy) {
        if (!isOwner(folder, grantedBy)) {
            throw new AppException("Делиться папкой может только владелец");
        }
        if (isOwner(folder, target)) {
            throw new AppException("Владелец уже имеет полный доступ");
        }
        FolderAccess access = folderAccessRepository.findByFolderAndUser(folder, target).orElseGet(FolderAccess::new);
        access.setFolder(folder);
        access.setUser(target);
        access.setAccessLevel(level == null ? AccessLevel.READ : level);
        access.setGrantedBy(grantedBy);
        folderAccessRepository.save(access);
    }

    @Transactional
    public void grantFile(StoredFile file, User target, AccessLevel level, User grantedBy) {
        if (!isOwner(file, grantedBy)) {
            throw new AppException("Делиться файлом может только владелец");
        }
        if (isOwner(file, target)) {
            throw new AppException("Владелец уже имеет полный доступ");
        }
        FileAccess access = fileAccessRepository.findByFileAndUser(file, target).orElseGet(FileAccess::new);
        access.setFile(file);
        access.setUser(target);
        access.setAccessLevel(level == null ? AccessLevel.READ : level);
        access.setGrantedBy(grantedBy);
        fileAccessRepository.save(access);
    }

    public boolean isOwner(Folder folder, User user) {
        return folder.getOwner().getId().equals(user.getId());
    }

    public boolean isOwner(StoredFile file, User user) {
        return file.getOwner().getId().equals(user.getId());
    }
}
