package com.blokus.blokus.service.ai;

import com.blokus.blokus.model.Piece;

/**
 * Utility class for AI to transform pieces (rotate, flip) 
 * and evaluate different piece orientations.
 */
public class AiPieceTransformer {
    
    /**
     * Rotates a piece shape clockwise by the specified number of times
     * 
     * @param shape The original piece shape as a 2D boolean array
     * @param rotations Number of 90-degree clockwise rotations to apply
     * @return The rotated shape
     */
    public boolean[][] rotateShape(boolean[][] shape, int rotations) {
        boolean[][] result = copyShape(shape);
        for (int i = 0; i < (rotations % 4); i++) {
            result = rotateOnce(result);
        }
        return result;
    }
    
    /**
     * Rotates a piece shape clockwise once (90 degrees)
     * 
     * @param shape The piece shape to rotate
     * @return The rotated shape
     */
    private boolean[][] rotateOnce(boolean[][] shape) {
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
     * Flips a piece shape horizontally
     * 
     * @param shape The piece shape to flip
     * @return The flipped shape
     */
    public boolean[][] flipShape(boolean[][] shape) {
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
     * Creates a deep copy of a shape array
     * 
     * @param shape The shape to copy
     * @return A new array with the same contents
     */
    private boolean[][] copyShape(boolean[][] shape) {
        int rows = shape.length;
        int cols = shape[0].length;
        boolean[][] copy = new boolean[rows][cols];
        
        for (int r = 0; r < rows; r++) {
            System.arraycopy(shape[r], 0, copy[r], 0, cols);
        }
        
        return copy;
    }
    
    /**
     * Generates all possible transformations of a piece shape
     * (rotations and flips)
     * 
     * @param piece The piece to transform
     * @return Array of all possible unique transformations
     */
    public boolean[][][] getAllTransformations(Piece piece) {
        boolean[][] originalShape = piece.getShape();
        
        // At most 8 possible transformations (4 rotations, and 4 flipped rotations)
        boolean[][][] transformations = new boolean[8][][];
        
        // Add rotations
        for (int i = 0; i < 4; i++) {
            transformations[i] = rotateShape(originalShape, i);
        }
        
        // Flip the piece and add rotations of the flipped piece
        boolean[][] flippedShape = flipShape(originalShape);
        for (int i = 0; i < 4; i++) {
            transformations[4 + i] = rotateShape(flippedShape, i);
        }
        
        return transformations;
    }
    
    /**
     * Gets the dimensions of a piece shape
     * 
     * @param shape The piece shape
     * @return An array [height, width]
     */
    public int[] getShapeDimensions(boolean[][] shape) {
        return new int[]{shape.length, shape[0].length};
    }
} 