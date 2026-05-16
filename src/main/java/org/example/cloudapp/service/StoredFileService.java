package org.example.cloudapp.service;

import org.example.cloudapp.dto.StoredFileDto;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.StoredFile;
import org.example.cloudapp.entity.User;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.repository.StoredFileRepository;
import org.example.cloudapp.util.mapper.StoredFileMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoredFileService {
    private final StoredFileRepository storedFileRepository;
    private final StorageService storageService;
    private final FolderService folderService;
    private final StoredFileMapper storedFileMapper;

    public StoredFileService(StoredFileRepository storedFileRepository, StorageService storageService,
                             FolderService folderService, StoredFileMapper storedFileMapper) {
        this.storedFileRepository = storedFileRepository;
        this.storageService = storageService;
        this.folderService = folderService;
        this.storedFileMapper = storedFileMapper;
    }

    @Transactional
    public StoredFileDto upload(Long folderId, MultipartFile upload, User user) {
        if (upload.isEmpty()) {
            throw new AppException("Выберите файл для загрузки");
        }

        Folder folder = folderService.getOwnedFolder(folderId, user);
        StorageService.StoredBinary binary = storageService.save(user.getId(), upload);
        String originalName = originalName(upload);

        StoredFile file = new StoredFile();
        file.setDisplayName(originalName);
        file.setOriginalName(originalName);
        file.setStorageKey(binary.storageKey());
        file.setChecksum(binary.checksum());
        file.setContentType(upload.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : upload.getContentType());
        file.setExtension(extension(originalName));
        file.setSize(upload.getSize());
        file.setOwner(user);
        file.setFolder(folder);
        return storedFileMapper.toDto(storedFileRepository.save(file));
    }

    @Transactional(readOnly = true)
    public DownloadedFile download(Long fileId, User user) {
        StoredFile file = getOwnedFile(fileId, user);
        Resource resource = storageService.load(file.getOwner().getId(), file.getStorageKey());
        return new DownloadedFile(file.getDisplayName(), file.getContentType(), resource);
    }

    private StoredFile getOwnedFile(Long fileId, User user) {
        StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new AppException("Файл не найден"));
        if (!file.getOwner().getId().equals(user.getId())) {
            throw new AppException("Нет доступа к этому файлу");
        }
        return file;
    }

    private String originalName(MultipartFile upload) {
        String filename = upload.getOriginalFilename() == null ? "file" : upload.getOriginalFilename();
        return StringUtils.cleanPath(filename);
    }

    private String extension(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(index + 1).toLowerCase();
    }

    public record DownloadedFile(String filename, String contentType, Resource resource) {
    }
}
