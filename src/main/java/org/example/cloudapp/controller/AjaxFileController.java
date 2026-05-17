package org.example.cloudapp.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.example.cloudapp.form.ShareForm;
import org.example.cloudapp.service.StoredFileService;
import org.example.cloudapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ajax/files")
public class AjaxFileController {
    private final UserService userService;
    private final StoredFileService storedFileService;

    public AjaxFileController(UserService userService, StoredFileService storedFileService) {
        this.userService = userService;
        this.storedFileService = storedFileService;
    }

    @PostMapping("/{id}/share")
    public Map<String, String> share(@PathVariable Long id, @Valid @RequestBody ShareForm form, Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        storedFileService.shareFile(id, form, user);
        return Map.of("status", "shared");
    }
}
