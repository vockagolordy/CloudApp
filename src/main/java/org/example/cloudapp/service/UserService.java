package org.example.cloudapp.service;

import java.util.Set;
import org.example.cloudapp.entity.Role;
import org.example.cloudapp.entity.User;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.form.RegisterForm;
import org.example.cloudapp.repository.RoleRepository;
import org.example.cloudapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterForm form) {
        String normalizedEmail = form.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AppException("Пользователь с таким email уже существует");
        }

        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleRepository.save(new Role(DEFAULT_ROLE)));

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setDisplayName(form.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(form.password()));
        user.setEnabled(true);
        user.setRoles(Set.of(role));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AppException("Пользователь не найден"));
    }
}
