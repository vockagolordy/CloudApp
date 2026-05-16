package org.example.cloudapp.controller;

import org.example.cloudapp.service.FolderService;
import org.example.cloudapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppController {
    private final UserService userService;
    private final FolderService folderService;

    public AppController(UserService userService, FolderService folderService) {
        this.userService = userService;
        this.folderService = folderService;
    }

    @GetMapping("/app")
    public String app(Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        var root = folderService.getOrCreateRoot(user);
        return "redirect:/folders/" + root.getId();
    }
}
