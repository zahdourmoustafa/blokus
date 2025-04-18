package com.blokus.blokus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * DTO for game creation
 */
public class GameCreateDto {

    @NotEmpty(message = "Le nom de la partie est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom doit contenir entre 3 et 50 caractères")
    private String name;

    @Min(value = 2, message = "Le nombre minimum de joueurs est 2")
    @Max(value = 4, message = "Le nombre maximum de joueurs est 4")
    private int maxPlayers;

    private boolean timedMode;

    public GameCreateDto() {
    }

    public GameCreateDto(String name, int maxPlayers, boolean timedMode) {
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.timedMode = timedMode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public boolean isTimedMode() {
        return timedMode;
    }

    public void setTimedMode(boolean timedMode) {
        this.timedMode = timedMode;
    }
} 