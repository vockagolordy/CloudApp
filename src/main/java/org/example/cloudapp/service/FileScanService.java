package org.example.cloudapp.service;

import java.time.Instant;
import org.example.cloudapp.entity.FileScanResult;
import org.example.cloudapp.entity.FileScanStatus;
import org.example.cloudapp.entity.StoredFile;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.repository.FileScanResultRepository;
import org.example.cloudapp.service.integration.BouncerVirusScanClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileScanService {
    private final FileScanResultRepository fileScanResultRepository;
    private final BouncerVirusScanClient virusScanClient;

    public FileScanService(FileScanResultRepository fileScanResultRepository,
                           BouncerVirusScanClient virusScanClient) {
        this.fileScanResultRepository = fileScanResultRepository;
        this.virusScanClient = virusScanClient;
    }

    @Transactional
    public FileScanResult scanAndSave(StoredFile file, MultipartFile upload) {
        BouncerVirusScanClient.ScanResponse response = virusScanClient.scan(upload);
        FileScanResult result = new FileScanResult();
        result.setFile(file);
        result.setStatus(response.status());
        result.setProvider(response.provider());
        result.setMessage(response.message());
        result.setThreats(response.threats());
        result.setScannedAt(Instant.now());
        FileScanResult saved = fileScanResultRepository.save(result);
        file.setScanResult(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public void requireDownloadAllowed(StoredFile file) {
        FileScanResult result = fileScanResultRepository.findByFile(file).orElse(null);
        if (result != null && result.getStatus() == FileScanStatus.INFECTED) {
            throw new AppException("Скачивание заблокировано: файл не прошел антивирусную проверку");
        }
    }
}
