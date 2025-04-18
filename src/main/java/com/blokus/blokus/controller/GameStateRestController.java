package com.blokus.blokus.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.service.GameService;
import com.blokus.blokus.service.GameLogicService;

/**
 * REST controller for game state.
 * Endpoints related to the removed board/piece implementation have been deleted.
 */
@RestController
@RequestMapping("/games/{gameId}")
public class GameStateRestController {

    private final GameService gameService;
    private final GameLogicService gameLogicService;

    public GameStateRestController(GameService gameService, GameLogicService gameLogicService) {
        this.gameService = gameService;
        this.gameLogicService = gameLogicService;
    }

    @GetMapping("/api/state")
    public ResponseEntity<?> getGameState(@PathVariable Long gameId, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            Game game = gameService.findById(gameId);

            if (game == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Game not found"));
            }

            // Create a custom game state response
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("gameId", game.getId());
            gameState.put("status", game.getStatus().toString());

            // Get current player info
            GameUser currentPlayer = game.getCurrentPlayer();
            if (currentPlayer != null) {
                Map<String, Object> playerInfo = new HashMap<>();
                playerInfo.put("username", currentPlayer.isBot() ? 
                    "Bot " + currentPlayer.getColor().name() : 
                    (currentPlayer.getUser() != null ? currentPlayer.getUser().getUsername() : "Unknown"));
                playerInfo.put("color", currentPlayer.getColor().name().toLowerCase());
                gameState.put("currentPlayer", playerInfo);
            }

            // Add player information
            List<Map<String, Object>> playerData = game.getPlayers().stream()
                .map(player -> {
                    Map<String, Object> pData = new HashMap<>();
                    pData.put("username", player.isBot() ? 
                        "Bot " + player.getColor().name() : 
                        (player.getUser() != null ? player.getUser().getUsername() : "Unknown"));
                    pData.put("color", player.getColor().name().toLowerCase());
                    pData.put("score", player.getScore());
                    // Add additional player data here
                    return pData;
                })
                .collect(Collectors.toList());
            gameState.put("players", playerData);

            // Get all pieces placed so far for each player
            for (GameUser player : game.getPlayers()) {
                String colorName = player.getColor().name().toLowerCase();
                List<Map<String, Object>> placedPieces = gameLogicService.getPlacedPieces(gameId).stream()
                    .filter(piece -> colorName.equalsIgnoreCase((String) piece.get("pieceColor")))
                    .collect(Collectors.toList());
                
                // Extract just the piece IDs for simpler handling in frontend
                List<String> placedPieceIds = placedPieces.stream()
                    .map(piece -> (String) piece.get("pieceId"))
                    .collect(Collectors.toList());
                
                // Add to player data
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> players = (List<Map<String, Object>>) gameState.get("players");
                for (Map<String, Object> playerEntry : players) {
                    String playerColorName = (String) playerEntry.get("color");
                    if (colorName.equalsIgnoreCase(playerColorName)) {
                        playerEntry.put("usedPieces", placedPieceIds);
                        break;
                    }
                }
            }

            // Note: In a real implementation, you would add board state here
            // For now we're returning placeholder data
            gameState.put("board", new Object[0]); // Empty board for now

            // Add available pieces information - placeholder
            Map<String, Object> availablePieces = new HashMap<>();
            gameState.put("availablePieces", availablePieces);

            return ResponseEntity.ok(gameState);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error retrieving game state: " + e.getMessage()));
        }
    }
} 