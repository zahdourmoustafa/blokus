package com.blokus.blokus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class UserRegistrationDto {
    
    @NotEmpty(message = "Le nom d'utilisateur ne peut pas être vide")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit avoir entre 3 et 50 caractères")
    private String username;
    
    @NotEmpty(message = "L'email ne peut pas être vide")
    @Email(message = "Veuillez fournir une adresse email valide")
    private String email;
    
    @NotEmpty(message = "Le mot de passe ne peut pas être vide")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;
    
    @NotEmpty(message = "La confirmation du mot de passe ne peut pas être vide")
    private String confirmPassword;
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getConfirmPassword() {
        return confirmPassword;
    }
    
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
} 