package com.blokus.blokus.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "game_users")
public class GameUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;
    
    @Enumerated(EnumType.STRING)
    private PlayerColor color;
    
    private int score;
    
    private boolean isBot;
    
    @ElementCollection(fetch = FetchType.EAGER) // Eager fetch might be needed depending on usage
    @CollectionTable(name = "game_user_available_pieces", joinColumns = @JoinColumn(name = "game_user_id"))
    @Column(name = "piece_id")
    private Set<String> availablePieceIds = new HashSet<>();
    
    // Enum for player colors
    public enum PlayerColor {
        BLUE, RED, GREEN, YELLOW
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public PlayerColor getColor() {
        return color;
    }

    public void setColor(PlayerColor color) {
        this.color = color;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean bot) {
        isBot = bot;
    }

    public Set<String> getAvailablePieceIds() {
        return availablePieceIds;
    }

    public void setAvailablePieceIds(Set<String> availablePieceIds) {
        this.availablePieceIds = availablePieceIds;
    }
} 