package com.blokus.blokus.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.blokus.blokus.dto.GameStateDTO;
import com.blokus.blokus.dto.MoveDTO;
import com.blokus.blokus.dto.PlayerPiecesDTO;
import com.blokus.blokus.model.Board;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.repository.GameUserRepository;
import com.blokus.blokus.service.GameLogicService;
import com.blokus.blokus.service.GameService;

import jakarta.persistence.EntityNotFoundException;

@Controller
public class GameWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;
    private final GameLogicService gameLogicService;
    private final GameUserRepository gameUserRepository;

    public GameWebSocketController(SimpMessagingTemplate messagingTemplate, 
                                  GameService gameService,
                                  GameLogicService gameLogicService,
                                  GameUserRepository gameUserRepository) {
        this.messagingTemplate = messagingTemplate;
        this.gameService = gameService;
        this.gameLogicService = gameLogicService;
        this.gameUserRepository = gameUserRepository;
    }

    /**
     * Handle piece placement
     */
    @MessageMapping("/game/{gameId}/move")
    public void handleMove(@DestinationVariable Long gameId, MoveDTO moveDTO) {
        try {
            // Place the piece using existing game logic service
            Board updatedBoard = gameLogicService.placePiece(
                gameId, 
                moveDTO.getPieceId(), 
                moveDTO.getX(), 
                moveDTO.getY(), 
                moveDTO.getRotation(), 
                moveDTO.isFlipped()
            );
            
            // Get current player after the move
            GameUser currentPlayer = gameLogicService.getCurrentPlayer(gameId);
            Game game = gameService.findById(gameId);
            
            // Get all pieces for all players
            List<PlayerPiecesDTO> allPlayerPieces = getAllPlayerPieces(gameId);
            
            // Create a game state DTO to send back to all clients
            GameStateDTO gameState = new GameStateDTO();
            gameState.setBoard(updatedBoard);
            gameState.setCurrentPlayerId(currentPlayer.getUser().getId());
            gameState.setCurrentPlayerColor(currentPlayer.getColor());
            gameState.setAllPlayerPieces(allPlayerPieces);
            gameState.setGameOver(game.getStatus() == Game.GameStatus.FINISHED);
            
            // Send the update to all clients subscribed to this game
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Handle invalid move or argument exceptions
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/error", "Invalid move: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            // Handle entity not found exceptions
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/error", "Entity not found: " + e.getMessage());
        } catch (DataAccessException e) {
            // Handle database access exceptions
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/error", "Database error: " + e.getMessage());
        } catch (Exception e) {
            // Handle any other exceptions
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/error", "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Handle pass turn action
     */
    @MessageMapping("/game/{gameId}/pass")
    public void handlePassTurn(@DestinationVariable Long gameId) {
        try {
            // Pass the turn using game logic service
            GameUser nextPlayer = gameLogicService.nextTurn(gameId);
            
            // Get updated game state
            Game game = gameService.findById(gameId);
            
            // Get all pieces for all players
            List<PlayerPiecesDTO> allPlayerPieces = getAllPlayerPieces(gameId);
            
            // Create a game state DTO to send back to all clients
            GameStateDTO gameState = new GameStateDTO();
            gameState.setBoard(game.getBoard());
            gameState.setCurrentPlayerId(nextPlayer.getUser().getId());
            gameState.setCurrentPlayerColor(nextPlayer.getColor());
            gameState.setAllPlayerPieces(allPlayerPieces);
            gameState.setGameOver(game.getStatus() == Game.GameStatus.FINISHED);
            
            // Send the update to all clients subscribed to this game
            messagingTemplate.convertAndSend("/topic/game/" + gameId, gameState);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Handle game state or argument exceptions
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/error", "Cannot pass turn: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            // Handle entity not found exceptions
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/error", "Entity not found: " + e.getMessage());
        } catch (DataAccessException e) {
            // Handle database access exceptions
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/error", "Database error: " + e.getMessage());
        } catch (Exception e) {
            // Handle any other exceptions
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/error", "Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to get all player pieces
     */
    private List<PlayerPiecesDTO> getAllPlayerPieces(Long gameId) {
        // Get all game participants
        List<GameUser> participants = gameUserRepository.findByGameId(gameId);
        
        // Get pieces for all players
        List<PlayerPiecesDTO> allPlayerPieces = new ArrayList<>();
        for (GameUser participant : participants) {
            PlayerPiecesDTO playerPiecesDTO = new PlayerPiecesDTO();
            
            if (participant.getUser() != null) {
                playerPiecesDTO.setUserId(participant.getUser().getId());
                playerPiecesDTO.setUsername(participant.getUser().getUsername());
                playerPiecesDTO.setColor(participant.getColor());
                
                // Get available pieces for this player
                List<Piece> playerAvailablePieces = gameLogicService.getAvailablePieces(gameId, participant.getUser().getId());
                playerPiecesDTO.setAvailablePieces(playerAvailablePieces);
                
                allPlayerPieces.add(playerPiecesDTO);
            }
        }
        
        return allPlayerPieces;
    }
} 