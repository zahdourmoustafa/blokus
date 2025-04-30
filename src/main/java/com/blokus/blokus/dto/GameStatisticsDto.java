package com.blokus.blokus.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GameStatisticsDto {

    private Long gameId;
    private String gameName;
    private LocalDateTime dateCompleted; // Or another relevant date field
    private String winnerUsername; // Can be null or specific string if no single winner
    private List<PlayerScoreDto> playerScores;

    // Inner class for player scores
    public static class PlayerScoreDto {
        private String username;
        private int score;

        public PlayerScoreDto(String username, int score) {
            this.username = username;
            this.score = score;
        }

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    // Constructor
    public GameStatisticsDto(Long gameId, String gameName, LocalDateTime dateCompleted, String winnerUsername, List<PlayerScoreDto> playerScores) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.dateCompleted = dateCompleted;
        this.winnerUsername = winnerUsername;
        this.playerScores = playerScores;
    }
    
    public GameStatisticsDto() {
    }

    // Getters and Setters
    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public LocalDateTime getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(LocalDateTime dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public void setWinnerUsername(String winnerUsername) {
        this.winnerUsername = winnerUsername;
    }

    public List<PlayerScoreDto> getPlayerScores() {
        return playerScores;
    }

    public void setPlayerScores(List<PlayerScoreDto> playerScores) {
        this.playerScores = playerScores;
    }
} 