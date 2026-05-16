package org.example.cloudapp.controller;

import jakarta.validation.Valid;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.form.FolderForm;
import org.example.cloudapp.form.ShareForm;
import org.example.cloudapp.service.FolderService;
import org.example.cloudapp.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FolderController {
    private final UserService userService;
    private final FolderService folderService;

    public FolderController(UserService userService, FolderService folderService) {
        this.userService = userService;
        this.folderService = folderService;
    }

    @GetMapping("/folders/{id}")
    public String show(@PathVariable Long id, Authentication authentication, Model model) {
        var user = userService.findByEmail(authentication.getName());
        model.addAttribute("email", authentication.getName());
        model.addAttribute("page", folderService.getFolderPage(id, user));
        if (!model.containsAttribute("folderForm")) {
            model.addAttribute("folderForm", new FolderForm(""));
        }
        if (!model.containsAttribute("shareForm")) {
            model.addAttribute("shareForm", new ShareForm("", null));
        }
        return "app/folder";
    }

    @PostMapping("/folders")
    public String create(@RequestParam Long parentId,
                         @Valid @ModelAttribute("folderForm") FolderForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Введите корректное название папки");
            return "redirect:/folders/" + parentId;
        }

        var user = userService.findByEmail(authentication.getName());
        try {
            folderService.createFolder(parentId, form.name(), user);
            redirectAttributes.addFlashAttribute("message", "Папка создана");
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/folders/" + parentId;
    }

    @PostMapping("/folders/{id}/rename")
    public String rename(@PathVariable Long id,
                         @RequestParam Long currentId,
                         @Valid @ModelAttribute("folderForm") FolderForm form,
                         BindingResult bindingResult,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Введите корректное название папки");
            return "redirect:/folders/" + currentId;
        }

        var user = userService.findByEmail(authentication.getName());
        try {
            folderService.renameFolder(id, form.name(), user);
            redirectAttributes.addFlashAttribute("message", "Папка переименована");
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/folders/" + currentId;
    }

    @PostMapping("/folders/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) Long currentId,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        var user = userService.findByEmail(authentication.getName());
        try {
            Long parentId = folderService.deleteFolder(id, user);
            redirectAttributes.addFlashAttribute("message", "Папка удалена");
            return "redirect:/folders/" + parentId;
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return currentId == null ? "redirect:/app" : "redirect:/folders/" + currentId;
        }
    }

    @PostMapping("/folders/{id}/share")
    public String share(@PathVariable Long id,
                        @RequestParam Long currentId,
                        @Valid @ModelAttribute("shareForm") ShareForm form,
                        BindingResult bindingResult,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Введите email пользователя для доступа");
            return "redirect:/folders/" + currentId;
        }

        var user = userService.findByEmail(authentication.getName());
        try {
            folderService.shareFolder(id, form, user);
            redirectAttributes.addFlashAttribute("message", "Доступ к папке выдан");
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/folders/" + currentId;
    }
}
