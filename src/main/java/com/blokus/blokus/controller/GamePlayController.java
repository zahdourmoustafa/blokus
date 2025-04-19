package com.blokus.blokus.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.model.PieceFactory;
import com.blokus.blokus.model.User;
import com.blokus.blokus.service.AiPlayerService;
import com.blokus.blokus.service.GameLogicService;
import com.blokus.blokus.service.GameService;
import com.blokus.blokus.service.GameWebSocketService;
import com.blokus.blokus.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Controller for game play related actions.
 */
@Controller
@RequestMapping("/games/{gameId}") // Changed mapping to include gameId
@SessionAttributes({"selectedPieceId", "pieceRotation", "pieceFlipped", "selectedPieceColor"})
public class GamePlayController {

    private static final Logger logger = LoggerFactory.getLogger(GamePlayController.class);
    
    private final GameService gameService;
    private final UserService userService;
    private final GameLogicService gameLogicService;
    private final GameWebSocketService gameWebSocketService;
    private final AiPlayerService aiPlayerService;

    // Constructor Injection
    public GamePlayController(GameService gameService, UserService userService, 
                             GameLogicService gameLogicService, GameWebSocketService gameWebSocketService,
                             AiPlayerService aiPlayerService) {
        this.gameService = gameService;
        this.userService = userService;
        this.gameLogicService = gameLogicService;
        this.gameWebSocketService = gameWebSocketService;
        this.aiPlayerService = aiPlayerService;
    }

    @GetMapping("/play")
    public String showGameBoard(@PathVariable Long gameId,
                               Model model,
                               HttpSession session, // Keep session for piece state later
                               RedirectAttributes redirectAttributes) { // Added for error handling
        try {
            Game game = gameService.findById(gameId);

            if (game == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Game not found.");
                return "redirect:/games"; // Redirect to games list if game doesn't exist
            }

            // Add basic game info to the model for now
            model.addAttribute("game", game);
            
            // Get the game participants and add to model
            List<GameUser> participants = gameService.getGameParticipants(gameId);
            model.addAttribute("participants", participants);

            // Add player scores to the model for UI display
            Map<String, Integer> playerScores = new HashMap<>();
            String[] colorOrder = {"blue", "green", "red", "yellow"};
            for (int i = 0; i < participants.size() && i < colorOrder.length; i++) {
                playerScores.put(colorOrder[i], participants.get(i).getScore());
            }
            model.addAttribute("playerScores", playerScores);

            // Get all placed pieces to hide them in the UI
            List<Map<String, Object>> placedPiecesData = gameLogicService.getPlacedPieces(gameId);
            Set<String> placedPieceIds = new HashSet<>();
            
            System.out.println("===== PLACED PIECES DEBUG =====");
            System.out.println("Game ID: " + gameId);
            System.out.println("Total placed pieces: " + placedPiecesData.size());
            
            for (Map<String, Object> piece : placedPiecesData) {
                String pieceId = (String) piece.get("pieceId");
                String pieceColor = (String) piece.get("pieceColor");
                placedPieceIds.add(pieceId);
                System.out.println("Placed piece: ID=" + pieceId + ", Color=" + pieceColor);
            }
            
            System.out.println("placedPieceIds size: " + placedPieceIds.size());
            System.out.println("================================");
            
            model.addAttribute("placedPieces", placedPieceIds);

            // Example: Get current user (needed for player-specific views later)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                 User currentUser = userService.findByUsername(auth.getName());
                 model.addAttribute("currentUser", currentUser);
            } else {
                // Handle case where user is not authenticated, perhaps redirect to login
                 redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to view the game.");
                 return "redirect:/login";
            }

            // Create the pieces for each player
            String[] colors = {"blue", "green", "red", "yellow"};
            Map<String, List<Piece>> playerPieces = new HashMap<>();
            
            for (String color : colors) {
                playerPieces.put(color, PieceFactory.createPieces(color));
            }
            
            model.addAttribute("playerPieces", playerPieces);

            // Get any flash error messages from previous redirects (e.g., invalid move)
            if (model.containsAttribute("errorMessage")) {
                 model.addAttribute("errorMessage", model.getAttribute("errorMessage"));
            }

            return "game/play"; // Return the name of the Thymeleaf template

        } catch (Exception e) {
            // Generic error handling
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading game: " + e.getMessage());
            return "redirect:/games";
        }
    }

    /**
     * Handles piece selection by players
     */
    @PostMapping("/select-piece")
    public String selectPiece(@PathVariable Long gameId,
                             @RequestParam("pieceId") String pieceId,
                             @RequestParam("pieceColor") String pieceColor,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        
        try {
            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to play.");
                return "redirect:/login";
            }
            
            User currentUser = userService.findByUsername(auth.getName());
            Game game = gameService.findById(gameId);
            
            if (game == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Game not found.");
                return "redirect:/games";
            }
            
            // Validate that the game is in PLAYING status
            if (game.getStatus() != Game.GameStatus.PLAYING) {
                redirectAttributes.addFlashAttribute("errorMessage", "Game is not in playing state.");
                return "redirect:/games/" + gameId + "/play";
            }
            
            // Get the game participants
            List<GameUser> participants = gameService.getGameParticipants(gameId);
            
            // Find the participant that matches the current user
            Optional<GameUser> userParticipantOpt = participants.stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(currentUser.getId()))
                .findFirst();
                
            if (userParticipantOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are not a participant in this game.");
                return "redirect:/games/" + gameId + "/play";
            }
            
            GameUser userParticipant = userParticipantOpt.get();
            
            // Determine the color assigned to the current user based on their position
            int userIndex = participants.indexOf(userParticipant);
            String[] playerColors = {"blue", "green", "red", "yellow"};
            String userColor = userIndex < playerColors.length ? playerColors[userIndex] : null;
            
            // Validate that the piece color matches the user's assigned color
            if (userColor == null || !userColor.equals(pieceColor)) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "You can only select pieces of your assigned color: " + userColor);
                return "redirect:/games/" + gameId + "/play";
            }
            
            // Store the selected piece info in the session
            model.addAttribute("selectedPieceId", pieceId);
            model.addAttribute("selectedPieceColor", pieceColor);
            model.addAttribute("pieceRotation", 0); // Initial rotation
            model.addAttribute("pieceFlipped", false); // Initial flip state
            
            // Redirect back to the game board with the piece selected
            return "redirect:/games/" + gameId + "/play";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error selecting piece: " + e.getMessage());
            return "redirect:/games/" + gameId + "/play";
        }
    }

    /**
     * Handles placing the selected piece on the board
     */
    @PostMapping("/place-piece")
    public String placePiece(@PathVariable Long gameId,
                           @RequestParam("x") int x,
                           @RequestParam("y") int y,
                          @RequestParam(value = "rotation", required = false, defaultValue = "0") Integer rotation,
                          @RequestParam(value = "flipped", required = false, defaultValue = "false") Boolean flipped,
                          @RequestParam(value = "pieceId", required = false) String requestPieceId,
                          @RequestParam(value = "pieceColor", required = false) String requestPieceColor,
                           Model model,
                          HttpServletRequest request,
                          HttpServletResponse response,
                           RedirectAttributes redirectAttributes) {
        
        try {
            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
               if (isAjaxRequest(request)) {
                   response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                   return null;
               }
                redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to play.");
                return "redirect:/login";
            }
            
            User currentUser = userService.findByUsername(auth.getName());
            Game game = gameService.findById(gameId);
            
            if (game == null) {
               if (isAjaxRequest(request)) {
                   response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                   return null;
               }
                redirectAttributes.addFlashAttribute("errorMessage", "Game not found.");
                return "redirect:/games";
            }
            
            // Get selected piece details from request parameters first (for AJAX calls)
            String selectedPieceId = requestPieceId;
            String selectedPieceColor = requestPieceColor;
            
            // If not in request params, try from session/model attributes
            if (selectedPieceId == null || selectedPieceColor == null) {
                selectedPieceId = String.valueOf(model.getAttribute("selectedPieceId"));
                selectedPieceColor = String.valueOf(model.getAttribute("selectedPieceColor"));
            }
            
            // If still null, try from a previous session
            if ("null".equals(selectedPieceId) || "null".equals(selectedPieceColor)) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    if (session.getAttribute("selectedPieceId") != null) {
                        selectedPieceId = String.valueOf(session.getAttribute("selectedPieceId"));
                    }
                    if (session.getAttribute("selectedPieceColor") != null) {
                        selectedPieceColor = String.valueOf(session.getAttribute("selectedPieceColor"));
                    }
                }
            }
            
            if ("null".equals(selectedPieceId) || "null".equals(selectedPieceColor)) {
                if (isAjaxRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return null;
                }
                redirectAttributes.addFlashAttribute("errorMessage", "No piece selected.");
                return "redirect:/games/" + gameId + "/play";
            }
            
            // Default values if not set (though they should be provided by the UI now)
            if (rotation == null) rotation = 0;
            if (flipped == null) flipped = false;
            
            // Call game logic service to attempt to place the piece
            boolean placementSuccess = gameLogicService.placePiece(gameId, currentUser.getId(), 
                                                           selectedPieceId, selectedPieceColor, 
                                                     x, y, rotation, flipped);
            
            if (!placementSuccess) {
                if (isAjaxRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return null;
                }
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid piece placement.");
                return "redirect:/games/" + gameId + "/play";
            }
           
            // Send real-time update to all clients
            gameWebSocketService.sendPiecePlacedUpdate(
                gameId, 
                selectedPieceId, 
                selectedPieceColor, 
                x, y, rotation, flipped,
                currentUser.getUsername()
            );
           
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
                
                gameWebSocketService.sendNextTurnUpdate(gameId, nextPlayer.getColor().name().toLowerCase(), nextPlayerName);
                
                // If next player is a bot, trigger AI move asynchronously
                if (nextPlayer.isBot()) {
                    // Process AI moves in a chain (handles multiple consecutive AI players)
                    processAiMoves(gameId);
                }
            }
           
            // Check if game is over
            if (gameLogicService.isGameOver(gameId)) {
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
            
            // Clear the session attributes after successful placement
            model.addAttribute("selectedPieceId", null);
            model.addAttribute("selectedPieceColor", null);
            model.addAttribute("pieceRotation", null);
            model.addAttribute("pieceFlipped", null);
            
            // For AJAX requests, return success status
            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_OK);
                return null;
            }
           
            // Redirect back to the game board for non-AJAX requests
            return "redirect:/games/" + gameId + "/play";
            
        } catch (Exception e) {
            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return null;
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Error placing piece: " + e.getMessage());
            return "redirect:/games/" + gameId + "/play";
        }
    }

    /**
     * Handle AI moves when it's a bot's turn
     */
    @GetMapping("/ai-move")
    public String handleAiMove(@PathVariable Long gameId, RedirectAttributes redirectAttributes) {
        try {
            Game game = gameService.findById(gameId);
            if (game == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Game not found");
                return "redirect:/games";
            }
            
            GameUser currentPlayer = game.getCurrentPlayer();
            if (currentPlayer == null || !currentPlayer.isBot()) {
                redirectAttributes.addFlashAttribute("errorMessage", "It's not an AI player's turn");
                return "redirect:/games/" + gameId + "/play";
            }
            
            boolean aiMoved = aiPlayerService.makeAiMove(gameId);
            if (!aiMoved) {
                redirectAttributes.addFlashAttribute("infoMessage", "AI player couldn't make a move and skipped turn");
            }
            
            return "redirect:/games/" + gameId + "/play";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error processing AI move: " + e.getMessage());
            return "redirect:/games/" + gameId + "/play";
        }
    }

    /**
     * Check if a request is an AJAX request
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    /**
     * Process AI moves for bot players asynchronously
     */
    @Async
    public void processAiMoves(Long gameId) {
        try {
            Game game = gameService.findById(gameId);
            if (game == null || game.getStatus() != Game.GameStatus.PLAYING) {
                logger.info("Cannot process AI moves: game is null or not in PLAYING state");
                return;
            }
            
            GameUser currentPlayer = game.getCurrentPlayer();
            if (currentPlayer == null) {
                logger.info("Cannot process AI moves: current player is null");
                return;
            }
            
            // If current player is not a bot, no AI move needed
            if (!currentPlayer.isBot()) {
                logger.info("Current player is human, no AI move needed");
                return;
            }
            
            String botColor = currentPlayer.getColor().name();
            logger.info("Processing AI move for bot: {}", botColor);
            
            // Add a delay to simulate thinking and allow animations to complete
            Random localRandom = new Random();
            
            try {
                // Longer delay to ensure state is properly updated
                int delay = 2000 + localRandom.nextInt(1000);
                if (botColor.equals("GREEN")) {
                    // Extra delay for GREEN bot which seems to need more time
                    delay += 1000;
                }
                logger.info("Waiting {}ms before processing {} bot move", delay, botColor);
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Thread interrupted while waiting to process {} bot move", botColor);
                return;
            }
            
            // Execute the AI move with special attention to GREEN bot
            logger.info("Executing AI move for {} bot", botColor);
            boolean aiMoved = false;
            int retries = 0;
            final int MAX_RETRIES = 3;
            
            while (!aiMoved && retries < MAX_RETRIES) {
                aiMoved = aiPlayerService.makeAiMove(gameId);
                
                if (!aiMoved) {
                    retries++;
                    logger.info("Retry {}/{} for {} bot", retries, MAX_RETRIES, botColor);
                    // Small wait between retries
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Thread interrupted during retry pause", e);
                        break;
                    }
                }
            }
            
            // Log move result with more details
            if (aiMoved) {
                logger.info("SUCCESS: {} bot successfully made a move", botColor);
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
                            TimeUnit.SECONDS.sleep(1);
                            processAiMoves(gameId);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.warn("Thread interrupted before processing next bot move", e);
                        }
                    }
                }
            } else {
                logger.warn("WARNING: {} bot failed to make a move after {} retries", botColor, MAX_RETRIES);
                
                // Force next turn if bot failed to move after retries
                logger.info("Forcing next turn due to bot failure");
                gameLogicService.nextTurn(gameId);
            }
            
        } catch (IllegalStateException e) {
            logger.error("Illegal game state in processAiMoves: {}", e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Runtime error in processAiMoves: {}", e.getMessage());
        }
    }
}