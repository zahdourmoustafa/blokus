package com.blokus.blokus.service.impl;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blokus.blokus.dto.GameCreateDto;
import com.blokus.blokus.model.Board;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.User;
import com.blokus.blokus.repository.GameRepository;
import com.blokus.blokus.repository.UserRepository;
import com.blokus.blokus.service.GameService;

@Service
public class GameServiceImpl implements GameService {

    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public GameServiceImpl(GameRepository gameRepository,
                           UserRepository userRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Game createGame(GameCreateDto gameDto) {
        User currentUser = getCurrentUser();
        
        // Create a new game with the provided details
        Game game = new Game();
        game.setName(gameDto.getName());
        game.setExpectedPlayers(gameDto.getExpectedPlayers());
        game.setMode(gameDto.getMode());
        
        // Initialize and associate a new board
        Board board = new Board();
        board.setGame(game);
        game.setBoard(board);
        
        // Save the game to get an ID
        game = gameRepository.save(game);
        
        // Create GameUser (player) entry for the creator
        GameUser creator = new GameUser();
        creator.setUser(currentUser);
        creator.setGame(game);
        creator.setColor(GameUser.PlayerColor.BLUE); // First player gets blue
        
        game.addPlayer(creator);
        
        // Save again with the creator added
        return gameRepository.save(game);
    }

    @Override
    public List<Game> findAvailableGames() {
        return gameRepository.findAvailableGames();
    }

    @Override
    public Game findById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Game not found with id: " + id));
    }

    @Override
    @Transactional
    public Game joinGame(Long gameId) {
        User currentUser = getCurrentUser();
        Game game = findById(gameId);
        
        // Check if game is joinable
        if (game.getStatus() != Game.GameStatus.WAITING) {
            throw new IllegalStateException("This game is no longer accepting players");
        }
        
        // Check if the user is already in the game
        Optional<GameUser> existingPlayer = game.getPlayers().stream()
                .filter(player -> player.getUser().getId().equals(currentUser.getId()))
                .findFirst();
        
        if (existingPlayer.isPresent()) {
            return game; // User is already in this game
        }
        
        // Check if there's room for more players
        if (game.getPlayers().size() >= game.getExpectedPlayers()) {
            throw new IllegalStateException("This game is already full");
        }
        
        // Assign the next available color based on player count
        GameUser.PlayerColor[] colors = GameUser.PlayerColor.values();
        GameUser.PlayerColor assignedColor = colors[game.getPlayers().size() % colors.length];
        
        // Create and add the new player
        GameUser newPlayer = new GameUser();
        newPlayer.setUser(currentUser);
        newPlayer.setGame(game);
        newPlayer.setColor(assignedColor);
        
        game.addPlayer(newPlayer);
        
        // Check if we should start the game now
        if (game.getPlayers().size() >= game.getExpectedPlayers()) {
            game.setStatus(Game.GameStatus.PLAYING);
        }
        
        return gameRepository.save(game);
    }

    @Override
    public List<Game> findMyGames() {
        User currentUser = getCurrentUser();
        return gameRepository.findGamesByUserId(currentUser.getId());
    }

    @Override
    @Transactional
    public Game startGame(Long gameId) {
        Game game = findById(gameId);
        
        if (game.getStatus() != Game.GameStatus.WAITING) {
            throw new IllegalStateException("Game cannot be started with status: " + game.getStatus());
        }
        
        if (game.getPlayers().size() < 2) {
            throw new IllegalStateException("Cannot start game with fewer than 2 players");
        }
        
        // Generate AI players if needed
        generateAIPlayersIfNeeded(game);
        
        game.setStatus(Game.GameStatus.PLAYING);
        return gameRepository.save(game);
    }
    
    /**
     * Generates AI players to fill the game if needed
     * @param game The game to add AI players to
     */
    private void generateAIPlayersIfNeeded(Game game) {
        int humanPlayersCount = game.getPlayers().size();
        int aiPlayersNeeded = 4 - humanPlayersCount; // Blokus is played by 4 players
        
        if (aiPlayersNeeded <= 0) {
            return; // No AI players needed
        }
        
        // Find the system user for AI (or create one if it doesn't exist)
        User aiUser = getOrCreateAIUser();
        
        // Create AI players with the remaining colors
        GameUser.PlayerColor[] colors = GameUser.PlayerColor.values();
        for (int i = 0; i < aiPlayersNeeded; i++) {
            GameUser aiPlayer = new GameUser();
            aiPlayer.setUser(aiUser);
            aiPlayer.setGame(game);
            aiPlayer.setBot(true);
            
            // Assign next available color
            aiPlayer.setColor(colors[(humanPlayersCount + i) % colors.length]);
            
            game.addPlayer(aiPlayer);
        }
    }
    
    /**
     * Gets or creates a user for AI players
     * @return User entity for AI
     */
    private User getOrCreateAIUser() {
        String aiUsername = "ai_system";
        
        return userRepository.findByUsername(aiUsername)
                .orElseGet(() -> {
                    // Create a system user for AI if it doesn't exist
                    User aiUser = new User();
                    aiUser.setUsername(aiUsername);
                    aiUser.setEmail("ai@blokus.com");
                    aiUser.setPassword("$2a$10$AI_SYSTEM_PASSWORD"); // This password is not usable
                    
                    return userRepository.save(aiUser);
                });
    }
    
    /**
     * Helper method to get the current authenticated user
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        // Get username from the authentication object
        String username = auth.getName();
        
        // Find the corresponding User entity
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
} 