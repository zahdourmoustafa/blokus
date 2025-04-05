package com.blokus.blokus.dto;

import java.util.List;

import com.blokus.blokus.model.GameUser.PlayerColor;
import com.blokus.blokus.model.Piece;

public class PlayerPiecesDTO {
    private Long userId;
    private String username;
    private PlayerColor color;
    private List<Piece> availablePieces;
    private List<Piece> placedPieces;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PlayerColor getColor() {
        return color;
    }

    public void setColor(PlayerColor color) {
        this.color = color;
    }

    public List<Piece> getAvailablePieces() {
        return availablePieces;
    }

    public void setAvailablePieces(List<Piece> availablePieces) {
        this.availablePieces = availablePieces;
    }

    public List<Piece> getPlacedPieces() {
        return placedPieces;
    }

    public void setPlacedPieces(List<Piece> placedPieces) {
        this.placedPieces = placedPieces;
    }
} 