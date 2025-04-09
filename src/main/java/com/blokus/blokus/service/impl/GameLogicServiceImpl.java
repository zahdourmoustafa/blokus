package com.blokus.blokus.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.Game.GameStatus;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.repository.GameRepository;
import com.blokus.blokus.repository.GameUserRepository;
import com.blokus.blokus.service.GameLogicService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implementation of game logic service.
 * Methods related to the removed board/piece implementation have been deleted.
 */
@Service
public class GameLogicServiceImpl implements GameLogicService {

    private final GameRepository gameRepository;
    private final GameUserRepository gameUserRepository;

    public GameLogicServiceImpl(GameRepository gameRepository, GameUserRepository gameUserRepository) {
        this.gameRepository = gameRepository;
        this.gameUserRepository = gameUserRepository;
    }

    @Override
    public GameUser getCurrentPlayer(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));

        if (game.getStatus() != GameStatus.PLAYING) {
            return null;
        }

        List<GameUser> players = game.getPlayers();
        if (players.isEmpty()) {
            return null;
        }

        // Determine current player based on turns taken or a dedicated field (Needs implementation)
        // Placeholder: return first player for now
        // int currentTurn = game.getCurrentTurn(); // Assuming Game entity has get/setCurrentTurn
        // return players.get(currentTurn % players.size());
        return players.get(0); // Simple placeholder
    }

    @Override
    @Transactional
    public GameUser nextTurn(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));

        if (game.getStatus() != GameStatus.PLAYING) {
            // Consider throwing an exception or returning null
            throw new IllegalStateException("Cannot advance turn, game is not in PLAYING state.");
        }

        List<GameUser> players = game.getPlayers();
        if (players.size() < 2) {
            // Not enough players to advance turn
            return getCurrentPlayer(gameId);
        }
        
        // Placeholder logic: Turn advancement needs real implementation
        // int currentTurn = game.getCurrentTurn(); // Assuming Game entity has get/setCurrentTurn
        // int nextTurnIndex = (currentTurn + 1) % players.size();
        
        // Update the game's current turn counter
        // game.setCurrentTurn(nextTurnIndex);
        // gameRepository.save(game);
        
        // Return null or first player as placeholder - real next player logic needed
        // return players.get(nextTurnIndex);
        return null; // Indicate logic is incomplete
    }

    @Override
    public boolean isGameOver(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));

        if (game.getStatus() == GameStatus.FINISHED) {
            return true;
        }
        if (game.getStatus() != GameStatus.PLAYING) {
            return false; // Only playing games can become over
        }
        
        // Simplified Game Over Condition: 
        // If no player can make a move (needs canPlayerMove re-implementation based on new rules)
        // For now, let's assume game over if all players have passed consecutively (needs tracking)
        // Or based on a round limit, etc.
        // Placeholder: return false
        return false; 
    }

    @Override
    @Transactional
    public Game calculateScores(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));

        if (game.getStatus() != GameStatus.FINISHED) {
            // Optionally force game to finish first or throw error
             game.setStatus(GameStatus.FINISHED);
             //log.warn("Calculating scores for a game that is not finished. Forcing status to FINISHED.");
        }

        // Scoring logic needs to be re-implemented based on the new game rules (if any)
        // Score might depend on turns taken, objectives met, etc.
        for (GameUser player : game.getPlayers()) {
            int score = 0; 
            // Placeholder: Assign a dummy score or implement new logic
            // score = calculatePlayerScore(gameId, player); // Needs re-implementation
            player.setScore(score); 
            gameUserRepository.save(player);
        }
        
        return gameRepository.save(game);
    }
} 