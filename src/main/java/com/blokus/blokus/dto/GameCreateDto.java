package com.blokus.blokus.dto;

import com.blokus.blokus.model.Game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GameCreateDto {
    
    @NotBlank(message = "Game name is required")
    @Size(min = 3, max = 50, message = "Game name must be between 3 and 50 characters")
    private String name;
    
    @Min(value = 2, message = "At least 2 players are required")
    @Max(value = 4, message = "Maximum 4 players allowed")
    private int expectedPlayers;
    
    private Game.GameMode mode = Game.GameMode.CLASSIC;
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getExpectedPlayers() {
        return expectedPlayers;
    }
    
    public void setExpectedPlayers(int expectedPlayers) {
        this.expectedPlayers = expectedPlayers;
    }
    
    public Game.GameMode getMode() {
        return mode;
    }
    
    public void setMode(Game.GameMode mode) {
        this.mode = mode;
    }
} 