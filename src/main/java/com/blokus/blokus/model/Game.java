package com.blokus.blokus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    
    @OneToOne(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private Board board;
    
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

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
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
    
    // Initialisation
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        status = GameStatus.WAITING;
    }
} 