package com.blokus.blokus.dto;

import java.time.LocalDateTime;

import com.blokus.blokus.model.Game.GameMode;
import com.blokus.blokus.model.Game.GameStatus;

public class GameListDto {

    private Long id;
    private String nom;
    private GameStatus statut;
    private GameMode mode;
    private LocalDateTime dateCreation;
    private int nombreJoueursHumains;
    private int nombreJoueursActuels;
    private String createur;

    // Constructeurs
    public GameListDto() {
    }

    public GameListDto(Long id, String nom, GameStatus statut, GameMode mode, 
                       LocalDateTime dateCreation, int nombreJoueursHumains, 
                       int nombreJoueursActuels, String createur) {
        this.id = id;
        this.nom = nom;
        this.statut = statut;
        this.mode = mode;
        this.dateCreation = dateCreation;
        this.nombreJoueursHumains = nombreJoueursHumains;
        this.nombreJoueursActuels = nombreJoueursActuels;
        this.createur = createur;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public GameStatus getStatut() {
        return statut;
    }

    public void setStatut(GameStatus statut) {
        this.statut = statut;
    }

    public GameMode getMode() {
        return mode;
    }

    public void setMode(GameMode mode) {
        this.mode = mode;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public int getNombreJoueursHumains() {
        return nombreJoueursHumains;
    }

    public void setNombreJoueursHumains(int nombreJoueursHumains) {
        this.nombreJoueursHumains = nombreJoueursHumains;
    }

    public int getNombreJoueursActuels() {
        return nombreJoueursActuels;
    }

    public void setNombreJoueursActuels(int nombreJoueursActuels) {
        this.nombreJoueursActuels = nombreJoueursActuels;
    }

    public String getCreateur() {
        return createur;
    }

    public void setCreateur(String createur) {
        this.createur = createur;
    }
} 