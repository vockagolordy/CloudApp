package org.example.cloudapp.config;

import org.example.cloudapp.entity.Role;
import org.example.cloudapp.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean
    public CommandLineRunner seedRoles(RoleRepository roleRepository) {
        return args -> roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
    }
}
