package com.blokus.blokus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.User;
import com.blokus.blokus.service.GameService;
import com.blokus.blokus.service.UserService;

import jakarta.servlet.http.HttpSession;
import java.util.List;
// Import Map, HashMap if needed for pieces/valid positions later
// import java.util.Map;
// import java.util.HashMap;

/**
 * Controller for game play related actions.
 */
@Controller
@RequestMapping("/games/{gameId}") // Changed mapping to include gameId
@SessionAttributes({"selectedPieceId", "pieceRotation", "pieceFlipped"})
public class GamePlayController {

    private final GameService gameService;
    private final UserService userService;

    // Constructor Injection
    public GamePlayController(GameService gameService, UserService userService) {
        this.gameService = gameService;
        this.userService = userService;
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

            // Example: Get current user (needed for player-specific views later)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                 User currentUser = userService.findByUsername(auth.getName());
                 model.addAttribute("currentUser", currentUser);
                 // TODO: Check if currentUser is a participant in the game
            } else {
                // Handle case where user is not authenticated, perhaps redirect to login
                 redirectAttributes.addFlashAttribute("errorMessage", "You must be logged in to view the game.");
                 return "redirect:/login";
            }

            // Placeholder: Add available pieces for the current player (replace with actual logic)
            // List<Piece> availablePieces = gameLogicService.getAvailablePieces(gameId, currentUser.getId());
            // model.addAttribute("availablePieces", availablePieces);

            // Placeholder: Add selected piece info from session (replace with actual logic)
            // Long selectedPieceId = (Long) session.getAttribute("selectedPieceId");
            // if (selectedPieceId != null) { ... handle selected piece ... }

            // Placeholder: Add valid positions (replace with actual logic)
            // model.addAttribute("validPositions", validPositions);

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

    // TODO: Add POST mappings for actions like selectPiece, rotatePiece, flipPiece, placePiece later
    // based on game_board_implementation.md examples

}