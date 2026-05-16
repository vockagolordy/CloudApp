package org.example.cloudapp.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderForm(
        @NotBlank(message = "Название папки обязательно")
        @Size(min = 1, max = 120, message = "Название должно быть от 1 до 120 символов")
        String name
) {
}
