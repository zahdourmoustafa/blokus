package com.blokus.blokus.service;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;

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

    // Methods removed: 
    // initializePieces, initializePlayerPieces, isValidMove, placePiece, 
    // getAvailablePieces, getAllPieces, canPlayerMove, getStartCorner

} 