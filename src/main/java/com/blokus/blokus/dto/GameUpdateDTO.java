package com.blokus.blokus.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for game updates sent through WebSockets
 */
public class GameUpdateDTO {
    
    private Long gameId;
    private String type; // "PIECE_PLACED", "NEXT_TURN", "GAME_OVER", etc.
    private String message;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
    
    public GameUpdateDTO() {
        this.timestamp = LocalDateTime.now();
    }
    
    public GameUpdateDTO(Long gameId, String type, String message, Map<String, Object> data) {
        this.gameId = gameId;
        this.type = type;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
} 