package com.blokus.blokus.service;

import java.util.List;

import com.blokus.blokus.dto.GameCreateDto;
import com.blokus.blokus.model.Game;

public interface GameService {
    
    /**
     * Creates a new game with the provided details
     * @param gameDto Data for creating a new game
     * @return The created game
     */
    Game createGame(GameCreateDto gameDto);
    
    /**
     * Finds all available games (with WAITING status)
     * @return List of available games
     */
    List<Game> findAvailableGames();
    
    /**
     * Finds a game by its ID
     * @param id Game ID
     * @return The game if found, null otherwise
     */
    Game findById(Long id);
    
    /**
     * Current user joins a game
     * @param gameId ID of the game to join
     * @return The joined game
     */
    Game joinGame(Long gameId);
    
    /**
     * Find games played by the current user
     * @return List of user's games
     */
    List<Game> findMyGames();
    
    /**
     * Start a game when all expected players have joined
     * @param gameId ID of the game to start
     * @return The started game
     */
    Game startGame(Long gameId);
} 