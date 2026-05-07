package org.example.cloudapp.controller;

import jakarta.validation.Valid;
import org.example.cloudapp.exception.AppException;
import org.example.cloudapp.form.RegisterForm;
import org.example.cloudapp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/app";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm("", "", ""));
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(form);
        } catch (AppException ex) {
            bindingResult.rejectValue("email", "email.exists", ex.getMessage());
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute("message", "Аккаунт создан. Теперь можно войти");
        return "redirect:/login";
    }
}
