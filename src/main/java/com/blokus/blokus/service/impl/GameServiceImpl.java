package com.blokus.blokus.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blokus.blokus.dto.GameCreateDto;
import com.blokus.blokus.model.Board;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.Game.GameStatus;
import com.blokus.blokus.model.Game.GameMode;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.GameUser.PlayerColor;
import com.blokus.blokus.model.User;
import com.blokus.blokus.repository.BoardRepository;
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
    private final BoardRepository boardRepository;

    public GameServiceImpl(GameRepository gameRepository,
            GameUserRepository gameUserRepository, BoardRepository boardRepository) {
        this.gameRepository = gameRepository;
        this.gameUserRepository = gameUserRepository;
        this.boardRepository = boardRepository;
    }

    @Override
    @Transactional
    public Game createGame(GameCreateDto gameDto, User creator) {
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
        
        // Initialize empty board
        Board board = new Board();
        board.setGame(game);
        // Board is already initialized with an empty grid in its constructor
        boardRepository.save(board);
        
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
        
        // Check if game is full
        List<GameUser> currentPlayers = gameUserRepository.findByGameId(gameId);
        if (currentPlayers.size() >= game.getExpectedPlayers()) {
            throw new IllegalStateException("La partie est déjà complète");
        }
        
        // Add user to game with appropriate color
        GameUser gameUser = new GameUser();
        gameUser.setGame(game);
        gameUser.setUser(user);
        
        // Assign color based on join order
        PlayerColor[] colors = {PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED, PlayerColor.GREEN};
        gameUser.setColor(colors[currentPlayers.size()]);
        
        gameUserRepository.save(gameUser);
        
        // Check if game should start automatically
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
        int playerCount = gameUserRepository.countByGameId(gameId);
        return playerCount >= game.getExpectedPlayers();
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
        
        List<GameUser> currentPlayers = gameUserRepository.findByGameId(gameId);
        if (currentPlayers.size() >= game.getExpectedPlayers()) {
            throw new IllegalStateException("La partie est déjà complète");
        }
        
        // Créer un bot
        GameUser botUser = new GameUser();
        botUser.setGame(game);
        botUser.setBot(true);
        
        // Assigner une couleur en fonction de l'ordre de rejointe
        PlayerColor[] colors = {PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.RED, PlayerColor.GREEN};
        botUser.setColor(colors[currentPlayers.size()]);
        
        gameUserRepository.save(botUser);
        
        // Vérifier si la partie devrait démarrer automatiquement
        if (isGameReadyToStart(gameId)) {
            startGame(gameId);
        }
        
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