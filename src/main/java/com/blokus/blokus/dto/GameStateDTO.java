package com.blokus.blokus.dto;

import java.util.List;

import com.blokus.blokus.model.Board;
import com.blokus.blokus.model.GameUser.PlayerColor;

public class GameStateDTO {
    private Board board;
    private Long currentPlayerId;
    private PlayerColor currentPlayerColor;
    private boolean isGameOver;
    private List<PlayerPiecesDTO> allPlayerPieces;

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public Long getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setCurrentPlayerId(Long currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }

    public PlayerColor getCurrentPlayerColor() {
        return currentPlayerColor;
    }

    public void setCurrentPlayerColor(PlayerColor currentPlayerColor) {
        this.currentPlayerColor = currentPlayerColor;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }

    public List<PlayerPiecesDTO> getAllPlayerPieces() {
        return allPlayerPieces;
    }

    public void setAllPlayerPieces(List<PlayerPiecesDTO> allPlayerPieces) {
        this.allPlayerPieces = allPlayerPieces;
    }
} 