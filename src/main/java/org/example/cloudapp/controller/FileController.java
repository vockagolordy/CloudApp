package org.example.cloudapp.controller;

import jakarta.validation.Valid;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.form.ShareForm;
import org.example.cloudapp.service.StoredFileService;
import org.example.cloudapp.service.UserService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileController {
    private final UserService userService;
    private final StoredFileService storedFileService;

    public FileController(UserService userService, StoredFileService storedFileService) {
        this.userService = userService;
        this.storedFileService = storedFileService;
    }

    @PostMapping("/files")
    public String upload(@RequestParam Long folderId,
                         @RequestParam("file") MultipartFile file,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        var user = userService.findByEmail(authentication.getName());
        try {
            storedFileService.upload(folderId, file, user);
            redirectAttributes.addFlashAttribute("message", "Файл загружен");
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/folders/" + folderId;
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id, Authentication authentication) {
        var user = userService.findByEmail(authentication.getName());
        var file = storedFileService.download(id, user);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }

    @PostMapping("/files/{id}/share")
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
            storedFileService.shareFile(id, form, user);
            redirectAttributes.addFlashAttribute("message", "Доступ к файлу выдан");
        } catch (AppException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/folders/" + currentId;
    }
}
