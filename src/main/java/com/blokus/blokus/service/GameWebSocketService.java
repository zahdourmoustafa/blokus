package com.blokus.blokus.service;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.blokus.blokus.dto.GameUpdateDTO;

/**
 * Service for handling WebSocket messaging for game updates
 */
@Service
public class GameWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public GameWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Send a game update to all clients subscribed to the game channel
     * 
     * @param gameId The ID of the game
     * @param type The type of the update (e.g., "PIECE_PLACED", "NEXT_TURN")
     * @param message A human-readable message
     * @param data Additional data to send with the update
     */
    public void sendGameUpdate(Long gameId, String type, String message, Map<String, Object> data) {
        GameUpdateDTO update = new GameUpdateDTO(gameId, type, message, data);
        messagingTemplate.convertAndSend("/topic/games/" + gameId, update);
    }
    
    /**
     * Send a piece placed update
     * 
     * @param gameId The ID of the game
     * @param pieceId The ID of the placed piece
     * @param pieceColor The color of the placed piece
     * @param x The x-coordinate where the piece was placed
     * @param y The y-coordinate where the piece was placed
     * @param rotation The rotation of the piece
     * @param flipped Whether the piece is flipped
     * @param playerUsername The username of the player who placed the piece
     */
    public void sendPiecePlacedUpdate(Long gameId, String pieceId, String pieceColor, 
                                      int x, int y, int rotation, boolean flipped,
                                      String playerUsername) {
        Map<String, Object> data = Map.of(
            "pieceId", pieceId,
            "pieceColor", pieceColor,
            "x", x,
            "y", y,
            "rotation", rotation,
            "flipped", flipped,
            "playerUsername", playerUsername
        );
        
        sendGameUpdate(gameId, "PIECE_PLACED", 
                      playerUsername + " placed a piece on the board", data);
    }
    
    /**
     * Send a next turn update
     * 
     * @param gameId The ID of the game
     * @param nextPlayerUsername The username of the next player
     */
    public void sendNextTurnUpdate(Long gameId, String nextPlayerUsername) {
        Map<String, Object> data = Map.of(
            "nextPlayerUsername", nextPlayerUsername
        );
        
        sendGameUpdate(gameId, "NEXT_TURN", 
                      "It's " + nextPlayerUsername + "'s turn", data);
    }
    
    /**
     * Send a game over update
     * 
     * @param gameId The ID of the game
     * @param winnerUsername The username of the winner
     * @param scores A map of player usernames to their scores
     */
    public void sendGameOverUpdate(Long gameId, String winnerUsername, Map<String, Integer> scores) {
        Map<String, Object> data = Map.of(
            "winnerUsername", winnerUsername,
            "scores", scores
        );
        
        sendGameUpdate(gameId, "GAME_OVER", 
                      winnerUsername + " won the game!", data);
    }
} 