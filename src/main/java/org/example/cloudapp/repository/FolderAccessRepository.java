package org.example.cloudapp.repository;

import java.util.List;
import java.util.Optional;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.FolderAccess;
import org.example.cloudapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderAccessRepository extends JpaRepository<FolderAccess, Long> {
    Optional<FolderAccess> findByFolderAndUser(Folder folder, User user);

    List<FolderAccess> findByUserOrderByCreatedAtDesc(User user);

    void deleteByFolder(Folder folder);
}
