package com.blokus.blokus.service;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import java.util.List;
import java.util.Map;

/**
 * Service for game logic operations.
 * Methods related to specific board/piece implementation have been removed.
 */
public interface GameLogicService {
    
    /**
     * Get the current player whose turn it is.
     * (Implementation might need adjustment based on how turns are tracked without board state)
     * 
     * @param gameId The game ID
     * @return The GameUser whose turn it is, or null if not applicable.
     */
    GameUser getCurrentPlayer(Long gameId);
    
    /**
     * Move to the next player's turn.
     * (Implementation might need adjustment based on how turns are tracked)
     * 
     * @param gameId The game ID
     * @return The next player, or null if not applicable.
     */
    GameUser nextTurn(Long gameId);
    
    /**
     * Check if the game is over.
     * (Implementation might need adjustment based on game rules without specific board state)
     * 
     * @param gameId The game ID
     * @return true if game is over, false otherwise
     */
    boolean isGameOver(Long gameId);
    
    /**
     * Calculate scores for all players.
     * (Implementation might need adjustment based on scoring rules)
     * 
     * @param gameId The game ID
     * @return The game with updated scores
     */
    Game calculateScores(Long gameId);

    /**
     * Place a piece on the game board
     * 
     * @param gameId The game ID
     * @param userId The user ID placing the piece
     * @param pieceId The ID of the piece to place
     * @param pieceColor The color of the piece
     * @param x The x-coordinate on the board
     * @param y The y-coordinate on the board
     * @param rotation The rotation of the piece (0, 90, 180, 270)
     * @param flipped Whether the piece is flipped
     * @return true if the piece was placed successfully, false otherwise
     */
    boolean placePiece(Long gameId, Long userId, String pieceId, String pieceColor, 
                       int x, int y, Integer rotation, Boolean flipped);

    /**
     * Get all pieces that have been placed in the game
     * 
     * @param gameId The game ID
     * @return A list of maps containing information about each placed piece
     */
    List<Map<String, Object>> getPlacedPieces(Long gameId);

    /**
     * Check if the given player can make any legal move with their remaining pieces
     * @param player The GameUser to check
     * @param gameId The game ID
     * @return true if the player can move, false otherwise
     */
    boolean canPlayerMove(GameUser player, Long gameId);

    // Methods removed: 
    // initializePieces, initializePlayerPieces, isValidMove, placePiece, 
    // getAvailablePieces, getAllPieces, canPlayerMove, getStartCorner

} 