package org.example.cloudapp.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.example.cloudapp.entity.AccessLevel;

public record ShareForm(
        @NotBlank(message = "Email обязателен")
        @Email(message = "Введите корректный email")
        String email,
        AccessLevel accessLevel
) {
}
