package com.blokus.blokus.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.Game.GameStatus;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.model.PieceFactory;
import com.blokus.blokus.repository.GameRepository;
import com.blokus.blokus.repository.GameUserRepository;
import com.blokus.blokus.service.GameLogicService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implementation of game logic service.
 * Methods related to the removed board/piece implementation have been deleted.
 */
@Service
public class GameLogicServiceImpl implements GameLogicService {

    private static final Logger logger = LoggerFactory.getLogger(GameLogicServiceImpl.class);
    
    private final GameRepository gameRepository;
    private final GameUserRepository gameUserRepository;

    public GameLogicServiceImpl(GameRepository gameRepository, GameUserRepository gameUserRepository) {
        this.gameRepository = gameRepository;
        this.gameUserRepository = gameUserRepository;
    }

    @Override
    public GameUser getCurrentPlayer(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));

        if (game.getStatus() != GameStatus.PLAYING) {
            return null;
        }

        // Use the getCurrentPlayer method we added to the Game entity
        return game.getCurrentPlayer();
    }

    @Override
    @Transactional
    public GameUser nextTurn(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));

        if (game.getStatus() != GameStatus.PLAYING) {
            throw new IllegalStateException("Cannot advance turn, game is not in PLAYING state.");
        }

        // Get all players in the game
        List<GameUser> allPlayers = gameUserRepository.findByGameId(gameId);
        if (allPlayers.isEmpty()) {
            throw new IllegalStateException("Cannot advance turn, no players in the game.");
        }
        
        System.out.println("==============================================================");
        System.out.println("DETAILED TURN TRANSITION DEBUG - GAME ID: " + gameId);
        System.out.println("==============================================================");
        
        // DETAILED DEBUG: Check all players and their assigned colors
        System.out.println("ALL PLAYERS IN THE GAME WITH COLORS:");
        int blueCount = 0, yellowCount = 0, greenCount = 0, redCount = 0;
        
        for (GameUser player : allPlayers) {
            GameUser.PlayerColor color = player.getColor();
            String playerType = player.isBot() ? "Bot" : "Human";
            String playerName = player.isBot() ? 
                              "Bot " + color : 
                              (player.getUser() != null ? player.getUser().getUsername() : "Unknown");
            
            String playerId = player.getUser() != null ? String.valueOf(player.getUser().getId()) : "Bot";
            
            System.out.println(" - " + playerType + " player: " + playerName + " with color " + color + " (ID: " + playerId + ")");
            
            // Count players by color using switch instead of if-else chain
            switch (color) {
                case BLUE -> blueCount++;
                case YELLOW -> yellowCount++;
                case GREEN -> greenCount++;
                case RED -> redCount++;
            }
        }
        
        // Create a map of players by color for quick lookup
        GameUser bluePlayer = findPlayerByColor(allPlayers, GameUser.PlayerColor.BLUE);
        GameUser yellowPlayer = findPlayerByColor(allPlayers, GameUser.PlayerColor.YELLOW);
        GameUser redPlayer = findPlayerByColor(allPlayers, GameUser.PlayerColor.RED);
        GameUser greenPlayer = findPlayerByColor(allPlayers, GameUser.PlayerColor.GREEN);
        
        // Report on player distribution
        System.out.println("COLOR DISTRIBUTION: Blue=" + blueCount + ", Yellow=" + yellowCount + 
                          ", Green=" + greenCount + ", Red=" + redCount);
        System.out.println("BLUE player: " + formatPlayerInfo(bluePlayer));
        System.out.println("YELLOW player: " + formatPlayerInfo(yellowPlayer));
        System.out.println("RED player: " + formatPlayerInfo(redPlayer));
        System.out.println("GREEN player: " + formatPlayerInfo(greenPlayer));
        
        if (yellowPlayer == null) {
            throw new IllegalStateException("ERROR: Yellow player is missing but required in Blokus turn order");
        }
        
        // Get current player
        GameUser currentPlayer = game.getCurrentPlayer();
        
        // Current color (default to null if no current player)
        GameUser.PlayerColor currentColor = (currentPlayer != null) ? currentPlayer.getColor() : null;
        
        System.out.println("CURRENT TURN: " + formatPlayerInfo(currentPlayer) + " with color " + currentColor);
        
        // SIMPLE, DIRECT IMPLEMENTATION - less complex, less prone to errors
        GameUser nextPlayer;
        
        // ENFORCED strict sequence with direct player references
        // BLUE → YELLOW → GREEN → RED → BLUE
        if (currentPlayer == null) {
            System.out.println("No current player, defaulting to BLUE");
            nextPlayer = bluePlayer;
        } 
        else if (currentColor == GameUser.PlayerColor.BLUE) {
            System.out.println("Current player is BLUE, next is YELLOW");
            nextPlayer = yellowPlayer;
        } 
        else if (currentColor == GameUser.PlayerColor.YELLOW) {
            System.out.println("Current player is YELLOW, next is GREEN");
            nextPlayer = greenPlayer;
        } 
        else if (currentColor == GameUser.PlayerColor.GREEN) {
            System.out.println("Current player is GREEN, next is RED");
            nextPlayer = redPlayer;
        } 
        else if (currentColor == GameUser.PlayerColor.RED) {
            System.out.println("Current player is RED, next is BLUE");
            nextPlayer = bluePlayer;
        }
        else {
            System.out.println("Unknown current color, defaulting to BLUE");
            nextPlayer = bluePlayer;
        }
        
        // Sanity check to ensure we got a valid player
        if (nextPlayer == null) {
            throw new IllegalStateException("Next player is null after direct lookup, which should not happen");
        }
        
        System.out.println("NEXT TURN: " + formatPlayerInfo(nextPlayer) + " with color " + nextPlayer.getColor());
        System.out.println("==============================================================");
        
        // Always set the determined player as the current player, no more complex logic
        game.setCurrentPlayer(nextPlayer);
        gameRepository.save(game);
        return nextPlayer;
    }
    
    // Helper method to format player info consistently
    private String formatPlayerInfo(GameUser player) {
        if (player == null) {
            return "MISSING";
        }
        
        if (player.isBot()) {
            return "Bot " + player.getColor();
        }
        
        if (player.getUser() != null) {
            return player.getUser().getUsername() + " (ID: " + player.getUser().getId() + ")";
        }
        
        return "Unknown player";
    }

    @Override
    public boolean isGameOver(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));

        if (game.getStatus() == GameStatus.FINISHED) {
            return true;
        }
        if (game.getStatus() != GameStatus.PLAYING) {
            return false; // Only playing games can become over
        }
        
        // Simplified Game Over Condition: 
        // If no player can make a move (needs canPlayerMove re-implementation based on new rules)
        // For now, let's assume game over if all players have passed consecutively (needs tracking)
        // Or based on a round limit, etc.
        // Placeholder: return false
        return false; 
    }

    @Override
    @Transactional
    public Game calculateScores(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));

        if (game.getStatus() != GameStatus.FINISHED) {
            // Optionally force game to finish first or throw error
            game.setStatus(GameStatus.FINISHED);
            //log.warn("Calculating scores for a game that is not finished. Forcing status to FINISHED.");
        }

        // Scoring logic needs to be re-implemented based on the new game rules (if any)
        // Score might depend on turns taken, objectives met, etc.
        for (GameUser player : game.getPlayers()) {
            int score = 0; 
            // Placeholder: Assign a dummy score or implement new logic
            // score = calculatePlayerScore(gameId, player); // Needs re-implementation
            player.setScore(score); 
            gameUserRepository.save(player);
        }
        
        return gameRepository.save(game);
    }

    @Override
    @Transactional
    public boolean placePiece(Long gameId, Long userId, String pieceId, String pieceColor, 
                            int x, int y, Integer rotation, Boolean flipped) {
        try {
            System.out.println("=============================================================");
            System.out.println("PLACE PIECE DETAILED DEBUG - GAME ID: " + gameId);
            System.out.println("=============================================================");
            System.out.println("Parameters received:");
            System.out.println("  - gameId: " + gameId);
            System.out.println("  - userId: " + userId);
            System.out.println("  - pieceId: " + pieceId);
            System.out.println("  - pieceColor: " + pieceColor);
            System.out.println("  - position: (" + x + "," + y + ")");
            System.out.println("  - rotation: " + rotation);
            System.out.println("  - flipped: " + flipped);
            
            // Store the current piece placement info for validation
            Map<String, Object> currentPiece = new HashMap<>();
            currentPiece.put("pieceId", pieceId);
            currentPiece.put("pieceColor", pieceColor);
            currentPiece.put("x", x);
            currentPiece.put("y", y);
            currentPiece.put("rotation", rotation);
            currentPiece.put("flipped", flipped);
            
            currentPiecePlacementInfo.clear();  // Clear previous info
            currentPiecePlacementInfo.add(currentPiece);
            
            // Use new debug method for more detailed tracing
            debugPiecePlacement(pieceId, pieceColor, x, y, rotation, flipped);
            
            // Validate parameters first - but allow null userId for AI players
            if (gameId == null || pieceId == null || pieceColor == null) {
                System.out.println("ERROR: Missing required parameters gameId, pieceId, or pieceColor");
                return false;
            }
            
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> {
                        System.out.println("ERROR: Game not found with id: " + gameId);
                        return new EntityNotFoundException("Game not found with id: " + gameId);
                    });

            System.out.println("Game found: " + game.getId() + " - Status: " + game.getStatus());
                              
            if (game.getStatus() != GameStatus.PLAYING) {
                System.out.println("Game is not in PLAYING status: " + game.getStatus());
                return false;
            }

            // Get and validate current player
            GameUser currentPlayer = getCurrentPlayer(gameId);
            System.out.println("Current player: " + (currentPlayer != null ? 
                formatPlayerInfo(currentPlayer) : "null"));
                
            if (currentPlayer == null) {
                System.out.println("ERROR: No current player found");
                return false;
            }
            
            // DEBUGGING: Log ALL available pieces for the current player
            System.out.println("========= AVAILABLE PIECES DEBUG =========");
            System.out.println("Player " + formatPlayerInfo(currentPlayer) + " has the following pieces available:");
            if (currentPlayer.getAvailablePieceIds() == null) {
                System.out.println("  - availablePieceIds is NULL!");
            } else if (currentPlayer.getAvailablePieceIds().isEmpty()) {
                System.out.println("  - availablePieceIds is EMPTY!");
            } else {
                currentPlayer.getAvailablePieceIds().forEach(availablePieceId -> 
                    System.out.println("  - Piece ID: " + availablePieceId));
            }
            System.out.println("========= END PIECES DEBUG =========");
            
            // Handle bot players (AI) differently from human players
            boolean isAiPlayer = currentPlayer.isBot();
            
            if (isAiPlayer) {
                System.out.println("AI player detected: " + formatPlayerInfo(currentPlayer));
                // For AI players, we skip the userId validation
                // since AI players don't have an associated User entity
            } else {
                // For human players, we need to validate the userId and user association
                if (currentPlayer.getUser() == null) {
                    System.out.println("ERROR: Human player has no user associated");
                    return false;
                }
                
                if (userId == null) {
                    System.out.println("ERROR: Missing userId for human player");
                    return false;
                }
                
                if (!currentPlayer.getUser().getId().equals(userId)) {
                    System.out.println("ERROR: Turn validation failed. Current player: " + 
                         currentPlayer.getUser().getId() + ", requestUserId: " + userId);
                    return false;
                }
            }
            
            // Color validation - make sure player's color matches piece color
            GameUser.PlayerColor playerColor = currentPlayer.getColor();
            String expectedColorStr = playerColor.toString().toLowerCase();
            System.out.println("Player color: " + playerColor + ", expected color string: " + expectedColorStr + 
                            ", piece color: " + pieceColor);
            
            if (!expectedColorStr.equalsIgnoreCase(pieceColor)) {
                System.out.println("ERROR: Piece color doesn't match player's color");
                return false;
            }
            
            // CRITICAL: Validate that the piece is available for the player
            // If availablePieceIds is null or empty, the player has no pieces left to place
            if (currentPlayer.getAvailablePieceIds() == null || 
                currentPlayer.getAvailablePieceIds().isEmpty() || 
                !currentPlayer.getAvailablePieceIds().contains(pieceId)) {
                System.out.println("ERROR: Piece " + pieceId + " is not available for player " + 
                             formatPlayerInfo(currentPlayer));
                return false;
            }
            
            // Check if this is the player's first piece placement
            // We'll determine this by checking if any pieces with this color have been placed yet
            // In a real implementation, you would check the board state or a player's piece usage history
            
            // For this simplified implementation, we'll check based on the player's starting corner
            // If the player's starting corner is still available, it means they haven't placed a piece yet
            
            // Get all pieces placed so far (in a real implementation, you'd have this data)
            List<Map<String, Object>> placedPieces = getPlacedPieces(gameId);
            
            // Check if the player has already placed any pieces
            boolean hasPlayerPlacedPieces = false;
            for (Map<String, Object> piece : placedPieces) {
                if (pieceColor.equalsIgnoreCase((String)piece.get("pieceColor"))) {
                    hasPlayerPlacedPieces = true;
                    break;
                }
            }
            
            // First piece placement validation
            if (!hasPlayerPlacedPieces) {
                System.out.println("This is the first piece placement for color: " + pieceColor);
                
                // For the first piece, it must be placed in the player's designated corner
                boolean isValidCornerForColor = false;
                
                switch (playerColor) {
                    case BLUE -> isValidCornerForColor = isCoveringOrTouchingCorner(x, y, 0, 0);
                    case YELLOW -> isValidCornerForColor = isCoveringOrTouchingCorner(x, y, 19, 0);
                    case RED -> isValidCornerForColor = isCoveringOrTouchingCorner(x, y, 0, 19);
                    case GREEN -> isValidCornerForColor = isCoveringOrTouchingCorner(x, y, 19, 19);
                }
                
                if (!isValidCornerForColor) {
                    System.out.println("ERROR: First piece for " + playerColor + " must be placed at or touching designated corner");
                    System.out.println("  - BLUE must touch corner (0,0)");
                    System.out.println("  - YELLOW must touch corner (19,0)");
                    System.out.println("  - RED must touch corner (0,19)");
                    System.out.println("  - GREEN must touch corner (19,19)");
                    return false;
                }
                
                System.out.println("First piece placed correctly at or touching designated corner");
            } else {
                // For subsequent pieces, implement standard Blokus rules
                // Pieces must touch at corners but not edges
                System.out.println("This is a subsequent piece placement for color: " + pieceColor);
                
                // Check if the piece has valid diagonal connectivity to an existing piece of the same color
                boolean hasDiagonalTouch = validateDiagonalTouch(gameId, pieceColor, x, y, rotation, flipped, pieceId);
                
                if (!hasDiagonalTouch) {
                    System.out.println("ERROR: Subsequent piece must touch diagonally with at least one piece of the same color");
                    return false;
                }
                
                System.out.println("Diagonal touch validation passed");
            }

            System.out.println("All validations passed! Processing piece placement...");
            
            // Record this piece as placed (in memory map)
            recordPiecePlacement(gameId, pieceId, pieceColor, x, y, rotation, flipped);
            
            // **REMOVE THE PIECE FROM THE PLAYER'S AVAILABLE SET**
            boolean removed = currentPlayer.getAvailablePieceIds().remove(pieceId);
            if (removed) {
                System.out.println("Removed piece " + pieceId + " from player " + 
                                 formatPlayerInfo(currentPlayer) + "'s available pieces.");
                gameUserRepository.save(currentPlayer); // Persist the change
            } else {
                // This should theoretically not happen if the check above passed
                System.out.println("WARNING: Failed to remove piece " + pieceId + 
                                 " from player " + formatPlayerInfo(currentPlayer) + 
                                 "'s available pieces, even though it was present.");
            }
            
            // DEBUG - Get the current player before advancing to next turn
            // currentPlayer = game.getCurrentPlayer(); // Re-fetch might be needed if state changed
            System.out.println("Current player before next turn (after piece removal): " + formatPlayerInfo(currentPlayer));
           
            // Advance to next player's turn - MOVED TO AiPlayerService or a Controller
            // GameUser nextPlayer = nextTurn(gameId);
            
            System.out.println("Piece placement successful!");
            System.out.println("=============================================================");
            
            return true;
        } catch (Exception e) {
            System.out.println("EXCEPTION in placePiece method: " + e.getMessage());
            // Replace printStackTrace with proper logging
            logger.error("Error placing piece: ", e);
            return false;
        }
    }
    
    // Simple in-memory storage for placed pieces (in a real implementation, this would be in the database)
    private static final Map<Long, List<Map<String, Object>>> GAME_PLACED_PIECES = new HashMap<>();
    
    // Helper method to record a piece placement
    private void recordPiecePlacement(Long gameId, String pieceId, String pieceColor, 
                                    int x, int y, Integer rotation, Boolean flipped) {
        // Get or create the list of placed pieces for this game
        List<Map<String, Object>> placedPieces = GAME_PLACED_PIECES.computeIfAbsent(gameId, k -> new ArrayList<>());
        
        // Create a record of the placed piece
        Map<String, Object> placedPiece = new HashMap<>();
        placedPiece.put("pieceId", pieceId);
        placedPiece.put("pieceColor", pieceColor);
        placedPiece.put("x", x);
        placedPiece.put("y", y);
        placedPiece.put("rotation", rotation);
        placedPiece.put("flipped", flipped);
        
        // Add to the list of placed pieces
        placedPieces.add(placedPiece);
        
        System.out.println("Recorded piece placement: " + placedPiece);
    }
    
    // Helper method to get all placed pieces for a game
    @Override
    public List<Map<String, Object>> getPlacedPieces(Long gameId) {
        return GAME_PLACED_PIECES.getOrDefault(gameId, new ArrayList<>());
    }

    /**
     * Helper method to find a player by color
     */
    private GameUser findPlayerByColor(List<GameUser> players, GameUser.PlayerColor color) {
        for (GameUser player : players) {
            if (player.getColor() == color) {
                return player;
            }
        }
        return null;
    }

    /**
     * Helper method to check if a piece is covering or touching the corner position
     */
    private boolean isCoveringOrTouchingCorner(int pieceX, int pieceY, int cornerX, int cornerY) {
        System.out.println("CRITICAL DEBUG: Checking if piece at (" + pieceX + "," + pieceY + ") " +
                          "is covering or touching corner at (" + cornerX + "," + cornerY + ")");
        
        String pieceId = null;
        String pieceColor = null;
        Integer rotation = null;
        Boolean flipped = null;
        
        // Find this piece in the current game state to get its info
        try {
            for (Map<String, Object> placedPiece : currentPiecePlacementInfo) {
                if (((Number) placedPiece.get("x")).intValue() == pieceX &&
                    ((Number) placedPiece.get("y")).intValue() == pieceY) {
                    pieceId = (String) placedPiece.get("pieceId");
                    pieceColor = (String) placedPiece.get("pieceColor");
                    rotation = (Integer) placedPiece.get("rotation");
                    flipped = (Boolean) placedPiece.get("flipped");
                    break;
                }
            }
        } catch (ClassCastException e) {
            logger.error("Type conversion error for piece data: {}", e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Unexpected error getting piece info: {}", e.getMessage());
        }
        
        // If we couldn't find the piece info, try basic check
        if (pieceId == null) {
            // Basic check - just check if the piece covers the corner
            if (pieceX == cornerX && pieceY == cornerY) {
                System.out.println("Piece directly covers corner at (" + cornerX + "," + cornerY + ")");
                return true;
            }
            
            System.out.println("WARNING: Using fallback corner check due to missing piece info");
            
            // Simple fallback check for pieces near the corner
            return Math.abs(pieceX - cornerX) <= 3 && Math.abs(pieceY - cornerY) <= 3;
        }
        
        // Get the piece shape
        boolean[][] shape = null;
        try {
            shape = getPieceShape(pieceId, pieceColor, rotation, flipped);
        } catch (Exception e) {
            System.out.println("Error getting piece shape: " + e.getMessage());
        }
        
        if (shape == null) {
            // If we can't get the shape, revert to basic check
            System.out.println("WARNING: Using fallback corner check due to null shape");
            return Math.abs(pieceX - cornerX) <= 3 && Math.abs(pieceY - cornerY) <= 3;
        }
        
        // Check each cell of the piece
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    int absX = pieceX + c;
                    int absY = pieceY + r;
                    
                    // If any cell covers the corner, return true
                    if (absX == cornerX && absY == cornerY) {
                        System.out.println("Piece cell at (" + absX + "," + absY + ") covers corner");
                        return true;
                    }
                }
            }
        }
        
        // Additional debug info
        System.out.println("Piece does NOT cover corner directly, pieceX=" + pieceX + 
                          ", pieceY=" + pieceY + ", shape width=" + 
                          shape[0].length +
                          ", shape height=" + shape.length);
        
        System.out.println("Piece does NOT cover corner at (" + cornerX + "," + cornerY + ")");
        return false;
    }
    
    // Store current piece placement info temporarily
    private final List<Map<String, Object>> currentPiecePlacementInfo = new ArrayList<>();

    /**
     * Helper method to debug piece placement information
     */
    private void debugPiecePlacement(String pieceId, String pieceColor, int x, int y, Integer rotation, Boolean flipped) {
        System.out.println("PIECE PLACEMENT DEBUG INFO:");
        System.out.println("  - Piece ID: " + pieceId);
        System.out.println("  - Piece Color: " + pieceColor);
        System.out.println("  - Position: (" + x + "," + y + ")");
        System.out.println("  - Rotation: " + rotation);
        System.out.println("  - Flipped: " + flipped);
    }

    /**
     * Validate that a piece placement has proper diagonal touch with pieces of the same color
     */
    private boolean validateDiagonalTouch(Long gameId, String pieceColor, int x, int y, 
                                        Integer rotation, Boolean flipped, String pieceId)
    {
        // Get all placed pieces of the same color
        List<Map<String, Object>> allPlacedPieces = getPlacedPieces(gameId);
        List<Map<String, Object>> sameColorPieces = allPlacedPieces.stream()
                .filter(piece -> pieceColor.equalsIgnoreCase((String) piece.get("pieceColor")))
                .collect(Collectors.toList());
        
        // Get the shape of the piece being placed
        boolean[][] pieceShape = getPieceShape(pieceId, pieceColor, rotation, flipped);
        if (pieceShape == null) {
            System.out.println("ERROR: Could not determine piece shape for ID " + pieceId);
            return false;
        }
        
        // Create a board representation to track cells of same-color pieces
        boolean[][] sameColorBoard = new boolean[20][20];
        
        // Mark all cells occupied by same-color pieces
        for (Map<String, Object> piece : sameColorPieces) {
            String placedPieceId = (String) piece.get("pieceId");
            int placedX = (int) piece.get("x");
            int placedY = (int) piece.get("y");
            Integer placedRotation = (Integer) piece.get("rotation");
            Boolean placedFlipped = (Boolean) piece.get("flipped");
            
            boolean[][] placedShape = getPieceShape(placedPieceId, pieceColor, placedRotation, placedFlipped);
            if (placedShape == null) continue;
            
            for (int py = 0; py < placedShape.length; py++) {
                for (int px = 0; px < placedShape[0].length; px++) {
                    if (placedShape[py][px]) {
                        int boardX = placedX + px;
                        int boardY = placedY + py;
                        if (boardX >= 0 && boardX < 20 && boardY >= 0 && boardY < 20) {
                            sameColorBoard[boardY][boardX] = true;
                        }
                    }
                }
            }
        }
        
        // Check for diagonal touch between new piece and existing same-color pieces
        boolean hasDiagonalTouch = false;
        boolean hasEdgeTouch = false;
        
        for (int py = 0; py < pieceShape.length; py++) {
            for (int px = 0; px < pieceShape[0].length; px++) {
                if (pieceShape[py][px]) {
                    int boardX = x + px;
                    int boardY = y + py;
                    
                    // Check for edge-to-edge touches (invalid)
                    int[][] edges = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // Up, down, left, right
                    for (int[] e : edges) {
                        int ex = boardX + e[0];
                        int ey = boardY + e[1];
                        
                        if (ex >= 0 && ex < 20 && ey >= 0 && ey < 20 && sameColorBoard[ey][ex]) {
                            hasEdgeTouch = true;
                            System.out.println("ERROR: Found edge touch at (" + ex + "," + ey + 
                                              ") with new piece cell at (" + boardX + "," + boardY + ")");
                            break;
                        }
                    }
                    
                    if (hasEdgeTouch) break;
                    
                    // Check all diagonal neighbors
                    int[][] diagonals = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
                    for (int[] d : diagonals) {
                        int dx = boardX + d[0];
                        int dy = boardY + d[1];
                        
                        if (dx >= 0 && dx < 20 && dy >= 0 && dy < 20 && sameColorBoard[dy][dx]) {
                            hasDiagonalTouch = true;
                            System.out.println("Found diagonal touch at (" + dx + "," + dy + 
                                              ") with new piece cell at (" + boardX + "," + boardY + ")");
                            break;
                        }
                    }
                    
                    if (hasDiagonalTouch) break;
                }
            }
            if (hasDiagonalTouch || hasEdgeTouch) break;
        }
        
        // A valid move must have diagonal touch and must NOT have edge touch
        return hasDiagonalTouch && !hasEdgeTouch;
    }

    /**
     * Get the shape of a piece based on its ID, rotation, and flip status
     * This is a simplified implementation - in a real game, you'd have a more comprehensive piece library
     */
    private boolean[][] getPieceShape(String pieceId, String pieceColor, Integer rotation, Boolean flipped) {
        try {
            // Create a temporary piece from the factory to get its shape
            List<Piece> pieces = PieceFactory.createPieces(pieceColor);
            for (Piece piece : pieces) {
                if (pieceId.equals(String.valueOf(piece.getId()))) {
                    boolean[][] originalShape = piece.getShape();
                    
                    // Apply rotation and flip if needed
                    boolean[][] transformedShape = originalShape;
                    
                    // Apply rotation (0, 90, 180, 270 degrees)
                    for (int r = 0; r < (rotation / 90); r++) {
                        transformedShape = rotateShape(transformedShape);
                    }
                    
                    // Apply flip if needed
                    if (flipped) {
                        transformedShape = flipShape(transformedShape);
                    }
                    
                    return transformedShape;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR in getPieceShape: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Rotate a shape 90 degrees clockwise
     */
    private boolean[][] rotateShape(boolean[][] shape) {
        int height = shape.length;
        int width = shape[0].length;
        boolean[][] rotated = new boolean[width][height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rotated[x][height - 1 - y] = shape[y][x];
            }
        }
        
        return rotated;
    }

    /**
     * Flip a shape horizontally
     */
    private boolean[][] flipShape(boolean[][] shape) {
        int height = shape.length;
        int width = shape[0].length;
        boolean[][] flipped = new boolean[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                flipped[y][width - 1 - x] = shape[y][x];
            }
        }
        
        return flipped;
    }
} 