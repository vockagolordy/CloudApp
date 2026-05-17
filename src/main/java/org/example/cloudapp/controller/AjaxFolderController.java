package org.example.cloudapp.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.example.cloudapp.form.FolderForm;
import org.example.cloudapp.form.ShareForm;
import org.example.cloudapp.service.FolderService;
import org.example.cloudapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ajax/folders")
public class AjaxFolderController {
    private final UserService userService;
    private final FolderService folderService;

    public AjaxFolderController(UserService userService, FolderService folderService) {
        this.userService = userService;
        this.folderService = folderService;
    }

    @PatchMapping("/{id}")
    public Object rename(@PathVariable Long id, @Valid @RequestBody FolderForm form, Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        return folderService.renameFolder(id, form.name(), user);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        folderService.deleteFolder(id, user);
        return Map.of("status", "deleted");
    }

    @PostMapping("/{id}/share")
    public Map<String, String> share(@PathVariable Long id, @Valid @RequestBody ShareForm form, Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        folderService.shareFolder(id, form, user);
        return Map.of("status", "shared");
    }
}
