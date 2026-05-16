package org.example.cloudapp.repository;

import java.util.List;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findByFolderOrderByDisplayNameAsc(Folder folder);
}
