package com.blokus.blokus.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.model.PieceFactory;
import com.blokus.blokus.repository.GameUserRepository;
import com.blokus.blokus.service.ai.AiBotMoveHandler;
import com.blokus.blokus.service.ai.AiMovePlacementStrategy;
import com.blokus.blokus.service.ai.AiMoveEvaluator;
import com.blokus.blokus.service.ai.AiPieceTransformer;

/**
 * Service for handling AI player moves in the Blokus game.
 * Implements basic AI strategy for bot players.
 */
@Service
public class AiPlayerService {
    
    private final GameLogicService gameLogicService;
    private final GameService gameService;
    @SuppressWarnings("unused")
    private final GameWebSocketService gameWebSocketService;
    private final GameUserRepository gameUserRepository;
    private final AiBotMoveHandler aiBotMoveHandler;
    private final AiMovePlacementStrategy movePlacementStrategy;
    private final AiMoveEvaluator moveEvaluator;
    private final AiPieceTransformer pieceTransformer;
    
    public AiPlayerService(
            GameLogicService gameLogicService,
            GameService gameService,
            GameWebSocketService gameWebSocketService,
            GameUserRepository gameUserRepository) {
        this.gameLogicService = gameLogicService;
        this.gameService = gameService;
        this.gameWebSocketService = gameWebSocketService;
        this.gameUserRepository = gameUserRepository;
        
        // Initialize helper components
        this.pieceTransformer = new AiPieceTransformer();
        this.moveEvaluator = new AiMoveEvaluator();
        this.movePlacementStrategy = new AiMovePlacementStrategy(gameLogicService, pieceTransformer, moveEvaluator);
        this.aiBotMoveHandler = new AiBotMoveHandler(gameLogicService, gameWebSocketService);
    }
    
    /**
     * Determines if the current player is a bot
     */
    public boolean isCurrentPlayerBot(Game game) {
        GameUser currentPlayer = game.getCurrentPlayer();
        return currentPlayer != null && currentPlayer.isBot();
    }
    
    /**
     * Makes a move for the AI player
     */
    public boolean makeAiMove(Long gameId) {
        Game game = gameService.findById(gameId);
        
        if (game == null || !isCurrentPlayerBot(game)) {
            return false;
        }
        
        GameUser aiPlayer = game.getCurrentPlayer();
        String colorName = aiPlayer.getColor().name().toLowerCase();
        
        // Special handlers for red and green bots which have simpler strategies
        if ("green".equalsIgnoreCase(colorName) || "red".equalsIgnoreCase(colorName)) {
            return aiBotMoveHandler.handleBotMove(gameId, aiPlayer, colorName);
        }
        
        // Log the AI move to help with debugging
        System.out.println("AI player " + colorName + " is making a move");
        
        // Ensure AI player has pieces available
        initializePlayerPiecesIfNeeded(aiPlayer, colorName);
        
        if (aiPlayer.getAvailablePieceIds().isEmpty()) {
            System.out.println("AI player " + colorName + " has no more pieces to play.");
            return false;
        }
        
        // Get all pieces for this color
        List<Piece> allPieces = PieceFactory.createPieces(colorName);
        
        // Check if this is the first move for this AI player
        List<Map<String, Object>> placedPieces = getPlacedPiecesByColor(gameId, colorName);
        boolean isFirstMove = placedPieces.isEmpty();
        
        // Get available pieces that haven't been placed yet
        List<Piece> availablePieces = getAvailablePieces(allPieces, aiPlayer);
        
        if (availablePieces.isEmpty()) {
            System.out.println("No available pieces for AI player " + colorName);
            return false;
        }
        
        if (isFirstMove) {
            return movePlacementStrategy.placeFirstPiece(gameId, aiPlayer, colorName, availablePieces);
        } else {
            return movePlacementStrategy.placeSubsequentPiece(gameId, colorName, availablePieces, placedPieces);
        }
    }
    
    /**
     * Initialize player pieces if needed
     */
    private void initializePlayerPiecesIfNeeded(GameUser aiPlayer, String colorName) {
        if (aiPlayer.getAvailablePieceIds() == null) {
            System.out.println("Initializing pieces for " + colorName);
            // Initialize with standard pieces if null
            Set<String> pieceIds = PieceFactory.createPieces(colorName)
                    .stream()
                    .map(Piece::getId)
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
            aiPlayer.setAvailablePieceIds(pieceIds);
            // Save the updated game user to persist the available pieces
            gameUserRepository.save(aiPlayer);
        }
    }
    
    /**
     * Get a list of available pieces for a player
     */
    private List<Piece> getAvailablePieces(List<Piece> allPieces, GameUser player) {
        return allPieces.stream()
            .filter(piece -> player.getAvailablePieceIds().contains(String.valueOf(piece.getId())))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all pieces placed by a specific color
     */
    private List<Map<String, Object>> getPlacedPiecesByColor(Long gameId, String color) {
        // Get all placed pieces for the game
        List<Map<String, Object>> allPlacedPieces = gameLogicService.getPlacedPieces(gameId);
        
        // Filter to only pieces of the specified color
        return allPlacedPieces.stream()
                .filter(piece -> color.equalsIgnoreCase((String) piece.get("pieceColor")))
                .collect(Collectors.toList());
    }
} 