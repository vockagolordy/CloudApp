package org.example.cloudapp.repository;

import java.util.Optional;
import org.example.cloudapp.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
