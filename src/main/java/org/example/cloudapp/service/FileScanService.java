package org.example.cloudapp.service;

import java.time.Instant;
import java.util.List;
import org.example.cloudapp.entity.FileScanResult;
import org.example.cloudapp.entity.FileScanStatus;
import org.example.cloudapp.entity.StoredFile;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.repository.FileScanResultRepository;
import org.example.cloudapp.service.integration.BouncerVirusScanClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
        result.setExternalScanId(response.scanId());
        result.setMessage(response.message());
        result.setThreats(response.threats());
        result.setScannedAt(Instant.now());
        FileScanResult saved = fileScanResultRepository.save(result);
        file.setScanResult(saved);
        return saved;
    }

    @Scheduled(fixedDelayString = "${cloudapp.virus-scan.refresh-delay:10000}")
    @Transactional
    @CacheEvict(value = "folderPage", allEntries = true)
    public void refreshPendingScans() {
        List<FileScanResult> pendingResults = fileScanResultRepository
                .findTop20ByStatusOrderByCreatedAtAsc(FileScanStatus.PENDING);
        for (FileScanResult result : pendingResults) {
            if (!StringUtils.hasText(result.getExternalScanId())) {
                result.setStatus(FileScanStatus.FAILED);
                result.setMessage("Не удалось обновить проверку: внешний scan id не сохранен");
                result.setScannedAt(Instant.now());
                continue;
            }
            BouncerVirusScanClient.ScanResponse response = virusScanClient.getResult(result.getExternalScanId());
            result.setStatus(response.status());
            result.setProvider(response.provider());
            result.setExternalScanId(response.scanId() == null ? result.getExternalScanId() : response.scanId());
            result.setMessage(response.message());
            result.setThreats(response.threats());
            result.setScannedAt(Instant.now());
        }
    }

    @Transactional(readOnly = true)
    public void requireDownloadAllowed(StoredFile file) {
        FileScanResult result = fileScanResultRepository.findByFile(file).orElse(null);
        if (result != null && result.getStatus() == FileScanStatus.INFECTED) {
            throw new AppException("Скачивание заблокировано: файл не прошел антивирусную проверку");
        }
    }
}
