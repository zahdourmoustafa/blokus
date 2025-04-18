package com.blokus.blokus.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.blokus.blokus.model.GameUser.PlayerColor;

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
            System.out.println("Debug getCurrentPlayer: No players or game not playing");
            return null;
        }
        
        // If no index set or invalid index, default to first player (BLUE)
        if (currentPlayerIndex == null || currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            System.out.println("Debug getCurrentPlayer: Invalid index, finding BLUE player");
            // Try to find BLUE player
            for (GameUser player : players) {
                if (player.getColor() == PlayerColor.BLUE) {
                    return player;
                }
            }
            // If no BLUE player, return first player
            System.out.println("Debug getCurrentPlayer: No BLUE player found, using first player");
            return players.get(0);
        }
        
        // Return player at the current index
        GameUser player = players.get(currentPlayerIndex);
        System.out.println("Debug getCurrentPlayer: Returning player at index " + currentPlayerIndex + 
                         " with color " + player.getColor());
        return player;
    }
    
    /**
     * Gets a sorted list of players in standard Blokus color order: BLUE, YELLOW, GREEN, RED
     * @return The sorted list of players
     */
    public List<GameUser> getSortedPlayers() {
        List<GameUser> sortedPlayers = new ArrayList<>(players);
        sortedPlayers.sort((p1, p2) -> {
            if (p1.getColor() == null || p2.getColor() == null) {
                return 0;
            }
            
            int p1Order = getColorOrder(p1.getColor());
            int p2Order = getColorOrder(p2.getColor());
            
            return Integer.compare(p1Order, p2Order);
        });
        
        // Debug the sorted player list
        System.out.println("DEBUG getSortedPlayers: Players after sorting:");
        for (int i = 0; i < sortedPlayers.size(); i++) {
            GameUser p = sortedPlayers.get(i);
            String pName = p.isBot() ? "Bot " + p.getColor() : 
                          (p.getUser() != null ? p.getUser().getUsername() : "Unknown");
            System.out.println("  Index " + i + ": " + pName + " (" + p.getColor() + ")");
        }
        
        return sortedPlayers;
    }
    
    /**
     * Get the order value for a color (BLUE=1, YELLOW=2, GREEN=3, RED=4)
     */
    private int getColorOrder(PlayerColor color) {
        return switch (color) {
            case BLUE -> 1;
            case YELLOW -> 2;
            case GREEN -> 3;
            case RED -> 4;
            default -> 5;
        };
    }
    
    /**
     * Gets a player by their color
     * @param color The player color to find
     * @return The player with that color, or null if not found
     */
    public GameUser getPlayerByColor(PlayerColor color) {
        return players.stream()
                .filter(player -> player.getColor() == color)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets the index of the current player
     * @return The current player index or null if not set
     */
    public Integer getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    /**
     * Sets the current player for the game
     * @param player The player to set as current
     */
    public void setCurrentPlayer(GameUser player) {
        // Simple direct assignment - don't use indices which can cause errors
        if (player == null) {
            this.currentPlayerIndex = null;
            System.out.println("DEBUG setCurrentPlayer: Setting currentPlayerIndex to null");
        } else {
            // Store the player's ID or another direct reference instead of computed index
            System.out.println("DEBUG setCurrentPlayer: Setting current player to " + 
                (player.isBot() ? "Bot " + player.getColor() : 
                (player.getUser() != null ? player.getUser().getUsername() : "Unknown")) + 
                " with color " + player.getColor());
            
            // Print all players for debugging
            System.out.println("DEBUG setCurrentPlayer: All players in game:");
            for (int i = 0; i < players.size(); i++) {
                GameUser p = players.get(i);
                System.out.println("  Player at index " + i + ": " + 
                    (p.isBot() ? "Bot " + p.getColor() : 
                    (p.getUser() != null ? p.getUser().getUsername() : "Unknown")) + 
                    " (color: " + p.getColor() + ", id: " + p.getId() + ")");
            }
            
            // First try finding by ID (most accurate)
            int directIndex = -1;
            if (player.getId() != null) {
                for (int i = 0; i < players.size(); i++) {
                    if (player.getId().equals(players.get(i).getId())) {
                        directIndex = i;
                        System.out.println("DEBUG setCurrentPlayer: Found player by ID at index: " + directIndex);
                        break;
                    }
                }
            }
            
            // If not found by ID, try finding by color
            if (directIndex == -1 && player.getColor() != null) {
                for (int i = 0; i < players.size(); i++) {
                    if (player.getColor() == players.get(i).getColor()) {
                        directIndex = i;
                        System.out.println("DEBUG setCurrentPlayer: Found player by COLOR at index: " + directIndex);
                        break;
                    }
                }
            }
            
            // If not found by color, try last resort with equals
            if (directIndex == -1) {
                for (int i = 0; i < players.size(); i++) {
                    if (players.get(i).equals(player)) {
                        directIndex = i;
                        System.out.println("DEBUG setCurrentPlayer: Found player by EQUALS at index: " + directIndex);
                        break;
                    }
                }
            }
            
            if (directIndex != -1) {
                this.currentPlayerIndex = directIndex;
                System.out.println("DEBUG setCurrentPlayer: Set currentPlayerIndex to: " + directIndex);
            } else {
                System.out.println("DEBUG setCurrentPlayer: WARNING - Player not found in players list");
                // If we still can't find the player, use the default ordering approach
                // Try to find the player by color in proper order: BLUE, YELLOW, GREEN, RED
                GameUser.PlayerColor targetColor = player.getColor();
                if (targetColor != null) {
                    GameUser.PlayerColor[] colorOrder = {
                        GameUser.PlayerColor.BLUE,
                        GameUser.PlayerColor.YELLOW, 
                        GameUser.PlayerColor.GREEN, 
                        GameUser.PlayerColor.RED
                    };
                    
                    int targetColorIndex = -1;
                    for (int i = 0; i < colorOrder.length; i++) {
                        if (colorOrder[i] == targetColor) {
                            targetColorIndex = i;
                            break;
                        }
                    }
                    
                    if (targetColorIndex >= 0) {
                        System.out.println("DEBUG setCurrentPlayer: Trying color order approach, target color index: " + targetColorIndex);
                        this.currentPlayerIndex = targetColorIndex;
                    } else {
                        this.currentPlayerIndex = 0;
                    }
                } else {
                    // Last resort
                    this.currentPlayerIndex = 0;
                }
            }
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