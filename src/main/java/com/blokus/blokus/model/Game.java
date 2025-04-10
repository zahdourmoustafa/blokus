package com.blokus.blokus.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private GameStatus status;
    
    @Enumerated(EnumType.STRING)
    private GameMode mode;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @Column(name = "expected_players")
    private int expectedPlayers;
    
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameUser> players = new ArrayList<>();
    
    // Reference to the current player (whose turn it is)
    // Note: This is not mapped as ManyToOne to avoid circular references
    // since GameUser already has a ManyToOne to Game
    @Column(name = "current_player_index")
    private Integer currentPlayerIndex;
    
    // Enum pour le statut de la partie
    public enum GameStatus {
        WAITING, PLAYING, FINISHED
    }
    
    // Enum pour le mode de jeu
    public enum GameMode {
        CLASSIC, TIMED
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public GameMode getMode() {
        return mode;
    }

    public void setMode(GameMode mode) {
        this.mode = mode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public int getExpectedPlayers() {
        return expectedPlayers;
    }

    public void setExpectedPlayers(int expectedPlayers) {
        this.expectedPlayers = expectedPlayers;
    }

    public List<GameUser> getPlayers() {
        return players;
    }

    public void setPlayers(List<GameUser> players) {
        this.players = players;
    }
    
    // Méthodes utilitaires
    public void addPlayer(GameUser player) {
        players.add(player);
        player.setGame(this);
    }
    
    public void removePlayer(GameUser player) {
        players.remove(player);
        player.setGame(null);
    }
    
    /**
     * Gets the current player whose turn it is
     * @return Current player or null if game is not in progress
     */
    public GameUser getCurrentPlayer() {
        // Only return a current player if the game is in playing status
        if (status != GameStatus.PLAYING || players.isEmpty()) {
            return null;
        }
        
        // If currentPlayerIndex is not set, default to the first player
        if (currentPlayerIndex == null) {
            return players.isEmpty() ? null : players.get(0);
        }
        
        // Make sure the index is valid
        if (currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            return players.get(currentPlayerIndex);
        }
        
        // Fallback to first player if index is invalid
        return players.isEmpty() ? null : players.get(0);
    }
    
    /**
     * Sets the current player for the game
     * @param player The player to set as current
     */
    public void setCurrentPlayer(GameUser player) {
        if (player == null || !players.contains(player)) {
            // If player is null or not in the game, set to first player or null
            currentPlayerIndex = players.isEmpty() ? null : 0;
        } else {
            // Set to the index of the player in the list
            currentPlayerIndex = players.indexOf(player);
        }
    }
    
    // Initialisation
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        status = GameStatus.WAITING;
        currentPlayerIndex = 0; // Start with the first player
    }
} 