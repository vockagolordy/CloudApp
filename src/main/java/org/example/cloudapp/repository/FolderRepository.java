package org.example.cloudapp.repository;

import java.util.List;
import java.util.Optional;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.User;
import org.example.cloudapp.repository.custom.FolderRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, Long>, FolderRepositoryCustom {
    Optional<Folder> findByOwnerAndRootTrue(User owner);

    List<Folder> findByOwnerAndParentOrderByNameAsc(User owner, Folder parent);

    List<Folder> findByParentOrderByNameAsc(Folder parent);

    long countByParent(Folder parent);

    boolean existsByOwnerAndParentAndNameIgnoreCase(User owner, Folder parent, String name);

    boolean existsByOwnerAndParentAndNameIgnoreCaseAndIdNot(User owner, Folder parent, String name, Long id);

    boolean existsByParentAndNameIgnoreCase(Folder parent, String name);

    boolean existsByParentAndNameIgnoreCaseAndIdNot(Folder parent, String name, Long id);
}
