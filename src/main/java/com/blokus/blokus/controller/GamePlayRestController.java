package com.blokus.blokus.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.User;
import com.blokus.blokus.service.AiPlayerService;
import com.blokus.blokus.service.GameLogicService;
import com.blokus.blokus.service.GameService;
import com.blokus.blokus.service.GameWebSocketService;
import com.blokus.blokus.service.UserService;

/**
 * REST Controller for game play actions
 */
@RestController
@RequestMapping("/games/{gameId}")
public class GamePlayRestController {

    private static final Logger logger = LoggerFactory.getLogger(GamePlayRestController.class);
    
    private final GameService gameService;
    private final UserService userService;
    private final GameLogicService gameLogicService;
    private final GameWebSocketService gameWebSocketService;
    private final AiPlayerService aiPlayerService;

    public GamePlayRestController(GameService gameService, UserService userService, 
                             GameLogicService gameLogicService, GameWebSocketService gameWebSocketService,
                             AiPlayerService aiPlayerService) {
        this.gameService = gameService;
        this.userService = userService;
        this.gameLogicService = gameLogicService;
        this.gameWebSocketService = gameWebSocketService;
        this.aiPlayerService = aiPlayerService;
    }

    @PostMapping("/api/place-piece")
    public ResponseEntity<?> placePieceRest(
            @PathVariable Long gameId,
            @RequestParam("x") int x,
            @RequestParam("y") int y,
            @RequestParam(value = "rotation", required = false, defaultValue = "0") Integer rotation,
            @RequestParam(value = "flipped", required = false, defaultValue = "false") Boolean flipped,
            @RequestParam("pieceId") String pieceId,
            @RequestParam("pieceColor") String pieceColor) {
        
        try {
            logger.info("REST API: Received place-piece request:");
            logger.info("  - gameId: {}", gameId);
            logger.info("  - pieceId: {}", pieceId);
            logger.info("  - pieceColor: {}", pieceColor);
            logger.info("  - position: ({}, {})", x, y);
            logger.info("  - rotation: {}", rotation);
            logger.info("  - flipped: {}", flipped);
            
            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                logger.info("REST API: User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                    .body(Map.of("error", "User not authenticated"));
            }
            
            User currentUser = userService.findByUsername(auth.getName());
            logger.info("REST API: Current user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());
            
            Game game = gameService.findById(gameId);
            if (game == null) {
                logger.info("REST API: Game not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(Map.of("error", "Game not found"));
            }
            
            // Default values if not set (though they should be provided by the UI now)
            if (rotation == null) rotation = 0;
            if (flipped == null) flipped = false;
            
            logger.info("REST API: Calling GameLogicService.placePiece");
            
            // Call game logic service to attempt to place the piece
            boolean placementSuccess = gameLogicService.placePiece(gameId, currentUser.getId(), 
                                                          pieceId, pieceColor, 
                                                     x, y, rotation, flipped);
            
            if (!placementSuccess) {
                logger.info("REST API: Placement failed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("error", "Invalid piece placement"));
            }
           
            logger.info("REST API: Piece placed successfully, sending WebSocket update");
            
            // Send real-time update to all clients
            gameWebSocketService.sendPiecePlacedUpdate(
                gameId, 
                pieceId, 
                pieceColor, 
                x, y, rotation, flipped,
                currentUser.getUsername()
            );
           
            logger.info("REST API: Moving to next player's turn");
            
            // Move to next player's turn
            GameUser nextPlayer = gameLogicService.nextTurn(gameId);
           
            // Send next turn update if the game hasn't ended
            if (nextPlayer != null) {
                String nextPlayerName;
                if (nextPlayer.isBot()) {
                    // Bot player - use color-based name
                    String colorName = nextPlayer.getColor().name().toLowerCase();
                    nextPlayerName = "Bot " + colorName.substring(0, 1).toUpperCase() + colorName.substring(1);
                } else if (nextPlayer.getUser() != null) {
                    // Human player - use username
                    nextPlayerName = nextPlayer.getUser().getUsername();
                } else {
                    nextPlayerName = "Unknown Player";
                }
                
                logger.info("REST API: Next player: {}", nextPlayerName);
                gameWebSocketService.sendNextTurnUpdate(gameId, nextPlayer.getColor().name().toLowerCase(), nextPlayerName);
                
                // If next player is a bot, trigger AI move asynchronously
                if (nextPlayer.isBot()) {
                    logger.info("REST API: Next player is a bot, processing AI turn chain");
                    // Process AI moves in a chain (handles multiple consecutive AI players)
                    processAiTurnChain(gameId);
                }
            }
           
            // Check if game is over
            if (gameLogicService.isGameOver(gameId)) {
                logger.info("REST API: Game is over, calculating scores");
                // Calculate final scores
                game = gameLogicService.calculateScores(gameId);
               
                // Find the winner and prepare score data
                Map<String, Integer> scores = new HashMap<>();
                String winnerUsername = "Unknown";
                int highestScore = -1;
               
                for (GameUser player : game.getPlayers()) {
                    String playerName;
                    if (player.isBot()) {
                        // Bot player - use color-based name
                        String colorName = player.getColor().name().toLowerCase();
                        playerName = "Bot " + colorName.substring(0, 1).toUpperCase() + colorName.substring(1);
                    } else if (player.getUser() != null) {
                        // Human player - use username
                        playerName = player.getUser().getUsername();
                    } else {
                        playerName = "Unknown Player";
                    }
                    
                    scores.put(playerName, player.getScore());
                    
                    if (player.getScore() > highestScore) {
                        highestScore = player.getScore();
                        winnerUsername = playerName;
                    }
                }
               
                // Send game over update
                gameWebSocketService.sendGameOverUpdate(gameId, winnerUsername, scores);
            }
            
            logger.info("REST API: Returning success response");
            return ResponseEntity.ok(Map.of("status", "success"));
            
        } catch (Exception e) {
            logger.error("REST API: Exception in place-piece: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }
    
    /**
     * Process a chain of AI turns when multiple AI players need to move in sequence
     * 
     * @param gameId The game ID
     */
    private void processAiTurnChain(Long gameId) {
        processNextAiTurn(gameId);
    }
    
    /**
     * Process the next AI turn in the chain
     * After each bot move, always advance the turn, send NEXT_TURN, and if the next player is a bot, trigger their move.
     * 
     * @param gameId The game ID
     */
    @Async
    public void processNextAiTurn(Long gameId) {
        try {
            Game game = gameService.findById(gameId);
            if (game == null) {
                logger.error("ERROR: Game not found in REST processNextAiTurn");
                return;
            }
            GameUser currentPlayer = game.getCurrentPlayer();
            if (currentPlayer == null) {
                logger.info("Exiting AI chain: No current player");
                return;
            }
            if (!currentPlayer.isBot()) {
                logger.info("Exiting AI chain: Current player is human - {} ({})",
                    (currentPlayer.getUser() != null ? currentPlayer.getUser().getUsername() : "Unknown"),
                    currentPlayer.getColor().name());
                return;
            }
            String botColor = currentPlayer.getColor().name();
            logger.info("REST API: AI chain processing: Bot {}", botColor);
            // Add a small delay before executing the AI move for visual feedback
            try {
                int delay = 2000 + new Random().nextInt(1000);
                if (botColor.equals("GREEN")) {
                    delay += 1000;
                }
                logger.info("REST API: Waiting {}ms before processing {} bot move", delay, botColor);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("REST API ERROR: Thread interrupted while waiting to process {} bot move", botColor);
                return;
            }
            // Execute the AI move
            logger.info("REST API: Executing AI move for {} bot", botColor);
            boolean aiMoved = aiPlayerService.makeAiMove(gameId);
            if (aiMoved) {
                logger.info("REST API SUCCESS: {} bot successfully made a move", botColor);
                // Advance to next player's turn
                GameUser nextPlayer = gameLogicService.nextTurn(gameId);
                if (nextPlayer != null) {
                    String nextPlayerName;
                    if (nextPlayer.isBot()) {
                        String colorName = nextPlayer.getColor().name().toLowerCase();
                        nextPlayerName = "Bot " + colorName.substring(0, 1).toUpperCase() + colorName.substring(1);
                    } else if (nextPlayer.getUser() != null) {
                        nextPlayerName = nextPlayer.getUser().getUsername();
                    } else {
                        nextPlayerName = "Unknown Player";
                    }
                    gameWebSocketService.sendNextTurnUpdate(gameId, nextPlayer.getColor().name().toLowerCase(), nextPlayerName);
                    // If next player is a bot, trigger their move after a short delay
                    if (nextPlayer.isBot()) {
                        try {
                            Thread.sleep(1000);
                            processNextAiTurn(gameId);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.warn("Thread interrupted before processing next bot move", e);
                        }
                    }
                }
            } else {
                logger.info("REST API WARNING: {} bot failed to make a move", botColor);
                // Force move to next player if bot is stuck
                gameLogicService.nextTurn(gameId);
            }
            // Check if game is over
            game = gameService.findById(gameId);
            if (game == null || game.getStatus() != Game.GameStatus.PLAYING) {
                logger.info("REST API: Exiting AI chain: Game is over or null");
            }
        } catch (Exception e) {
            logger.error("CRITICAL ERROR in REST processNextAiTurn: {}", e.getMessage(), e);
        }
    }
} 