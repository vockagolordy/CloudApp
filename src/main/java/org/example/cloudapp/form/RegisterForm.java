package org.example.cloudapp.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterForm(
        @NotBlank(message = "Email обязателен")
        @Email(message = "Введите корректный email")
        String email,

        @NotBlank(message = "Имя обязательно")
        @Size(min = 2, max = 120, message = "Имя должно быть от 2 до 120 символов")
        String displayName,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 120, message = "Пароль должен быть от 8 до 120 символов")
        String password
) {
}
