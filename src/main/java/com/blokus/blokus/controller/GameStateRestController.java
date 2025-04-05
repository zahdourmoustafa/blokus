package com.blokus.blokus.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blokus.blokus.dto.MoveDTO;
import com.blokus.blokus.dto.PlayerPiecesDTO;
import com.blokus.blokus.model.Board;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.model.User;
import com.blokus.blokus.repository.GameUserRepository;
import com.blokus.blokus.repository.PieceRepository;
import com.blokus.blokus.service.BotService;
import com.blokus.blokus.service.GameLogicService;
import com.blokus.blokus.service.GameService;
import com.blokus.blokus.service.UserService;

import jakarta.validation.Valid;

/**
 * REST controller for game state and moves
 */
@RestController
@RequestMapping("/api/games")
public class GameStateRestController {

    private final GameService gameService;
    private final GameLogicService gameLogicService;
    private final UserService userService;
    private final BotService botService;
    private final GameUserRepository gameUserRepository;
    private final PieceRepository pieceRepository;

    public GameStateRestController(GameService gameService, GameLogicService gameLogicService,
                                  UserService userService, BotService botService,
                                  GameUserRepository gameUserRepository, PieceRepository pieceRepository) {
        this.gameService = gameService;
        this.gameLogicService = gameLogicService;
        this.userService = userService;
        this.botService = botService;
        this.gameUserRepository = gameUserRepository;
        this.pieceRepository = pieceRepository;
    }

    /**
     * Get the current game state
     */
    @GetMapping("/{gameId}/state")
    public ResponseEntity<?> getGameState(@PathVariable Long gameId,
                                         @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        try {
            User user = userService.findByUsername(principal.getUsername());
            Game game = gameService.findById(gameId);
            
            // Process bot turns if necessary
            botService.handleBotTurn(gameId);
                
            Board board = game.getBoard();
            GameUser currentPlayer = gameLogicService.getCurrentPlayer(gameId);
            List<Piece> availablePieces = gameLogicService.getAvailablePieces(gameId, user.getId());
            boolean canMove = gameLogicService.canPlayerMove(gameId, user.getId());
            boolean isPlayerTurn = currentPlayer.getUser().getId().equals(user.getId());
            
            // Get all game participants
            List<GameUser> participants = gameUserRepository.findByGameId(gameId);
            
            // Get pieces for all players
            List<PlayerPiecesDTO> allPlayerPieces = new ArrayList<>();
            for (GameUser participant : participants) {
                PlayerPiecesDTO playerPiecesDTO = new PlayerPiecesDTO();
                
                User playerUser = participant.getUser();
                playerPiecesDTO.setUserId(playerUser.getId());
                playerPiecesDTO.setUsername(playerUser.getUsername());
                playerPiecesDTO.setColor(participant.getColor());
                
                // Get available pieces for this player
                List<Piece> playerAvailablePieces = gameLogicService.getAvailablePieces(gameId, playerUser.getId());
                playerPiecesDTO.setAvailablePieces(playerAvailablePieces);
                
                allPlayerPieces.add(playerPiecesDTO);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("gameStatus", game.getStatus().name());
            response.put("boardState", board.getGrid());
            response.put("currentPlayer", currentPlayer.getUser().getUsername());
            response.put("currentPlayerColor", currentPlayer.getColor().name());
            response.put("isPlayerTurn", isPlayerTurn);
            response.put("canMove", canMove);
            response.put("availablePieces", availablePieces.stream().map(Piece::getId).collect(Collectors.toList()));
            response.put("allPlayerPieces", allPlayerPieces);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get available pieces for the current user
     */
    @GetMapping("/{gameId}/pieces")
    public ResponseEntity<?> getAvailablePieces(@PathVariable Long gameId,
                                              @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        try {
            User user = userService.findByUsername(principal.getUsername());
            List<Piece> pieces = gameLogicService.getAvailablePieces(gameId, user.getId());
            
            List<Map<String, Object>> pieceData = pieces.stream().map(p -> {
                Map<String, Object> data = new HashMap<>();
                data.put("id", p.getId());
                data.put("type", p.getType().name());
                data.put("shape", p.getShape());
                return data;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(pieceData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Make a move
     */
    @PostMapping("/{gameId}/move")
    public ResponseEntity<?> makeMove(@PathVariable Long gameId, @Valid @RequestBody MoveDTO moveDTO,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        try {
            User user = userService.findByUsername(principal.getUsername());
            
            // Validate it's the player's turn
            GameUser currentPlayer = gameLogicService.getCurrentPlayer(gameId);
            if (!currentPlayer.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "It's not your turn"));
            }
            
            // Try to place the piece
            Board updatedBoard = gameLogicService.placePiece(
                    gameId, 
                    moveDTO.getPieceId(), 
                    moveDTO.getX(), 
                    moveDTO.getY(), 
                    moveDTO.getRotation(),
                    moveDTO.isFlipped()
            );
            
            // Process bot turns after the player move
            Game game = gameService.findById(gameId);
            int botMoves = botService.processBotRound(game);
            
            // Get the updated game state after bot moves
            GameUser newCurrentPlayer = gameLogicService.getCurrentPlayer(gameId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("boardState", updatedBoard.getGrid());
            response.put("gameStatus", game.getStatus().name());
            response.put("currentPlayer", newCurrentPlayer.getUser().getUsername());
            response.put("currentPlayerColor", newCurrentPlayer.getColor().name());
            response.put("botMovesPerformed", botMoves);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Skip a turn
     */
    @PostMapping("/{gameId}/skip")
    public ResponseEntity<?> skipTurn(@PathVariable Long gameId,
                                     @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        try {
            User user = userService.findByUsername(principal.getUsername());
            
            // Validate it's the player's turn
            GameUser currentPlayer = gameLogicService.getCurrentPlayer(gameId);
            if (!currentPlayer.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "It's not your turn"));
            }
            
            // Check if player can make a move
            boolean canMove = gameLogicService.canPlayerMove(gameId, user.getId());
            if (canMove) {
                return ResponseEntity.badRequest().body(Map.of("error", "You still have valid moves available"));
            }
            
            // Skip to next player
            gameLogicService.nextTurn(gameId);
            
            // Process bot turns after the player skips
            Game game = gameService.findById(gameId);
            int botMoves = botService.processBotRound(game);
            
            // Get the updated game state
            GameUser newCurrentPlayer = gameLogicService.getCurrentPlayer(gameId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("gameStatus", game.getStatus().name());
            response.put("currentPlayer", newCurrentPlayer.getUser().getUsername());
            response.put("currentPlayerColor", newCurrentPlayer.getColor().name());
            response.put("botMovesPerformed", botMoves);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Initialize game pieces
     */
    @PostMapping("/{gameId}/initialize")
    public ResponseEntity<?> initializeGame(@PathVariable Long gameId,
                                          @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        try {
            // Only game creator can initialize the game
            User user = userService.findByUsername(principal.getUsername());
            Game game = gameService.findById(gameId);
            GameUser creator = game.getPlayers().get(0);
            
            if (!creator.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only the game creator can initialize the game"));
            }
            
            // Check if pieces already exist
            List<Piece> existingPieces = pieceRepository.findByBoardId(game.getBoard().getId());
            if (!existingPieces.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "piecesCount", existingPieces.size(),
                    "message", "Pieces already initialized"
                ));
            }
            
            // Initialize pieces
            List<Piece> pieces = gameLogicService.initializePieces(gameId);
            
            // Process bot turns if it's a bot's turn
            int botMoves = botService.processBotRound(game);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "piecesCount", pieces.size(),
                "botMovesPerformed", botMoves
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Check if a move is valid
     */
    @PostMapping("/{gameId}/checkMove")
    public ResponseEntity<?> checkMove(@PathVariable Long gameId, @Valid @RequestBody MoveDTO moveDTO,
                                      @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        try {
            User user = userService.findByUsername(principal.getUsername());
            
            // Validate it's the player's turn
            GameUser currentPlayer = gameLogicService.getCurrentPlayer(gameId);
            if (!currentPlayer.getUser().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "It's not your turn"));
            }
            
            // Check if the move is valid
            boolean isValid = gameLogicService.isValidMove(
                    gameId, 
                    moveDTO.getPieceId(), 
                    moveDTO.getX(), 
                    moveDTO.getY(), 
                    moveDTO.getRotation(),
                    moveDTO.isFlipped()
            );
            
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 