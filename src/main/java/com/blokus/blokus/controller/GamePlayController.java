package com.blokus.blokus.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.blokus.blokus.model.Board;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.Game.GameStatus;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.model.User;
import com.blokus.blokus.service.GameLogicService;
import com.blokus.blokus.service.GameService;
import com.blokus.blokus.service.UserService;

/**
 * Controller for game play
 */
@Controller
@RequestMapping("/games")
public class GamePlayController {

    private final GameService gameService;
    private final GameLogicService gameLogicService;
    private final UserService userService;

    public GamePlayController(GameService gameService, GameLogicService gameLogicService,
                              UserService userService) {
        this.gameService = gameService;
        this.gameLogicService = gameLogicService;
        this.userService = userService;
    }
    
    /**
     * Show the game play page
     */
    @GetMapping("/{id}/gameplay")
    public String showGamePlay(@PathVariable Long id, Model model,
                             @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {
        try {
            // Get current authenticated user
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Get the game
            Game game = gameService.findById(id);
            
            // Check if game is in playing state
            if (game.getStatus() != GameStatus.PLAYING) {
                if (game.getStatus() == GameStatus.WAITING) {
                    return "redirect:/games/" + id;
                } else {
                    return "redirect:/games?error=Game is finished";
                }
            }
            
            // Check if user is a participant
            List<GameUser> participants = game.getPlayers();
            boolean isParticipant = participants.stream()
                    .anyMatch(p -> p.getUser().getId().equals(user.getId()));
            
            if (!isParticipant) {
                return "redirect:/games?error=You are not a participant in this game";
            }
            
            // Get current player
            GameUser currentPlayer = gameLogicService.getCurrentPlayer(id);
            boolean isPlayerTurn = currentPlayer.getUser().getId().equals(user.getId());
            
            // Get game board
            Board board = game.getBoard();
            
            // Get player color
            GameUser playerGameUser = participants.stream()
                    .filter(p -> p.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow();
            
            // Get available pieces for the player
            List<Piece> availablePieces = gameLogicService.getAvailablePieces(id, user.getId());
            
            // Check if player can make a move
            boolean canMove = gameLogicService.canPlayerMove(id, user.getId());
            
            // Add all data to model
            model.addAttribute("game", game);
            model.addAttribute("board", board);
            model.addAttribute("participants", participants);
            model.addAttribute("currentPlayer", currentPlayer);
            model.addAttribute("isPlayerTurn", isPlayerTurn);
            model.addAttribute("playerColor", playerGameUser.getColor());
            model.addAttribute("availablePieces", availablePieces);
            model.addAttribute("canMove", canMove);
            model.addAttribute("user", user);
            
            // If game was just started, initialize pieces
            if (availablePieces.isEmpty() && game.getStatus() == GameStatus.PLAYING) {
                List<Piece> pieces = gameLogicService.initializePieces(id);
                if (!pieces.isEmpty()) {
                    // Reload the current page without redirect to avoid redirect loop
                    availablePieces = gameLogicService.getAvailablePieces(id, user.getId());
                    model.addAttribute("availablePieces", availablePieces);
                    // Continue to the game/play view instead of redirecting
                }
            }
        
        return "game/play";
        } catch (Exception e) {
            return "redirect:/games?error=" + e.getMessage();
        }
    }
    
    /**
     * Skip the current player's turn
     */
    @PostMapping("/{id}/skip")
    public String skipTurn(@PathVariable Long id,
                          @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            // Get current authenticated user
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Get the game
            Game game = gameService.findById(id);
            
            // Check if game is in playing state
            if (game.getStatus() != GameStatus.PLAYING) {
                redirectAttributes.addFlashAttribute("errorMessage", "Game is not in progress");
                return "redirect:/games/" + id;
            }
            
            // Check if it's the user's turn
            GameUser currentPlayer = gameLogicService.getCurrentPlayer(id);
            if (!currentPlayer.getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "It's not your turn");
                return "redirect:/games/" + id + "/gameplay";
            }
            
            // Check if player can make a move
            boolean canMove = gameLogicService.canPlayerMove(id, user.getId());
            if (canMove) {
                redirectAttributes.addFlashAttribute("errorMessage", "You still have valid moves available");
                return "redirect:/games/" + id + "/gameplay";
        }
        
        // Move to next player
            gameLogicService.nextTurn(id);
        
        // Check if game is over
            if (gameLogicService.isGameOver(id)) {
                game.setStatus(GameStatus.FINISHED);
                gameLogicService.calculateScores(id);
                redirectAttributes.addFlashAttribute("successMessage", "Game is over! Scores have been calculated.");
                return "redirect:/games/" + id;
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Turn skipped");
            return "redirect:/games/" + id + "/gameplay";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/games/" + id + "/gameplay";
        }
    }
    
    /**
     * Initialize pieces for a new game
     */
    @PostMapping("/{id}/initialize")
    public String initializeGame(@PathVariable Long id,
                               @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            // Get the game
            Game game = gameService.findById(id);
            
            // Check if game is in playing state
            if (game.getStatus() != GameStatus.PLAYING) {
                redirectAttributes.addFlashAttribute("errorMessage", "Game is not in progress");
                return "redirect:/games/" + id;
            }
            
            // Initialize pieces
            gameLogicService.initializePieces(id);
            
            redirectAttributes.addFlashAttribute("successMessage", "Game initialized successfully");
            return "redirect:/games/" + id + "/gameplay";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/games/" + id + "/gameplay";
        }
    }
} 