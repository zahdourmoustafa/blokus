package com.blokus.blokus.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.model.PieceFactory;
import com.blokus.blokus.service.GameLogicService;
import com.blokus.blokus.service.GameWebSocketService;

/**
 * Handles bot moves for the Blokus game.
 * This class implements simpler strategies for red and green bots.
 */
public class AiBotMoveHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AiBotMoveHandler.class);
    
    private final GameLogicService gameLogicService;
    private final GameWebSocketService gameWebSocketService;
    private final Random random = new Random();
    
    // Board size constants
    private static final int BOARD_SIZE = 20;
    
    // Starting corner positions for each color (standard Blokus rules)
    private static final int[][] STARTING_CORNERS = {
        {0, 0},      // BLUE: top-left
        {19, 0},     // YELLOW: top-right
        {0, 19},     // RED: bottom-left
        {19, 19}     // GREEN: bottom-right
    };
    
    public AiBotMoveHandler(GameLogicService gameLogicService, 
                          GameWebSocketService gameWebSocketService) {
        this.gameLogicService = gameLogicService;
        this.gameWebSocketService = gameWebSocketService;
    }
    
    /**
     * Handles a move for a bot player with simplified strategy.
     * Used primarily for red and green bots.
     * 
     * @param gameId Game ID
     * @param botPlayer The bot player making the move
     * @param colorName The color of the bot player
     * @return true if a move was successfully made, false otherwise
     */
    public boolean handleBotMove(Long gameId, GameUser botPlayer, String colorName) {
        System.out.println("Bot " + colorName + " is making a simplified move");
        
        // Ensure player has available pieces
        if (botPlayer.getAvailablePieceIds() == null || botPlayer.getAvailablePieceIds().isEmpty()) {
            System.out.println("Bot " + colorName + " has no more pieces available.");
            return false;
        }
        
        // Get placed pieces to determine if this is the first move
        List<Map<String, Object>> placedPieces = gameLogicService.getPlacedPieces(gameId);
        List<Map<String, Object>> colorPieces = filterPiecesByColor(placedPieces, colorName);
        boolean isFirstMove = colorPieces.isEmpty();
        
        if (isFirstMove) {
            return makeFirstMove(gameId, botPlayer, colorName);
        } else {
            return makeSubsequentMove(gameId, botPlayer, colorName, placedPieces);
        }
    }
    
    /**
     * Makes the first move for a bot player.
     * Places a medium-sized piece in the starting corner.
     */
    private boolean makeFirstMove(Long gameId, GameUser botPlayer, String colorName) {
        System.out.println("Bot " + colorName + " making first move");
        
        // Get starting corner
        int[] corner = getStartingCornerForColor(colorName);
        if (corner == null) {
            System.out.println("Error: Invalid color name: " + colorName);
            return false;
        }
        
        int cornerX = corner[0];
        int cornerY = corner[1];
        
        System.out.println("Bot " + colorName + " starting corner: (" + cornerX + "," + cornerY + ")");
        
        // Get all pieces
        List<Piece> pieces = PieceFactory.createPieces(colorName);
        
        // Filter to only available pieces
        List<Piece> availablePieces = filterAvailablePieces(pieces, botPlayer.getAvailablePieceIds());
        
        if (availablePieces.isEmpty()) {
            System.out.println("Bot " + colorName + " has no pieces available for first move");
            return false;
        }
        
        System.out.println("Bot " + colorName + " has " + availablePieces.size() + " available pieces");
        
        // For first move, prefer simpler pieces for first move (I-tetromino or small L-shape)
        List<Piece> simplePieces = new ArrayList<>();
        for (Piece piece : availablePieces) {
            // Use pieces with IDs 0-2 which are simpler shapes
            if (piece.getId() <= 2) {
                simplePieces.add(piece);
            }
        }
        
        // If no simple pieces available, try medium-sized pieces
        List<Piece> mediumPieces = new ArrayList<>();
        if (simplePieces.isEmpty()) {
            for (Piece piece : availablePieces) {
                int pieceSize = countCells(piece.getShape());
                if (pieceSize >= 3 && pieceSize <= 4) {
                    mediumPieces.add(piece);
                }
            }
        }
        
        // Use pieces in order of preference: simple, medium, any available
        List<Piece> piecesToTry = !simplePieces.isEmpty() ? simplePieces : 
                                 (!mediumPieces.isEmpty() ? mediumPieces : availablePieces);
        
        System.out.println("Bot " + colorName + " trying " + piecesToTry.size() + " pieces for placement");
        
        // Try each piece
        for (Piece piece : piecesToTry) {
            System.out.println("Bot " + colorName + " trying piece ID: " + piece.getId());
            
            // Try with different rotations
            for (int rotation = 0; rotation < 4; rotation++) {
                for (boolean flipped : new boolean[]{false, true}) {
                    // For first move, place at the corner
                    // Adjust position based on piece shape and corner position
                    int[] adjustedPosition = calculateCornerPosition(piece, rotation, flipped, cornerX, cornerY);
                    
                    if (adjustedPosition != null) {
                        int x = adjustedPosition[0];
                        int y = adjustedPosition[1];
                        
                        System.out.println("Bot " + colorName + " attempting to place piece " + piece.getId() + 
                                          " at position (" + x + "," + y + ") rotation=" + rotation + 
                                          " flipped=" + flipped);
                        
                        try {
                            boolean placed = gameLogicService.placePiece(
                                    gameId,
                                    botPlayer.getUser() != null ? botPlayer.getUser().getId() : null,
                                    String.valueOf(piece.getId()),
                                    colorName,
                                    x, y,
                                    rotation,
                                    flipped);
                            
                            if (placed) {
                                System.out.println("Bot " + colorName + " successfully placed first piece: " + piece.getId() + 
                                                 " at (" + x + "," + y + ")");
                                
                                // Remove piece from available pieces
                                botPlayer.getAvailablePieceIds().remove(String.valueOf(piece.getId()));
                                
                                // Notify clients via WebSocket with PIECE_PLACED event
                                gameWebSocketService.sendPiecePlacedUpdate(
                                    gameId,
                                    String.valueOf(piece.getId()),
                                    colorName,
                                    x, y, rotation, flipped,
                                    "Bot " + Character.toUpperCase(colorName.charAt(0)) + colorName.substring(1)
                                );
                                
                                return true;
                            } else {
                                System.out.println("Bot " + colorName + " failed to place piece " + piece.getId() + 
                                                 " at (" + x + "," + y + ")");
                            }
                        } catch (Exception e) {
                            System.out.println("Exception during bot " + colorName + " piece placement: " + e.getMessage());
                            logger.error("Exception during bot {} piece placement", colorName, e);
                        }
                    } else {
                        System.out.println("No valid position found for piece " + piece.getId() + 
                                         " with rotation=" + rotation + " flipped=" + flipped);
                    }
                }
            }
        }
        
        System.out.println("Bot " + colorName + " failed to place first piece after trying all options");
        
        // Last resort: try all pieces with all possible positions
        for (Piece piece : availablePieces) {
            // Try one-cell offset from corner if direct corner placement doesn't work
            for (int offsetX = 0; offsetX <= 1; offsetX++) {
                for (int offsetY = 0; offsetY <= 1; offsetY++) {
                    int targetX = cornerX - (colorName.equals("green") || colorName.equals("yellow") ? offsetX : -offsetX);
                    int targetY = cornerY - (colorName.equals("green") || colorName.equals("red") ? offsetY : -offsetY);
                    
                    for (int rotation = 0; rotation < 4; rotation++) {
                        for (boolean flipped : new boolean[]{false, true}) {
                            try {
                                boolean placed = gameLogicService.placePiece(
                                        gameId,
                                        botPlayer.getUser() != null ? botPlayer.getUser().getId() : null,
                                        String.valueOf(piece.getId()),
                                        colorName,
                                        targetX, targetY,
                                        rotation,
                                        flipped);
                                
                                if (placed) {
                                    System.out.println("Bot " + colorName + " placed piece with last resort method: " + 
                                                     piece.getId() + " at (" + targetX + "," + targetY + ")");
                                    
                                    botPlayer.getAvailablePieceIds().remove(String.valueOf(piece.getId()));
                                    gameWebSocketService.sendPiecePlacedUpdate(
                                        gameId,
                                        String.valueOf(piece.getId()),
                                        colorName,
                                        targetX, targetY, rotation, flipped,
                                        "Bot " + Character.toUpperCase(colorName.charAt(0)) + colorName.substring(1)
                                    );
                                    return true;
                                }
                            } catch (Exception ignored) {
                                // Just try next combination
                            }
                        }
                    }
                }
            }
        }
        
        System.out.println("Bot " + colorName + " completely failed to place any piece");
        return false;
    }
    
    /**
     * Makes a subsequent move for a bot player.
     * Uses a simple strategy of finding valid placements near existing pieces.
     */
    private boolean makeSubsequentMove(Long gameId, GameUser botPlayer, String colorName, 
                                     List<Map<String, Object>> placedPieces) {
        System.out.println("Bot " + colorName + " making subsequent move");
        List<Map<String, Object>> colorPieces = filterPiecesByColor(placedPieces, colorName);
        if (colorPieces.isEmpty()) {
            System.out.println("Error: No pieces found for color " + colorName);
            return false;
        }
        List<Piece> pieces = PieceFactory.createPieces(colorName);
        List<Piece> availablePieces = filterAvailablePieces(pieces, botPlayer.getAvailablePieceIds());
        if (availablePieces.isEmpty()) {
            System.out.println("Bot " + colorName + " has no pieces available for subsequent move");
            return false;
        }
        shuffleList(availablePieces);
        for (Piece piece : availablePieces) {
            for (int rotation = 0; rotation < 4; rotation++) {
                for (boolean flipped : new boolean[]{false, true}) {
                    boolean[][] shape = getTransformedShape(piece.getShape(), rotation, flipped);
                    int shapeHeight = shape.length;
                    int shapeWidth = shape[0].length;
                    for (int x = 0; x <= BOARD_SIZE - shapeWidth; x++) {
                        for (int y = 0; y <= BOARD_SIZE - shapeHeight; y++) {
                            if (isValidBlokusPlacement(shape, x, y, colorName, placedPieces)) {
                                boolean placed = gameLogicService.placePiece(
                                        gameId,
                                        botPlayer.getUser() != null ? botPlayer.getUser().getId() : null,
                                        String.valueOf(piece.getId()),
                                        colorName,
                                        x, y,
                                        rotation,
                                        flipped);
                                if (placed) {
                                    System.out.println("Bot " + colorName + " placed subsequent piece: " + 
                                                      piece.getId() + " at (" + x + "," + y + ")");
                                    botPlayer.getAvailablePieceIds().remove(String.valueOf(piece.getId()));
                                    gameWebSocketService.sendPiecePlacedUpdate(
                                        gameId,
                                        String.valueOf(piece.getId()),
                                        colorName,
                                        x, y, rotation, flipped,
                                        "Bot " + Character.toUpperCase(colorName.charAt(0)) + colorName.substring(1)
                                    );
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Bot " + colorName + " failed to place any subsequent piece");
        return false;
    }
    
    /**
     * Calculates the position to place a piece so it covers the starting corner
     */
    private int[] calculateCornerPosition(Piece piece, int rotation, boolean flipped, 
                                        int cornerX, int cornerY) {
        boolean[][] shape = getTransformedShape(piece.getShape(), rotation, flipped);
        
        // Debug output
        System.out.println("DEBUG - Bot trying to place at corner: (" + cornerX + "," + cornerY + ")");
        
        // Find the first active cell to place at the corner
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    // Calculate position so this cell is at the corner
                    int x = cornerX - c;
                    int y = cornerY - r;
                    
                    // Check if the entire piece is on the board
                    if (isPieceOnBoard(shape, x, y)) {
                        System.out.println("DEBUG - Found valid position: (" + x + "," + y + ")");
                        return new int[]{x, y};
                    }
                }
            }
        }
        
        System.out.println("DEBUG - Failed to find valid position for corner: (" + cornerX + "," + cornerY + ")");
        
        // If no position found with the first active cell at the corner,
        // try to find any valid position where at least one cell is at the corner
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    int x = cornerX - c;
                    int y = cornerY - r;
                    
                    // Check if piece is on board and covers the corner
                    if (isPieceOnBoard(shape, x, y) && 
                        doesPieceCoverPosition(shape, x, y, cornerX, cornerY)) {
                        System.out.println("DEBUG - Found alternate valid position: (" + x + "," + y + ")");
                        return new int[]{x, y};
                    }
                }
            }
        }
        
        return null; // No valid position found
    }
    
    /**
     * Checks if a piece at position (x,y) covers a specific position (targetX, targetY)
     */
    private boolean doesPieceCoverPosition(boolean[][] shape, int pieceX, int pieceY, int targetX, int targetY) {
        // Check if any active cell in the piece covers the target position
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    int cellX = pieceX + c;
                    int cellY = pieceY + r;
                    if (cellX == targetX && cellY == targetY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Gets a transformed (rotated and/or flipped) shape
     */
    private boolean[][] getTransformedShape(boolean[][] originalShape, int rotation, boolean flipped) {
        boolean[][] result = originalShape;
        
        // Apply flipping first if needed
        if (flipped) {
            result = flipShape(result);
        }
        
        // Then apply rotation
        for (int i = 0; i < rotation; i++) {
            result = rotateShape(result);
        }
        
        return result;
    }
    
    /**
     * Rotates a shape 90 degrees clockwise
     */
    private boolean[][] rotateShape(boolean[][] shape) {
        int rows = shape.length;
        int cols = shape[0].length;
        boolean[][] rotated = new boolean[cols][rows];
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = shape[r][c];
            }
        }
        
        return rotated;
    }
    
    /**
     * Flips a shape horizontally
     */
    private boolean[][] flipShape(boolean[][] shape) {
        int rows = shape.length;
        int cols = shape[0].length;
        boolean[][] flipped = new boolean[rows][cols];
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                flipped[r][cols - 1 - c] = shape[r][c];
            }
        }
        
        return flipped;
    }
    
    /**
     * Checks if a piece is entirely on the board
     */
    private boolean isPieceOnBoard(boolean[][] shape, int x, int y) {
        if (shape == null) {
            System.out.println("ERROR: Shape is null in isPieceOnBoard");
            return false;
        }
        
        System.out.println("DEBUG - Checking if piece is on board at position (" + x + "," + y + ")");
        
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    int absX = x + c;
                    int absY = y + r;
                    
                    if (!isValidPosition(absX, absY)) {
                        System.out.println("DEBUG - Cell at relative (" + r + "," + c + ") -> absolute (" + 
                                          absX + "," + absY + ") is out of bounds");
                        return false;
                    }
                }
            }
        }
        
        System.out.println("DEBUG - Piece is fully on board at position (" + x + "," + y + ")");
        return true;
    }
    
    /**
     * Counts the number of cells in a piece shape
     */
    private int countCells(boolean[][] shape) {
        int count = 0;
        for (boolean[] row : shape) {
            for (boolean cell : row) {
                if (cell) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Filters pieces by color
     */
    private List<Map<String, Object>> filterPiecesByColor(List<Map<String, Object>> pieces, 
                                                       String colorName) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        
        for (Map<String, Object> piece : pieces) {
            String pieceColor = (String) piece.get("pieceColor");
            if (colorName.equalsIgnoreCase(pieceColor)) {
                filtered.add(piece);
            }
        }
        
        return filtered;
    }
    
    /**
     * Filters to get only available pieces
     */
    private List<Piece> filterAvailablePieces(List<Piece> allPieces, Set<String> availablePieceIds) {
        List<Piece> available = new ArrayList<>();
        
        for (Piece piece : allPieces) {
            if (availablePieceIds.contains(String.valueOf(piece.getId()))) {
                available.add(piece);
            }
        }
        
        return available;
    }
    
    /**
     * Gets the starting corner for a given color
     */
    private int[] getStartingCornerForColor(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "BLUE" -> STARTING_CORNERS[0];
            case "YELLOW" -> STARTING_CORNERS[1];
            case "RED" -> STARTING_CORNERS[2];
            case "GREEN" -> STARTING_CORNERS[3];
            default -> null;
        };
    }
    
    /**
     * Checks if a position is valid (on the board)
     */
    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }
    
    /**
     * Shuffles a list for randomization
     */
    private <T> void shuffleList(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }
    
    // Add helper method to check edge and diagonal contact for a given placement
    private boolean isValidBlokusPlacement(boolean[][] shape, int x, int y, String colorName, List<Map<String, Object>> allPlacedPieces) {
        // Build a board of same-color pieces
        boolean[][] sameColorBoard = new boolean[20][20];
        for (Map<String, Object> piece : allPlacedPieces) {
            if (!colorName.equalsIgnoreCase((String) piece.get("pieceColor"))) continue;
            String placedPieceId = (String) piece.get("pieceId");
            int placedX = (int) piece.get("x");
            int placedY = (int) piece.get("y");
            Integer placedRotation = (Integer) piece.get("rotation");
            Boolean placedFlipped = (Boolean) piece.get("flipped");
            boolean[][] placedShape = getTransformedShape(PieceFactory.createPieces(colorName).stream().filter(p -> String.valueOf(p.getId()).equals(placedPieceId)).findFirst().get().getShape(), placedRotation != null ? placedRotation : 0, placedFlipped != null ? placedFlipped : false);
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
        // Build a set of all coordinates occupied by the new piece
        java.util.HashSet<String> newPieceCoords = new java.util.HashSet<>();
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    int absX = x + c;
                    int absY = y + r;
                    newPieceCoords.add(absX + "," + absY);
                }
            }
        }
        // Check for diagonal and edge contact
        boolean hasDiagonalTouch = false;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    int absX = x + c;
                    int absY = y + r;
                    // Check edge contact
                    int[][] edgeOffsets = {{-1,0},{1,0},{0,-1},{0,1}};
                    for (int[] off : edgeOffsets) {
                        int nx = absX + off[0];
                        int ny = absY + off[1];
                        if (nx >= 0 && nx < 20 && ny >= 0 && ny < 20 && sameColorBoard[ny][nx]) {
                            return false; // Immediately invalidate if any edge contact
                        }
                    }
                    // Check diagonal contact (must not be part of the new piece)
                    int[][] diagOffsets = {{-1,-1},{1,-1},{-1,1},{1,1}};
                    for (int[] off : diagOffsets) {
                        int nx = absX + off[0];
                        int ny = absY + off[1];
                        String coordKey = nx + "," + ny;
                        if (nx >= 0 && nx < 20 && ny >= 0 && ny < 20 && sameColorBoard[ny][nx] && !newPieceCoords.contains(coordKey)) {
                            hasDiagonalTouch = true;
                        }
                    }
                }
            }
        }
        // For the first move, there are no placed pieces, so allow
        if (allPlacedPieces.stream().noneMatch(p -> colorName.equalsIgnoreCase((String)p.get("pieceColor")))) {
            return true;
        }
        return hasDiagonalTouch;
    }
} 