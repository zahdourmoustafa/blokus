package com.blokus.blokus.service;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;

/**
 * Service for bot/AI operations
 */
public interface BotService {
    
    /**
     * Make a move as a bot
     * 
     * @param gameId The game ID
     * @param bot The bot player
     * @return true if a move was made, false otherwise
     */
    boolean makeBotMove(Long gameId, GameUser bot);
    
    /**
     * Check if it's a bot's turn and make a move if needed
     * 
     * @param gameId The game ID
     * @return true if a bot move was made, false otherwise
     */
    boolean handleBotTurn(Long gameId);
    
    /**
     * Process a full round of bot moves (all bots that need to play)
     * 
     * @param game The game
     * @return Number of bot moves performed
     */
    int processBotRound(Game game);
} 