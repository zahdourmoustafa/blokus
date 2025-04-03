package com.blokus.blokus.service;

import com.blokus.blokus.model.Board;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;

import java.util.List;

/**
 * Service for game logic operations
 */
public interface GameLogicService {
    
    /**
     * Initialize pieces for all players in a game
     * 
     * @param gameId The game ID
     * @return List of created pieces
     */
    List<Piece> initializePieces(Long gameId);
    
    /**
     * Check if a move is valid according to game rules
     * 
     * @param gameId The game ID
     * @param pieceId The piece ID
     * @param x The x coordinate
     * @param y The y coordinate
     * @param rotation The rotation (0, 90, 180, 270)
     * @param flipped Whether the piece is flipped or not
     * @return true if the move is valid, false otherwise
     */
    boolean isValidMove(Long gameId, Long pieceId, int x, int y, int rotation, boolean flipped);
    
    /**
     * Place a piece on the board
     * 
     * @param gameId The game ID
     * @param pieceId The piece ID
     * @param x The x coordinate
     * @param y The y coordinate
     * @param rotation The rotation (0, 90, 180, 270)
     * @param flipped Whether the piece is flipped or not
     * @return The updated board
     */
    Board placePiece(Long gameId, Long pieceId, int x, int y, int rotation, boolean flipped);
    
    /**
     * Get available pieces for a player
     * 
     * @param gameId The game ID
     * @param userId The user ID
     * @return List of available pieces
     */
    List<Piece> getAvailablePieces(Long gameId, Long userId);
    
    /**
     * Get the current player's turn
     * 
     * @param gameId The game ID
     * @return The GameUser whose turn it is
     */
    GameUser getCurrentPlayer(Long gameId);
    
    /**
     * Check if a player can make any valid move
     * 
     * @param gameId The game ID
     * @param userId The user ID
     * @return true if player can make a move, false otherwise
     */
    boolean canPlayerMove(Long gameId, Long userId);
    
    /**
     * Move to the next player's turn
     * 
     * @param gameId The game ID
     * @return The next player
     */
    GameUser nextTurn(Long gameId);
    
    /**
     * Check if the game is over
     * 
     * @param gameId The game ID
     * @return true if game is over, false otherwise
     */
    boolean isGameOver(Long gameId);
    
    /**
     * Calculate scores for all players
     * 
     * @param gameId The game ID
     * @return The game with updated scores
     */
    Game calculateScores(Long gameId);
    
    /**
     * Get the starting corner position for a player
     * 
     * @param color The player color
     * @return Array with [x, y] coordinates
     */
    int[] getStartCorner(GameUser.PlayerColor color);
} 