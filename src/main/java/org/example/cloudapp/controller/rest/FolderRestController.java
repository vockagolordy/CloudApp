package org.example.cloudapp.controller.rest;

import jakarta.validation.Valid;
import java.util.List;
import org.example.cloudapp.dto.FolderDto;
import org.example.cloudapp.dto.FolderPageDto;
import org.example.cloudapp.form.FolderForm;
import org.example.cloudapp.service.FolderService;
import org.example.cloudapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/folders")
public class FolderRestController {
    private final UserService userService;
    private final FolderService folderService;

    public FolderRestController(UserService userService, FolderService folderService) {
        this.userService = userService;
        this.folderService = folderService;
    }

    @GetMapping("/{id}")
    public FolderPageDto read(@PathVariable Long id, Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        return folderService.getFolderPage(id, user);
    }

    @GetMapping("/search")
    public List<FolderDto> search(@RequestParam String q, Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        return folderService.searchReadableFolders(q, user);
    }

    @PostMapping
    public FolderDto create(@RequestParam Long parentId,
                            @Valid @RequestBody FolderForm form,
                            Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        return folderService.createFolder(parentId, form.name(), user);
    }

    @PatchMapping("/{id}")
    public FolderDto update(@PathVariable Long id,
                            @Valid @RequestBody FolderForm form,
                            Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        return folderService.renameFolder(id, form.name(), user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        folderService.deleteFolder(id, user);
    }
}
