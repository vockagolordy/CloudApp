package org.example.cloudapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.example.cloudapp.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
    private final Path root;

    public StorageService(@Value("${cloudapp.storage-root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public StoredBinary save(Long userId, MultipartFile file) {
        String storageKey = UUID.randomUUID().toString();
        Path userDir = root.resolve(String.valueOf(userId)).normalize();
        Path target = userDir.resolve(storageKey).normalize();
        if (!target.startsWith(userDir)) {
            throw new AppException("Некорректный путь хранения файла");
        }

        try {
            Files.createDirectories(userDir);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = file.getInputStream();
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                Files.copy(digestInput, target);
            }
            return new StoredBinary(storageKey, HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new AppException("Не удалось сохранить файл");
        }
    }

    public Resource load(Long userId, String storageKey) {
        try {
            Path userDir = root.resolve(String.valueOf(userId)).normalize();
            Path target = userDir.resolve(storageKey).normalize();
            if (!target.startsWith(userDir)) {
                throw new AppException("Некорректный путь хранения файла");
            }
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new AppException("Файл на диске не найден");
            }
            return resource;
        } catch (IOException ex) {
            throw new AppException("Не удалось открыть файл");
        }
    }

    public void delete(Long userId, String storageKey) {
        try {
            Path userDir = root.resolve(String.valueOf(userId)).normalize();
            Path target = userDir.resolve(storageKey).normalize();
            if (target.startsWith(userDir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ex) {
            throw new AppException("Не удалось удалить файл с диска");
        }
    }

    public record StoredBinary(String storageKey, String checksum) {
    }
}
