package org.example.cloudapp.repository;

import java.util.List;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.StoredFile;
import org.example.cloudapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findByFolderOrderByDisplayNameAsc(Folder folder);

    @Query("""
            select coalesce(sum(file.size), 0)
            from StoredFile file
            where file.owner = :owner
            """)
    long calculateStorageUsedByOwner(@Param("owner") User owner);

    @Query("""
            select count(file)
            from StoredFile file
            where file.owner = :owner
              and file.size > (
                  select coalesce(avg(candidate.size), 0)
                  from StoredFile candidate
                  where candidate.owner = :owner
              )
            """)
    long countOwnedFilesLargerThanAverage(@Param("owner") User owner);
}
