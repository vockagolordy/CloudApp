package org.example.cloudapp.repository;

import java.util.Optional;
import org.example.cloudapp.entity.FileAccess;
import org.example.cloudapp.entity.StoredFile;
import org.example.cloudapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAccessRepository extends JpaRepository<FileAccess, Long> {
    Optional<FileAccess> findByFileAndUser(StoredFile file, User user);
}
