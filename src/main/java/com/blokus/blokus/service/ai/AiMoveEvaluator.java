package com.blokus.blokus.service.ai;

import java.util.List;
import java.util.Map;

import com.blokus.blokus.model.Piece;

/**
 * Evaluates potential moves for the AI and assigns scores based on strategic value.
 */
public class AiMoveEvaluator {
    
    // Board size for Blokus
    private static final int BOARD_SIZE = 20;
    
    // Weight factors for different evaluation criteria
    private static final double PIECE_SIZE_WEIGHT = 1.5;
    private static final double CORNER_ACCESS_WEIGHT = 2.0;
    private static final double BOARD_CONTROL_WEIGHT = 1.0;
    private static final double BLOCKING_OPPONENT_WEIGHT = 1.2;
    
    public AiMoveEvaluator() {
        // No initialization needed
    }
    
    /**
     * Evaluates a potential move and returns a score.
     * Higher scores indicate better moves.
     * 
     * @param piece The piece to place
     * @param pieceShape The transformed shape of the piece
     * @param x X-coordinate of placement
     * @param y Y-coordinate of placement
     * @param placedPieces List of pieces already placed on the board
     * @param colorName Color of the current player
     * @return Score for this move
     */
    public double evaluateMove(Piece piece, boolean[][] pieceShape, int x, int y, 
                              List<Map<String, Object>> placedPieces, String colorName) {
        
        // Base score is the size of the piece (prefer placing larger pieces early)
        double score = calculatePieceSizeScore(pieceShape) * PIECE_SIZE_WEIGHT;
        
        // Add score for moves that maintain access to corners
        score += evaluateCornerAccess(pieceShape, x, y, placedPieces) * CORNER_ACCESS_WEIGHT;
        
        // Add score for board control (center and expansion)
        score += evaluateBoardControl(pieceShape, x, y) * BOARD_CONTROL_WEIGHT;
        
        // Add score for blocking opponent moves
        score += evaluateBlockingOpponent(pieceShape, x, y, placedPieces, colorName) * BLOCKING_OPPONENT_WEIGHT;
        
        return score;
    }
    
    /**
     * Calculates a score based on the piece size.
     * Larger pieces get higher scores.
     */
    private double calculatePieceSizeScore(boolean[][] pieceShape) {
        int count = 0;
        for (boolean[] row : pieceShape) {
            for (boolean cell : row) {
                if (cell) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Evaluates how well the move maintains access to diagonal corners
     * for future piece placements.
     */
    private double evaluateCornerAccess(boolean[][] pieceShape, int x, int y, 
                                      List<Map<String, Object>> placedPieces) {
        double score = 0;
        
        // Check each cell of the piece
        for (int r = 0; r < pieceShape.length; r++) {
            for (int c = 0; c < pieceShape[0].length; c++) {
                if (pieceShape[r][c]) {
                    int absX = x + c;
                    int absY = y + r;
                    
                    // Check the 4 diagonal corners of this cell
                    int[][] corners = {
                        {absX - 1, absY - 1}, {absX + 1, absY - 1}, 
                        {absX - 1, absY + 1}, {absX + 1, absY + 1}
                    };
                    
                    for (int[] corner : corners) {
                        int cornerX = corner[0];
                        int cornerY = corner[1];
                        
                        // Check if this corner is on the board and not occupied
                        if (isValidPosition(cornerX, cornerY) && !isPositionOccupied(cornerX, cornerY, placedPieces)) {
                            score += 0.5;
                        }
                    }
                }
            }
        }
        
        return score;
    }
    
    /**
     * Evaluates how well the move controls the board.
     * Higher scores for moves toward the center in early game,
     * and for expansive moves in general.
     */
    private double evaluateBoardControl(boolean[][] pieceShape, int x, int y) {
        double score = 0;
        int centerX = BOARD_SIZE / 2;
        int centerY = BOARD_SIZE / 2;
        
        // Calculate average distance from center
        double totalDistance = 0;
        int cellCount = 0;
        
        for (int r = 0; r < pieceShape.length; r++) {
            for (int c = 0; c < pieceShape[0].length; c++) {
                if (pieceShape[r][c]) {
                    int absX = x + c;
                    int absY = y + r;
                    
                    // Calculate distance to center
                    double distance = Math.sqrt(Math.pow(absX - centerX, 2) + Math.pow(absY - centerY, 2));
                    totalDistance += distance;
                    cellCount++;
                }
            }
        }
        
        if (cellCount > 0) {
            double avgDistanceFromCenter = totalDistance / cellCount;
            
            // Normalize the distance so closer to center is better
            // Max distance is from corner to center, which is approximately BOARD_SIZE / sqrt(2)
            double maxPossibleDistance = BOARD_SIZE / Math.sqrt(2);
            double normalizedDistance = 1.0 - (avgDistanceFromCenter / maxPossibleDistance);
            
            score += normalizedDistance * 5; // Scale to make this component meaningful
        }
        
        return score;
    }
    
    /**
     * Evaluates how well the move blocks opponent pieces.
     * Higher scores for moves that restrict opponent's possible moves.
     */
    private double evaluateBlockingOpponent(boolean[][] pieceShape, int x, int y, 
                                          List<Map<String, Object>> placedPieces, 
                                          String playerColor) {
        double score = 0;
        
        // Identify opponent pieces (pieces of different color)
        List<Map<String, Object>> opponentPieces = placedPieces.stream()
            .filter(piece -> !playerColor.equalsIgnoreCase((String) piece.get("pieceColor")))
            .toList();
        
        if (opponentPieces.isEmpty()) {
            return 0; // No opponent pieces yet
        }
        
        // Check if we're placing adjacent to opponent pieces (blocking them)
        for (int r = 0; r < pieceShape.length; r++) {
            for (int c = 0; c < pieceShape[0].length; c++) {
                if (pieceShape[r][c]) {
                    int absX = x + c;
                    int absY = y + r;
                    
                    // Check adjacent positions
                    int[][] adjacentPositions = {
                        {absX - 1, absY}, {absX + 1, absY}, 
                        {absX, absY - 1}, {absX, absY + 1}
                    };
                    
                    for (int[] pos : adjacentPositions) {
                        if (isOpponentAdjacent(pos[0], pos[1], opponentPieces)) {
                            score += 1.0;
                        }
                    }
                }
            }
        }
        
        return score;
    }
    
    /**
     * Checks if a position is adjacent to an opponent's piece
     */
    private boolean isOpponentAdjacent(int x, int y, List<Map<String, Object>> opponentPieces) {
        for (Map<String, Object> piece : opponentPieces) {
            int pieceX = ((Number) piece.get("x")).intValue();
            int pieceY = ((Number) piece.get("y")).intValue();
            
            // Get the opponent piece shape
            boolean[][] opponentShape = getPieceShapeFromPlacedPiece();
            
            for (int r = 0; r < opponentShape.length; r++) {
                for (int c = 0; c < opponentShape[0].length; c++) {
                    if (opponentShape[r][c]) {
                        int absX = pieceX + c;
                        int absY = pieceY + r;
                        
                        if (absX == x && absY == y) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Gets the shape of a piece from placement data
     */
    private boolean[][] getPieceShapeFromPlacedPiece() {
        // In a future implementation, this method will use placement data to get the correct piece shape.
        
        // For now, return a simple representation
        boolean[][] shape = {{true}};
        
        // In a full implementation, we would:
        // 1. Get the original shape from PieceFactory
        // 2. Apply rotation (0, 1, 2, or 3 times) 
        // 3. Apply flip if needed
        
        return shape;
    }
    
    /**
     * Checks if a position is valid (on the board)
     */
    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }
    
    /**
     * Checks if a position is already occupied by any piece
     */
    private boolean isPositionOccupied(int x, int y, List<Map<String, Object>> placedPieces) {
        for (Map<String, Object> piece : placedPieces) {
            int pieceX = ((Number) piece.get("x")).intValue();
            int pieceY = ((Number) piece.get("y")).intValue();
            
            // Get the piece shape - using a simple 1x1 shape since actual shape isn't implemented yet
            boolean[][] shape = {{true}};
            
            // Check if any cell of the piece is at the given position
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[0].length; c++) {
                    if (shape[r][c]) {
                        int absX = pieceX + c;
                        int absY = pieceY + r;
                        
                        if (absX == x && absY == y) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
} 