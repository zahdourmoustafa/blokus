package com.blokus.blokus.service.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.service.GameLogicService;

/**
 * Implements strategies for AI piece placement.
 * Handles different scenarios like first move, subsequent moves, etc.
 */
public class AiMovePlacementStrategy {
    
    private final GameLogicService gameLogicService;
    private final AiPieceTransformer pieceTransformer;
    private final AiMoveEvaluator moveEvaluator;
    private final Random random;
    
    // Board size constants
    private static final int BOARD_SIZE = 20;
    
    // Starting corner positions for each color (standard Blokus rules)
    private static final int[][] STARTING_CORNERS = {
        {0, 0},      // BLUE: top-left
        {19, 0},     // YELLOW: top-right
        {0, 19},     // RED: bottom-left
        {19, 19}     // GREEN: bottom-right
    };
    
    public AiMovePlacementStrategy(GameLogicService gameLogicService, 
                                 AiPieceTransformer pieceTransformer,
                                 AiMoveEvaluator moveEvaluator) {
        this.gameLogicService = gameLogicService;
        this.pieceTransformer = pieceTransformer;
        this.moveEvaluator = moveEvaluator;
        this.random = new Random();
    }
    
    /**
     * Places the first piece for an AI player in their starting corner.
     * 
     * @param gameId Game ID
     * @param aiPlayer The AI player
     * @param colorName Color of the AI player
     * @param availablePieces List of available pieces to place
     * @return true if placement successful, false otherwise
     */
    public boolean placeFirstPiece(Long gameId, GameUser aiPlayer, String colorName, 
                                 List<Piece> availablePieces) {
        System.out.println("AI " + colorName + " placing first piece");
        
        // Get the starting corner for this color
        int[] corner = getStartingCornerForColor(colorName);
        if (corner == null) {
            System.out.println("Error: Invalid color name: " + colorName);
            return false;
        }
        
        int cornerX = corner[0];
        int cornerY = corner[1];
        
        System.out.println("Starting corner for " + colorName + ": (" + cornerX + "," + cornerY + ")");
        
        // Sort pieces by size (larger first for better strategy)
        List<Piece> sortedPieces = new ArrayList<>(availablePieces);
        sortedPieces.sort(Comparator.comparingInt(p -> -countCells(p.getShape())));
        
        // Try to place each piece, starting with the largest ones
        for (Piece piece : sortedPieces) {
            // For the first piece, try all possible orientations to find one that fits in the corner
            boolean[][][] transformations = pieceTransformer.getAllTransformations(piece);
            
            for (int i = 0; i < transformations.length; i++) {
                boolean[][] shape = transformations[i];
                int rotation = i % 4;  // 0, 1, 2, or 3
                boolean flipped = i >= 4;  // true for transformations[4] through transformations[7]
                
                // Adjust placement to make sure one cell occupies the corner
                int[] placementOffsets = findPlacementOffsetsForCorner(shape, cornerX, cornerY);
                
                if (placementOffsets != null) {
                    int x = placementOffsets[0];
                    int y = placementOffsets[1];
                    
                    // Try to place the piece
                    boolean placed = gameLogicService.placePiece(
                            gameId, 
                            aiPlayer.getUser().getId(), 
                            String.valueOf(piece.getId()), 
                            colorName, 
                            x, y, 
                            rotation, 
                            flipped);
                    
                    if (placed) {
                        System.out.println("First piece placed successfully: " + piece.getId() + 
                                           " at (" + x + "," + y + ") rotation=" + rotation + 
                                           " flipped=" + flipped);
                        
                        // Update player's available pieces
                        aiPlayer.getAvailablePieceIds().remove(String.valueOf(piece.getId()));
                        
                        return true;
                    }
                }
            }
        }
        
        System.out.println("Failed to place first piece for AI " + colorName);
        return false;
    }
    
    /**
     * Places a subsequent piece for an AI player.
     * Uses diagonal corners of already placed pieces.
     * 
     * @param gameId Game ID
     * @param colorName Color of the AI player
     * @param availablePieces List of available pieces to place
     * @param placedPieces List of pieces already placed by this player
     * @return true if placement successful, false otherwise
     */
    public boolean placeSubsequentPiece(Long gameId, String colorName, 
                                      List<Piece> availablePieces, 
                                      List<Map<String, Object>> placedPieces) {
        System.out.println("AI " + colorName + " placing subsequent piece");
        
        // If no pieces have been placed yet by this player, should use placeFirstPiece instead
        if (placedPieces.isEmpty()) {
            System.out.println("Error: No placed pieces found for " + colorName);
            return false;
        }
        
        // Sort available pieces by size (larger first for better strategy)
        List<Piece> sortedPieces = new ArrayList<>(availablePieces);
        sortedPieces.sort(Comparator.comparingInt(p -> -countCells(p.getShape())));
        
        // Find all valid moves with scores
        List<AiMove> validMoves = findAllValidMoves(gameId, colorName, sortedPieces, placedPieces);
        
        if (validMoves.isEmpty()) {
            System.out.println("No valid moves found for AI " + colorName);
            return false;
        }
        
        // Sort moves by score (highest first)
        validMoves.sort(Comparator.comparingDouble(AiMove::getScore).reversed());
        
        // Get the best move, with a small chance of choosing another high-scoring move for variety
        AiMove selectedMove;
        if (random.nextDouble() < 0.8 || validMoves.size() == 1) {
            // 80% of the time, choose the highest-scoring move
            selectedMove = validMoves.get(0);
        } else {
            // 20% of the time, choose randomly from the top 3 moves (or fewer if not enough)
            int maxIndex = Math.min(3, validMoves.size()) - 1;
            selectedMove = validMoves.get(random.nextInt(maxIndex + 1));
        }
        
        // Place the selected piece
        boolean placed = gameLogicService.placePiece(
                gameId,
                null, // userId is null for AI
                String.valueOf(selectedMove.getPiece().getId()),
                colorName,
                selectedMove.getX(),
                selectedMove.getY(),
                selectedMove.getRotation(),
                selectedMove.isFlipped());
        
        if (placed) {
            System.out.println("Piece placed successfully: " + selectedMove.getPiece().getId() + 
                               " at (" + selectedMove.getX() + "," + selectedMove.getY() + ") " +
                               "rotation=" + selectedMove.getRotation() + 
                               " flipped=" + selectedMove.isFlipped());
            return true;
        } else {
            System.out.println("Failed to place piece: " + selectedMove.getPiece().getId());
            return false;
        }
    }
    
    /**
     * Finds all valid moves for the AI player and scores them
     */
    private List<AiMove> findAllValidMoves(Long gameId, String colorName, 
                                         List<Piece> availablePieces, 
                                         List<Map<String, Object>> placedPieces) {
        List<AiMove> validMoves = new ArrayList<>();
        
        // Get all placed pieces (by all players) to check for valid placements
        List<Map<String, Object>> allPlacedPieces = gameLogicService.getPlacedPieces(gameId);
        
        // Extract diagonal corners from this player's previously placed pieces
        List<int[]> diagonalCorners = extractDiagonalCorners(placedPieces);
        
        for (Piece piece : availablePieces) {
            boolean[][][] transformations = pieceTransformer.getAllTransformations(piece);
            
            for (int i = 0; i < transformations.length; i++) {
                boolean[][] shape = transformations[i];
                int rotation = i % 4;
                boolean flipped = i >= 4;
                
                // Try to place the piece at each diagonal corner
                for (int[] corner : diagonalCorners) {
                    int cornerX = corner[0];
                    int cornerY = corner[1];
                    
                    // Find all possible ways to place this piece touching this corner diagonally
                    List<int[]> placementPositions = findDiagonalPlacementPositions(shape, cornerX, cornerY);
                    
                    for (int[] position : placementPositions) {
                        int x = position[0];
                        int y = position[1];
                        
                        // Check if this is a valid placement
                        if (isValidPlacement(shape, x, y, colorName, allPlacedPieces)) {
                            // Evaluate this move
                            double score = moveEvaluator.evaluateMove(
                                    piece, shape, x, y, allPlacedPieces, colorName);
                            
                            // Add to list of valid moves
                            validMoves.add(new AiMove(piece, shape, x, y, rotation, flipped, score));
                        }
                    }
                }
            }
        }
        
        return validMoves;
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
     * Extracts all potential diagonal corners from previously placed pieces
     */
    private List<int[]> extractDiagonalCorners(List<Map<String, Object>> placedPieces) {
        List<int[]> corners = new ArrayList<>();
        
        for (Map<String, Object> piece : placedPieces) {
            int pieceX = ((Number) piece.get("x")).intValue();
            int pieceY = ((Number) piece.get("y")).intValue();
            
            // Get the piece shape
            boolean[][] shape = getPieceShapeFromPlacedPiece();
            
            // Add diagonal corners for each cell of the piece
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[0].length; c++) {
                    if (shape[r][c]) {
                        int absX = pieceX + c;
                        int absY = pieceY + r;
                        
                        // Add the four diagonal corners if they're valid positions
                        int[][] potentialCorners = {
                            {absX - 1, absY - 1}, {absX + 1, absY - 1},
                            {absX - 1, absY + 1}, {absX + 1, absY + 1}
                        };
                        
                        for (int[] corner : potentialCorners) {
                            if (isValidPosition(corner[0], corner[1])) {
                                corners.add(corner);
                            }
                        }
                    }
                }
            }
        }
        
        return corners;
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
     * Finds the placement offsets for a piece to make sure it covers a specific corner
     */
    private int[] findPlacementOffsetsForCorner(boolean[][] shape, int cornerX, int cornerY) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    // Calculate placement so that this cell is at the corner
                    int x = cornerX - c;
                    int y = cornerY - r;
                    
                    // Check if this placement keeps the entire piece on the board
                    if (isEntirePieceOnBoard(shape, x, y)) {
                        return new int[]{x, y};
                    }
                }
            }
        }
        
        return null; // No valid placement found
    }
    
    /**
     * Finds positions to place a piece such that it touches a corner diagonally
     */
    private List<int[]> findDiagonalPlacementPositions(boolean[][] shape, int cornerX, int cornerY) {
        List<int[]> positions = new ArrayList<>();
        
        // Find all cells in the shape that could be positioned to create a diagonal connection
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    // Check each diagonal direction from this cell
                    int[][] diagonalOffsets = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
                    
                    for (int[] offset : diagonalOffsets) {
                        int cellX = cornerX - offset[0];
                        int cellY = cornerY - offset[1];
                        
                        // Calculate placement position
                        int x = cellX - c;
                        int y = cellY - r;
                        
                        // Check if this placement keeps the entire piece on the board
                        if (isEntirePieceOnBoard(shape, x, y)) {
                            positions.add(new int[]{x, y});
                        }
                    }
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Checks if the entire piece is on the board at the given position
     */
    private boolean isEntirePieceOnBoard(boolean[][] shape, int x, int y) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    int absX = x + c;
                    int absY = y + r;
                    
                    if (!isValidPosition(absX, absY)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a position is valid (on the board)
     */
    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }
    
    /**
     * Checks if a given placement is valid according to Blokus rules
     */
    private boolean isValidPlacement(boolean[][] shape, int x, int y, 
                                   String colorName, List<Map<String, Object>> allPlacedPieces) {
        // 1. Piece must be entirely on the board
        if (!isEntirePieceOnBoard(shape, x, y)) {
            return false;
        }
        
        // 2. Piece must not overlap with any other piece
        if (doesOverlapWithAnyPiece(shape, x, y, allPlacedPieces)) {
            return false;
        }
        
        // 3. Piece must touch at least one piece of the same color diagonally
        // and must not touch any piece of the same color edge-to-edge
        boolean touchesDiagonally = false;
        
        // Filter placed pieces to only those of the same color
        List<Map<String, Object>> sameColorPieces = allPlacedPieces.stream()
                .filter(p -> colorName.equalsIgnoreCase((String) p.get("pieceColor")))
                .toList();
        
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    int absX = x + c;
                    int absY = y + r;
                    
                    // Check for edge-to-edge contact with same color (not allowed)
                    if (hasEdgeContact(absX, absY, sameColorPieces)) {
                        return false;
                    }
                    
                    // Check for diagonal contact with same color (required)
                    if (hasDiagonalContact(absX, absY, sameColorPieces)) {
                        touchesDiagonally = true;
                    }
                }
            }
        }
        
        return touchesDiagonally;
    }
    
    /**
     * Checks if the piece overlaps with any existing piece
     */
    private boolean doesOverlapWithAnyPiece(boolean[][] shape, int x, int y, 
                                          List<Map<String, Object>> allPlacedPieces) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c]) {
                    int absX = x + c;
                    int absY = y + r;
                    
                    // Check if this position is already occupied
                    for (Map<String, Object> piece : allPlacedPieces) {
                        int pieceX = ((Number) piece.get("x")).intValue();
                        int pieceY = ((Number) piece.get("y")).intValue();
                        
                        // Check if this cell overlaps with any cell of the other piece
                        for (int otherR = 0; otherR < shape.length; otherR++) {
                            for (int otherC = 0; otherC < shape[0].length; otherC++) {
                                if (shape[otherR][otherC]) {
                                    int otherAbsX = pieceX + otherC;
                                    int otherAbsY = pieceY + otherR;
                                    
                                    if (absX == otherAbsX && absY == otherAbsY) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a cell has edge contact with any piece of the same color
     */
    private boolean hasEdgeContact(int x, int y, List<Map<String, Object>> sameColorPieces) {
        // Check the four adjacent positions
        int[][] adjacentPositions = {{x-1, y}, {x+1, y}, {x, y-1}, {x, y+1}};
        
        for (int[] pos : adjacentPositions) {
            for (Map<String, Object> piece : sameColorPieces) {
                int pieceX = ((Number) piece.get("x")).intValue();
                int pieceY = ((Number) piece.get("y")).intValue();
                
                boolean[][] shape = getPieceShapeFromPlacedPiece();
                
                for (int r = 0; r < shape.length; r++) {
                    for (int c = 0; c < shape[0].length; c++) {
                        if (shape[r][c]) {
                            int absX = pieceX + c;
                            int absY = pieceY + r;
                            
                            if (absX == pos[0] && absY == pos[1]) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a cell has diagonal contact with any piece of the same color
     */
    private boolean hasDiagonalContact(int x, int y, List<Map<String, Object>> sameColorPieces) {
        // Check the four diagonal positions
        int[][] diagonalPositions = {{x-1, y-1}, {x+1, y-1}, {x-1, y+1}, {x+1, y+1}};
        
        for (int[] pos : diagonalPositions) {
            for (Map<String, Object> piece : sameColorPieces) {
                int pieceX = ((Number) piece.get("x")).intValue();
                int pieceY = ((Number) piece.get("y")).intValue();
                
                boolean[][] shape = getPieceShapeFromPlacedPiece();
                
                for (int r = 0; r < shape.length; r++) {
                    for (int c = 0; c < shape[0].length; c++) {
                        if (shape[r][c]) {
                            int absX = pieceX + c;
                            int absY = pieceY + r;
                            
                            if (absX == pos[0] && absY == pos[1]) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Gets the shape of a piece from placement data.
     * This is a simplified implementation.
     */
    private boolean[][] getPieceShapeFromPlacedPiece() {
        // In a future implementation, this method will use placement data to get the correct piece shape.
        boolean[][] shape = {{true}};
        return shape;
    }
    
    /**
     * Container class to represent a possible AI move with its evaluation score
     */
    private static class AiMove {
        private final Piece piece;
        private final boolean[][] shape;
        private final int x;
        private final int y;
        private final int rotation;
        private final boolean flipped;
        private final double score;
        
        public AiMove(Piece piece, boolean[][] shape, int x, int y, 
                     int rotation, boolean flipped, double score) {
            this.piece = piece;
            this.shape = shape;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.flipped = flipped;
            this.score = score;
        }
        
        public Piece getPiece() {
            return piece;
        }
        
        // Currently not used locally, but kept for potential future use
        // and to maintain consistency with other getters
        @SuppressWarnings("unused")
        public boolean[][] getShape() {
            return shape;
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
        
        public int getRotation() {
            return rotation;
        }
        
        public boolean isFlipped() {
            return flipped;
        }
        
        public double getScore() {
            return score;
        }
    }
} 