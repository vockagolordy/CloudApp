package org.example.cloudapp.repository.custom.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import java.util.Locale;
import org.example.cloudapp.entity.Folder;
import org.example.cloudapp.entity.FolderAccess;
import org.example.cloudapp.entity.User;
import org.example.cloudapp.repository.custom.FolderRepositoryCustom;

public class FolderRepositoryCustomImpl implements FolderRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Folder> searchReadableByName(User user, String query, int limit) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Folder> criteria = builder.createQuery(Folder.class);
        Root<Folder> folder = criteria.from(Folder.class);

        Subquery<Long> sharedFolderIds = criteria.subquery(Long.class);
        Root<FolderAccess> access = sharedFolderIds.from(FolderAccess.class);
        sharedFolderIds.select(access.get("folder").get("id"))
                .where(builder.equal(access.get("user"), user));

        String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
        criteria.select(folder)
                .where(
                        builder.and(
                                builder.like(builder.lower(folder.get("name")), pattern),
                                builder.or(
                                        builder.equal(folder.get("owner"), user),
                                        folder.get("id").in(sharedFolderIds)
                                )
                        )
                )
                .orderBy(builder.asc(folder.get("name")), builder.asc(folder.get("id")));

        return entityManager.createQuery(criteria)
                .setMaxResults(limit)
                .getResultList();
    }
}
