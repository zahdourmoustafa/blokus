package com.blokus.blokus.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.DisposableBean;

import com.blokus.blokus.dto.GameCreateDto;
import com.blokus.blokus.dto.GameStatisticsDto;
import com.blokus.blokus.dto.GameStatisticsDto.PlayerScoreDto;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.Game.GameMode;
import com.blokus.blokus.model.Game.GameStatus;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.GameUser.PlayerColor;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.model.PieceFactory;
import com.blokus.blokus.model.User;
import com.blokus.blokus.repository.GameRepository;
import com.blokus.blokus.repository.GameUserRepository;
import com.blokus.blokus.service.GameLogicService;
import com.blokus.blokus.service.GameService;
import com.blokus.blokus.service.GameWebSocketService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implémentation du service de gestion des parties
 */
@Service
public class GameServiceImpl implements GameService, DisposableBean {

    private final GameRepository gameRepository;
    private final GameUserRepository gameUserRepository;
    private final GameLogicService gameLogicService;
    private final GameWebSocketService gameWebSocketService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<Long, ScheduledFuture<?>> gameTimers = new ConcurrentHashMap<>();

    public GameServiceImpl(GameRepository gameRepository,
            GameUserRepository gameUserRepository,
            GameLogicService gameLogicService,
            GameWebSocketService gameWebSocketService) {
        this.gameRepository = gameRepository;
        this.gameUserRepository = gameUserRepository;
        this.gameLogicService = gameLogicService;
        this.gameWebSocketService = gameWebSocketService;
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
        
        // Ensure minimum 2 players
        int requestedPlayers = gameDto.getMaxPlayers();
        if (requestedPlayers < 2) {
            requestedPlayers = 2; // Enforce minimum 2 players
        }
        game.setExpectedPlayers(requestedPlayers);
        
        game.setMode(gameDto.isTimedMode() ? GameMode.TIMED : GameMode.CLASSIC);
        game.setStatus(GameStatus.WAITING); // Explicitly set status to WAITING
        // Creation date is set automatically via @PrePersist in Game entity
        
        // Save game first to get ID
        game = gameRepository.save(game);
        
        // Create game-user relationship for creator (always BLUE)
        GameUser gameUserCreator = new GameUser();
        gameUserCreator.setGame(game);
        gameUserCreator.setUser(creator);
        gameUserCreator.setColor(PlayerColor.BLUE); // First player gets blue
        
        // Initialize creator's pieces
        Set<String> creatorPieceIds = PieceFactory.createPieces(PlayerColor.BLUE.name().toLowerCase())
                .stream()
                .map(Piece::getId) // Use method reference
                .map(String::valueOf)
                .collect(Collectors.toSet());
        gameUserCreator.setAvailablePieceIds(creatorPieceIds);
        
        gameUserRepository.save(gameUserCreator); // Save creator
        
        // STANDARD BLOKUS ORDER IS: BLUE, YELLOW, GREEN, RED
        // Debug add AI players
        System.out.println("Adding AI players to game " + game.getId() + ", expected human players: " + game.getExpectedPlayers());
        
        // ALWAYS create AI players for all colors not used by human players
        // Get existing colors
        List<PlayerColor> assignedColors = new ArrayList<>();
        assignedColors.add(PlayerColor.BLUE); // Creator has BLUE
        
        // For 2-player games, we expect a second human to join as YELLOW
        // So we only add bots for GREEN and RED
        // YELLOW will be assigned when the second player joins
        
        // REMOVE: Don't add a YELLOW bot for 2-player games
        // The second human player should get the YELLOW color
        // if (game.getExpectedPlayers() == 2) {
        //     GameUser yellowBot = new GameUser();
        //     yellowBot.setGame(game);
        //     yellowBot.setBot(true);
        //     yellowBot.setColor(PlayerColor.YELLOW);
        //     gameUserRepository.save(yellowBot);
        //     assignedColors.add(PlayerColor.YELLOW);
        //     System.out.println("Added YELLOW bot for 2-player game to ensure all colors are present");
        // }

        // Make it explicit that we're expecting a human YELLOW player
        if (game.getExpectedPlayers() == 2) {
            System.out.println("Expecting second human player to join as YELLOW");
        }
        
        // Modified bot creation to initialize pieces
        if (game.getExpectedPlayers() < 3) { // If expected players is 2, add GREEN bot
            GameUser greenBot = createAndInitializeBot(game, PlayerColor.GREEN);
            gameUserRepository.save(greenBot);
            assignedColors.add(PlayerColor.GREEN);
            System.out.println("Added GREEN bot with pieces");
        }
        
        if (game.getExpectedPlayers() < 4) { // If expected players is 2 or 3, add RED bot
            GameUser redBot = createAndInitializeBot(game, PlayerColor.RED);
            gameUserRepository.save(redBot);
            assignedColors.add(PlayerColor.RED);
            System.out.println("Added RED bot with pieces");
        }
        
        System.out.println("Final player setup: " + assignedColors);
        System.out.println("Waiting for " + (game.getExpectedPlayers() - 1) + " more human players to join");
        
        return game;
    }

    // Helper method to create and initialize a bot user
    private GameUser createAndInitializeBot(Game game, PlayerColor color) {
        GameUser bot = new GameUser();
        bot.setGame(game);
        bot.setBot(true);
        bot.setColor(color);
        
        // Initialize bot's pieces
        Set<String> botPieceIds = PieceFactory.createPieces(color.name().toLowerCase())
                .stream()
                .map(Piece::getId)
                .map(String::valueOf)
                .collect(Collectors.toSet());
        
        System.out.println("==== BOT INITIALIZATION DEBUG ====");
        System.out.println("Created bot with color: " + color);
        System.out.println("Initializing " + botPieceIds.size() + " pieces:");
        botPieceIds.forEach(id -> System.out.println("  - Piece ID: " + id));
        System.out.println("==== END BOT INITIALIZATION DEBUG ====");
        
        bot.setAvailablePieceIds(botPieceIds);
        
        return bot;
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
        GameUser gameUserJoiner = new GameUser();
        gameUserJoiner.setGame(game);
        gameUserJoiner.setUser(user);
        
        // Find colors already assigned to human players
        List<PlayerColor> takenHumanColors = humanPlayers.stream()
                .map(GameUser::getColor)
                .collect(Collectors.toList());
        
        // Check how many human players already exist to assign the appropriate color
        int humanPlayerCount = humanPlayers.size();
        
        System.out.println("Assigning color for new player. Current human player count: " + humanPlayerCount);
        
        // In a 2-player game, first player is BLUE, second player is YELLOW
        PlayerColor assignedColor = switch (humanPlayerCount) {
            case 0 -> {
                System.out.println("Assigning BLUE to first human player");
                yield PlayerColor.BLUE;
            }
            case 1 -> {
                System.out.println("Assigning YELLOW to second human player");
                yield PlayerColor.YELLOW;
            }
            case 2 -> {
                System.out.println("Assigning GREEN to third human player");
                yield PlayerColor.GREEN;
            }
            default -> {
                System.out.println("Assigning RED to fourth human player");
                yield PlayerColor.RED;
            }
        };
        
        // If the color is already taken by another human player, find the next available
        if (takenHumanColors.contains(assignedColor)) {
            System.out.println("Color " + assignedColor + " is already taken by a human player");
            
            // Find the first available color in the standard order
            for (PlayerColor color : new PlayerColor[] {PlayerColor.BLUE, PlayerColor.YELLOW, PlayerColor.GREEN, PlayerColor.RED}) {
                if (!takenHumanColors.contains(color)) {
                    assignedColor = color;
                    System.out.println("Reassigning to next available color: " + assignedColor);
                    break;
                }
            }
        }

        gameUserJoiner.setColor(assignedColor);
        
        // Initialize joining player's pieces
        Set<String> joinerPieceIds = PieceFactory.createPieces(assignedColor.name().toLowerCase())
                .stream()
                .map(Piece::getId)
                .map(String::valueOf)
                .collect(Collectors.toSet());
        gameUserJoiner.setAvailablePieceIds(joinerPieceIds);
        
        gameUserRepository.save(gameUserJoiner); // Save joining player
        
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
            throw new IllegalStateException("La partie n'est pas en état d'attente");
        }
        
        List<GameUser> players = gameUserRepository.findByGameId(gameId);
        if (players.isEmpty()) {
            throw new IllegalStateException("Pas de joueurs dans la partie");
        }
        
        // Debug
        System.out.println("Starting game: " + game.getId() + " with " + players.size() + " players");
        
        // Sort players by color to ensure consistent turn order
        // Standard Blokus order: BLUE, YELLOW, GREEN, RED
        List<GameUser> sortedPlayers = players.stream()
                .sorted((p1, p2) -> {
                    if (p1.getColor() == null || p2.getColor() == null) {
                        return 0;
                    }
                    // Get numeric order for color
                    int order1 = getColorOrder(p1.getColor());
                    int order2 = getColorOrder(p2.getColor());
                    return Integer.compare(order1, order2);
                })
                .collect(Collectors.toList());
        
        // Print player order debug info
        System.out.println("PLAYER ORDER IN GAME:");
        for (int i = 0; i < sortedPlayers.size(); i++) {
            GameUser player = sortedPlayers.get(i);
            String playerName = player.isBot() ? "Bot " + player.getColor() : 
                                               (player.getUser() != null ? player.getUser().getUsername() : "Unknown");
            System.out.println(i + ": " + playerName + " (" + player.getColor() + ")");
        }
        
        // Check if YELLOW color is missing - we need it for proper turn order
        boolean hasYellow = false;
        
        for (GameUser player : sortedPlayers) {
            if (player.getColor() == PlayerColor.YELLOW) {
                hasYellow = true;
                break;
            }
        }
        
        // Make sure all colors are represented (important for turn order)
        if (!hasYellow) {
            // Only add a Yellow bot if there are fewer than 2 human players
            List<GameUser> humanPlayers = sortedPlayers.stream()
                    .filter(p -> !p.isBot() && p.getUser() != null)
                    .collect(Collectors.toList());
            
            if (humanPlayers.size() < 2) {
                System.out.println("WARNING: No YELLOW player found, adding YELLOW bot as no second human player joined");
                GameUser yellowBot = new GameUser();
                yellowBot.setGame(game);
                yellowBot.setBot(true);
                yellowBot.setColor(PlayerColor.YELLOW);
                gameUserRepository.save(yellowBot);
                sortedPlayers.add(yellowBot); // Add to sorted list
                // Re-sort the list to maintain order
                sortedPlayers = sortedPlayers.stream()
                        .sorted((p1, p2) -> {
                            if (p1.getColor() == null || p2.getColor() == null) {
                                return 0;
                            }
                            int order1 = getColorOrder(p1.getColor());
                            int order2 = getColorOrder(p2.getColor());
                            return Integer.compare(order1, order2);
                        })
                        .collect(Collectors.toList());
            } else {
                System.out.println("ERROR: Two human players but no YELLOW player! Check color assignment logic.");
            }
        }
        
        // Start the game
        game.setStatus(GameStatus.PLAYING);
        
        // Set first player (always BLUE)
        GameUser firstPlayer = null;
        for (GameUser player : sortedPlayers) {
            if (player.getColor() == PlayerColor.BLUE) {
                firstPlayer = player;
                break;
            }
        }
        
        if (firstPlayer == null && !sortedPlayers.isEmpty()) {
            firstPlayer = sortedPlayers.get(0);
            System.out.println("WARNING: No BLUE player found! Using first player as starting player: " + 
                             firstPlayer.getColor());
        }
        
        if (firstPlayer != null) {
            game.setCurrentPlayer(firstPlayer);
            System.out.println("First player set to: " + 
                             (firstPlayer.isBot() ? "Bot " + firstPlayer.getColor() : 
                              (firstPlayer.getUser() != null ? firstPlayer.getUser().getUsername() : "Unknown")) + 
                             " with color " + firstPlayer.getColor());
            if (game.getMode() == GameMode.TIMED) {
                game.setTurnStartTime(LocalDateTime.now());
                // Game will be saved below, then schedule timer
            }
        } else {
            System.out.println("ERROR: No players available to set as first player");
        }
        
        Game savedGame = gameRepository.save(game); // Save game first

        if (savedGame.getMode() == GameMode.TIMED && savedGame.getStatus() == GameStatus.PLAYING) {
            scheduleTurnTimer(savedGame); // Schedule timer after game is saved and status is PLAYING
        }
        
        return savedGame;
    }
    
    /**
     * Get the ordinal value for a color based on standard Blokus order
     */
    private int getColorOrder(PlayerColor color) {
        return switch (color) {
            case BLUE -> 1;
            case YELLOW -> 2;
            case GREEN -> 3;
            case RED -> 4;
            default -> 99; // Any other colors (shouldn't happen)
        };
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
        
        // Créer un bot USING THE HELPER METHOD to ensure pieces are initialized
        GameUser botUser = createAndInitializeBot(game, botColor);
        // botUser.setGame(game); // Already done in helper
        // botUser.setBot(true); // Already done in helper
        // botUser.setColor(botColor); // Already done in helper
        
        gameUserRepository.save(botUser);
        System.out.println("Added " + botColor + " bot with pieces initialized."); // Added log
        
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
    
    @Override
    @Transactional
    public boolean placePiece(Long gameId, Long userId, String pieceId, String pieceColor, 
                            int x, int y, int rotation, boolean flipped) {
        Game game = findById(gameId);
        
        // Verify the game is in playing state
        if (game.getStatus() != GameStatus.PLAYING) {
            System.out.println("placePiece: Game " + gameId + " is not in PLAYING status.");
            return false;
        }

        // Cancel timer before attempting to place a piece if in timed mode
        if (game.getMode() == GameMode.TIMED) {
            cancelTimer(gameId);
        }
        
        // Delegate actual placement and validation to GameLogicService
        boolean placed = gameLogicService.placePiece(gameId, userId, pieceId, pieceColor, x, y, rotation, flipped);

        if (placed) {
            // Re-fetch game to get the absolute latest state after piece placement and potential turn advancement by GameLogicService
            Game updatedGame = findById(gameId); 
            
            if (updatedGame.getStatus() == GameStatus.PLAYING) {
                 if (updatedGame.getMode() == GameMode.TIMED) {
                    updatedGame.setTurnStartTime(LocalDateTime.now());
                    gameRepository.save(updatedGame); // Save the new turn start time
                    scheduleTurnTimer(updatedGame); // Schedule timer for the next player
                 }
                 // Notify clients about the game state change (e.g., piece placed, next turn)
                 // GameLogicServiceImpl.nextTurn already sends PIECE_PLACED and NEXT_TURN
                 // However, a general state change might be useful for the timer display on client.
                 gameWebSocketService.notifyGameStateChanged(updatedGame.getId());
            } else if (updatedGame.getStatus() == GameStatus.FINISHED) {
                // If game is finished, ensure any active timer is cancelled.
                if (updatedGame.getMode() == GameMode.TIMED) {
                    cancelTimer(updatedGame.getId());
                }
                // GameLogicServiceImpl.nextTurn or isGameOver should handle sending GAME_OVER WebSocket message.
            }
        } else {
            // If placement failed, and it was a timed game, we need to restart the timer for the current player.
            if (game.getMode() == GameMode.TIMED && game.getStatus() == GameStatus.PLAYING) {
                // Ensure the game object 'game' still reflects the state before the failed placement attempt for scheduling.
                // If findById was called inside gameLogicService.placePiece and modified 'game', this might be an issue.
                // Assuming 'game' variable here is still valid for the current player if 'placed' is false.
                scheduleTurnTimer(game); 
            }
        }
        return placed;
    }

    @Override
    @Transactional(readOnly = true) // Good practice for read-only operations
    public List<GameStatisticsDto> findUserCompletedGamesWithDetails(Long userId) {
        // 1. Find all GameUser entries for the user
        List<GameUser> userGameEntries = gameUserRepository.findByUserId(userId);

        // 2. Filter for completed games and map to DTOs
        return userGameEntries.stream()
            .map(GameUser::getGame) // Get the Game object from each GameUser entry
            .filter(game -> game.getStatus() == GameStatus.FINISHED) // Keep only finished games
            .distinct() // Avoid processing the same game multiple times if user has multiple entries (shouldn't happen ideally)
            .map(game -> {
                // 3. For each finished game, fetch details
                List<GameUser> allPlayersInGame = gameUserRepository.findByGameId(game.getId());
                
                // 4. Create PlayerScoreDtos
                List<PlayerScoreDto> playerScores = allPlayersInGame.stream()
                    .map(gu -> {
                        String username = gu.isBot() ? "Bot (" + gu.getColor().name() + ")" : 
                                       (gu.getUser() != null ? gu.getUser().getUsername() : "Inconnu");
                        int score = gu.getScore(); 
                        return new PlayerScoreDto(username, score);
                    })
                    .collect(Collectors.toList());

                // 5. Determine winner based on highest score (Blokus: Max score = least penalty)
                String winnerUsername = "Égalité"; // Default to draw
                GameUser winner = null;
                int bestScore = Integer.MIN_VALUE; // Start with lowest possible value
                boolean isDraw = false;

                if (!allPlayersInGame.isEmpty()) {
                    for (GameUser player : allPlayersInGame) {
                        if (player.getScore() > bestScore) { // Found a better (less negative) score
                            bestScore = player.getScore();
                            winner = player;
                            isDraw = false; // Reset draw flag
                        } else if (player.getScore() == bestScore) {
                             // Found another player with the same best score
                            // If the current winner is null (first player case) or scores match, mark as draw
                            isDraw = true; 
                        }
                    }
                }
                
                if (winner != null && !isDraw) {
                     winnerUsername = winner.isBot() ? "Bot (" + winner.getColor().name() + ")" : 
                                    (winner.getUser() != null ? winner.getUser().getUsername() : "Inconnu");
                } // Otherwise, it remains "Égalité"

                // 6. Create GameStatisticsDto
                return new GameStatisticsDto(
                    game.getId(),
                    game.getName(),
                    game.getEndedAt(), // Use endedAt for completion time
                    winnerUsername,
                    playerScores
                );
            })
            .collect(Collectors.toList());
    }

    private void scheduleTurnTimer(Game game) {
        if (game.getMode() != GameMode.TIMED || game.getStatus() != GameStatus.PLAYING) {
            return;
        }

        cancelTimer(game.getId()); // Cancel any existing timer for this game

        System.out.println("Scheduling turn timer for game " + game.getId() + " for player " + game.getCurrentPlayer().getColor() + " for 60 seconds.");
        ScheduledFuture<?> newTimer = scheduler.schedule(() -> {
            System.out.println("Timer expired for game " + game.getId() + ". Advancing turn due to timeout.");
            advanceTurnDueToTimeout(game.getId());
        }, 60, TimeUnit.SECONDS);
        gameTimers.put(game.getId(), newTimer);
    }

    private void cancelTimer(Long gameId) {
        ScheduledFuture<?> existingTimer = gameTimers.remove(gameId);
        if (existingTimer != null) {
            boolean cancelled = existingTimer.cancel(false);
            System.out.println("Cancelled timer for game " + gameId + ". Success: " + cancelled);
        }
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("Shutting down GameServiceImpl scheduler.");
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Scheduler did not terminate in the specified time.");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        gameTimers.clear();
    }

    @Transactional
    public Game advanceTurnDueToTimeout(Long gameId) {
        Game game = findById(gameId);
        if (game.getStatus() != GameStatus.PLAYING || game.getMode() != GameMode.TIMED) {
            // Only advance if game is playing and in timed mode
            // If not, ensure timer is cancelled just in case
            cancelTimer(gameId);
            return game;
        }

        System.out.println("Advancing turn due to timeout for game: " + gameId);
        // GameUser previousPlayer = game.getCurrentPlayer(); // Keep for potential future use if specific timeout notification is desired
        // String previousPlayerName = previousPlayer != null ? (previousPlayer.isBot() ? "Bot " + previousPlayer.getColor() : previousPlayer.getUser().getUsername()) : "Unknown"; // Unused variable

        // gameLogicService.nextTurn handles changing player, saving game, and WebSocket notifications for next turn or game over.
        GameUser nextPlayer = gameLogicService.nextTurn(gameId);

        // Re-fetch game to get the absolute latest state after nextTurn call
        Game updatedGame = findById(gameId); 

        if (updatedGame.getStatus() == GameStatus.PLAYING && nextPlayer != null) {
            updatedGame.setTurnStartTime(LocalDateTime.now());
            gameRepository.save(updatedGame); // Save updated turn start time
            scheduleTurnTimer(updatedGame); // Schedule timer for the new player's turn
            
            // Send a specific WebSocket message for timeout
            // gameWebSocketService.sendGameUpdate(gameId, "TURN_TIMEOUT", previousPlayerName + "'s turn timed out. Now " + nextPlayer.getUser().getUsername() + "'s turn.", Map.of("timedOutPlayer", previousPlayerName, "nextPlayer", nextPlayer.getUser().getUsername()));
            // The existing nextTurn call in gameLogicService should send a generic NEXT_TURN, 
            // clients can infer timeout if the turn changed without a piece placement.
            // However, a more specific message might be better.
            // For now, relying on notifyGameStateChanged for client to refresh timer.
            gameWebSocketService.notifyGameStateChanged(gameId);
            System.out.println("Turn advanced due to timeout for game " + gameId + ". Next player: " + (nextPlayer.isBot() ? "Bot " + nextPlayer.getColor() : nextPlayer.getUser().getUsername()));
        } else if (updatedGame.getStatus() == GameStatus.FINISHED) {
            System.out.println("Game " + gameId + " finished after timeout or no player could move.");
            cancelTimer(gameId); // Ensure timer is cleaned up if game ended
            // gameLogicService.nextTurn or calculateScores should have sent GAME_OVER
        } else {
            // This case (still PLAYING but nextPlayer is null) should ideally be handled by nextTurn leading to FINISHED.
            // If it occurs, means no one can play, game should be over.
            System.out.println("Game " + gameId + " is still PLAYING but no next player determined after timeout. This may indicate an issue or game end.");
            cancelTimer(gameId);
        }
        return updatedGame;
    }
} 