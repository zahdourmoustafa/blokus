package com.blokus.blokus.controller;

import com.blokus.blokus.dto.UserRegistrationDto;
import com.blokus.blokus.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                              BindingResult result, Model model) {
        
        // Vérifier si le nom d'utilisateur existe déjà
        if (userService.existsByUsername(userDto.getUsername())) {
            result.rejectValue("username", "error.username", "Ce nom d'utilisateur est déjà utilisé");
        }
        
        // Vérifier si l'email existe déjà
        if (userService.existsByEmail(userDto.getEmail())) {
            result.rejectValue("email", "error.email", "Cette adresse email est déjà utilisée");
        }
        
        // Vérifier si les mots de passe correspondent
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.password", "Les mots de passe ne correspondent pas");
        }
        
        if (result.hasErrors()) {
            return "auth/register";
        }
        
        userService.register(userDto);
        return "redirect:/login?success";
    }
    
    @GetMapping("/")
    public String home() {
        return "redirect:/home";
    }
    
    @GetMapping("/home")
    public String homePage() {
        return "home";
    }
} 