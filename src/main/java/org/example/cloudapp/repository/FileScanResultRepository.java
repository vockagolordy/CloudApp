package org.example.cloudapp.repository;

import java.util.List;
import java.util.Optional;
import org.example.cloudapp.entity.FileScanResult;
import org.example.cloudapp.entity.FileScanStatus;
import org.example.cloudapp.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileScanResultRepository extends JpaRepository<FileScanResult, Long> {
    Optional<FileScanResult> findByFile(StoredFile file);

    List<FileScanResult> findTop20ByStatusOrderByCreatedAtAsc(FileScanStatus status);
}
