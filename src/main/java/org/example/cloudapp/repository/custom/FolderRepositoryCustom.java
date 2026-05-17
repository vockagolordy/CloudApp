package org.example.cloudapp.repository.custom;

import java.util.List;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.User;

public interface FolderRepositoryCustom {
    List<Folder> searchReadableByName(User user, String query, int limit);
}
