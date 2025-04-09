package com.blokus.blokus.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blokus.blokus.dto.GameCreateDto;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.Game.GameMode;
import com.blokus.blokus.model.Game.GameStatus;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.GameUser.PlayerColor;
import com.blokus.blokus.model.User;
import com.blokus.blokus.repository.GameRepository;
import com.blokus.blokus.repository.GameUserRepository;
import com.blokus.blokus.service.GameService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implémentation du service de gestion des parties
 */
@Service
public class GameServiceImpl implements GameService {

    private final GameRepository gameRepository;
    private final GameUserRepository gameUserRepository;

    public GameServiceImpl(GameRepository gameRepository,
            GameUserRepository gameUserRepository) {
        this.gameRepository = gameRepository;
        this.gameUserRepository = gameUserRepository;
    }

    @Override
    @Transactional
    public Game createGame(GameCreateDto gameDto, User creator) {
        // Check if a game with the same name already exists
        if (gameRepository.findByName(gameDto.getName()).isPresent()) {
            throw new IllegalStateException("Une partie avec ce nom existe déjà. Veuillez choisir un autre nom.");
        }
        
        // Create new game with parameters from DTO
        Game game = new Game();
        game.setName(gameDto.getName());
        game.setExpectedPlayers(gameDto.getMaxPlayers());
        game.setMode(gameDto.isTimedMode() ? GameMode.TIMED : GameMode.CLASSIC);
        // Creation date is set automatically via @PrePersist in Game entity
        
        // Save game first to get ID
        game = gameRepository.save(game);
        
        // Create game-user relationship
        GameUser gameUser = new GameUser();
        gameUser.setGame(game);
        gameUser.setUser(creator);
        gameUser.setColor(PlayerColor.BLUE); // First player gets blue
        
        gameUserRepository.save(gameUser);
        
        // Add AI players to fill the remaining slots (up to 4 total players)
        int totalPlayers = 4; // Blokus is always played with 4 players
        int aiPlayersNeeded = totalPlayers - game.getExpectedPlayers();
        
        // Add AI players with the remaining colors
        PlayerColor[] aiColors = {PlayerColor.RED, PlayerColor.GREEN}; // Default for 2 human players
        if (game.getExpectedPlayers() == 1) {
             aiColors = new PlayerColor[]{PlayerColor.YELLOW, PlayerColor.RED, PlayerColor.GREEN};
        } else if (game.getExpectedPlayers() == 3) {
            aiColors = new PlayerColor[]{PlayerColor.GREEN};
        } // No AI needed if 4 players
        
        // Correct loop limit
        aiPlayersNeeded = Math.min(aiPlayersNeeded, aiColors.length);
        
        for (int i = 0; i < aiPlayersNeeded; i++) {
            GameUser aiPlayer = new GameUser();
            aiPlayer.setGame(game);
            aiPlayer.setBot(true);
            aiPlayer.setColor(aiColors[i]);
            gameUserRepository.save(aiPlayer);
        }
        
        return game;
    }

    @Override
    public Game findById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + id));
    }

    @Override
    public List<Game> findAvailableGames() {
        return gameRepository.findByStatus(GameStatus.WAITING);
    }

    @Override
    @Transactional
    public Game joinGame(Long gameId, User user) {
        Game game = findById(gameId);
        
        // Check if game can be joined
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("La partie n'est pas en attente de joueurs");
        }
        
        // Check if user is already in the game
        Optional<GameUser> existingGameUser = gameUserRepository.findByGameIdAndUserId(gameId, user.getId());
        if (existingGameUser.isPresent()) {
            throw new IllegalStateException("Vous êtes déjà dans cette partie");
        }
        
        // Check if game is full (expected human players)
        List<GameUser> humanPlayers = gameUserRepository.findByGameId(gameId).stream()
                .filter(p -> !p.isBot() && p.getUser() != null)
                .collect(Collectors.toList());
        
        if (humanPlayers.size() >= game.getExpectedPlayers()) {
            throw new IllegalStateException("La partie est déjà complète");
        }
        
        // Add user to game with appropriate color
        GameUser gameUser = new GameUser();
        gameUser.setGame(game);
        gameUser.setUser(user);
        
        // Assign color based on join order (for human players)
        PlayerColor[] colors = {PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED, PlayerColor.GREEN};
        gameUser.setColor(colors[humanPlayers.size()]);
        
        gameUserRepository.save(gameUser);
        
        // Check if all expected human players have joined
        if (isGameReadyToStart(gameId)) {
            startGame(gameId);
        }
        
        return game;
    }

    @Override
    public List<Game> findUserGames(Long userId) {
        return gameUserRepository.findByUserId(userId).stream()
                .map(GameUser::getGame)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isGameReadyToStart(Long gameId) {
        Game game = findById(gameId);
        // Count only human players (not bots)
        int humanPlayerCount = (int) gameUserRepository.findByGameId(gameId).stream()
                .filter(p -> !p.isBot() && p.getUser() != null)
                .count();
        return humanPlayerCount >= game.getExpectedPlayers();
    }

    @Override
    @Transactional
    public Game startGame(Long gameId) {
        Game game = findById(gameId);
        
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("La partie n'est pas en attente de joueurs");
        }
        
        game.setStatus(GameStatus.PLAYING);
        return gameRepository.save(game);
    }
    
    @Override
    @Transactional
    public Game leaveGame(Long gameId, User user) {
        Game game = findById(gameId);
        
        // Vérifier si la partie peut être quittée
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("Vous ne pouvez pas quitter une partie en cours ou terminée");
        }
        
        // Trouver la relation joueur-partie
        GameUser gameUser = gameUserRepository.findByGameIdAndUserId(gameId, user.getId())
                .orElseThrow(() -> new IllegalStateException("Vous n'êtes pas dans cette partie"));
        
        // Si c'est le créateur (premier joueur), supprimer la partie
        List<GameUser> players = gameUserRepository.findByGameId(gameId);
        if (players.size() == 1 || players.get(0).getUser().getId().equals(user.getId())) {
            // Supprimer la partie complètement
            gameRepository.delete(game);
            return null;
        } else {
            // Sinon, retirer le joueur de la partie
            gameUserRepository.delete(gameUser);
            return game;
        }
    }
    
    @Override
    @Transactional
    public List<GameUser> getGameParticipants(Long gameId) {
        return gameUserRepository.findByGameId(gameId);
    }
    
    @Override
    @Transactional
    public Game addBotToGame(Long gameId) {
        Game game = findById(gameId);
        
        // Vérifier si la partie peut accueillir un bot
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("Impossible d'ajouter un bot à une partie qui n'est pas en attente");
        }
        
        // Get all current players (human and AI)
        List<GameUser> allPlayers = gameUserRepository.findByGameId(gameId);
        
        // Count AI players
        List<GameUser> aiPlayers = allPlayers.stream()
                .filter(GameUser::isBot)
                .collect(Collectors.toList());
        
        // Calculate maximum number of AI players needed
        int totalPlayersNeeded = 4; // Blokus is always played with 4 players
        int maxAiPlayers = totalPlayersNeeded - game.getExpectedPlayers();
        
        // Check if we already have enough AI players
        if (aiPlayers.size() >= maxAiPlayers) {
            throw new IllegalStateException("Le nombre maximum de bots a déjà été atteint");
        }
        
        // Determine which colors are already taken
        List<PlayerColor> takenColors = allPlayers.stream()
                .map(GameUser::getColor)
                .collect(Collectors.toList());
        
        // Find the next available color
        PlayerColor botColor = null;
        for (PlayerColor color : PlayerColor.values()) {
            if (!takenColors.contains(color)) {
                botColor = color;
                break;
            }
        }
        
        if (botColor == null) {
            throw new IllegalStateException("Aucune couleur disponible pour le bot");
        }
        
        // Créer un bot
        GameUser botUser = new GameUser();
        botUser.setGame(game);
        botUser.setBot(true);
        botUser.setColor(botColor);
        
        gameUserRepository.save(botUser);
        
        return game;
    }
    
    @Override
    @Transactional
    public Game cancelGame(Long gameId, User user) {
        Game game = findById(gameId);
        
        // Vérifier si la partie peut être annulée
        if (game.getStatus() != GameStatus.WAITING) {
            throw new IllegalStateException("Impossible d'annuler une partie qui n'est pas en attente");
        }
        
        // Vérifier si l'utilisateur est le créateur
        List<GameUser> players = gameUserRepository.findByGameId(gameId);
        if (players.isEmpty() || !players.get(0).getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Seul le créateur de la partie peut l'annuler");
        }
        
        // Supprimer la partie
        gameRepository.delete(game);
        return null;
    }
} 