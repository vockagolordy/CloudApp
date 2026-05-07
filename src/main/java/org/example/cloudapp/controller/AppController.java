package org.example.cloudapp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppController {
    @GetMapping("/app")
    public String app(Authentication authentication, Model model) {
        model.addAttribute("email", authentication.getName());
        return "app/dashboard";
    }
}
