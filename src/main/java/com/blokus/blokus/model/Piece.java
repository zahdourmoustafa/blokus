package com.blokus.blokus.model;

import java.util.Arrays;

/**
 * Represents a Blokus game piece
 */
public class Piece {
    private int id;
    private boolean[][] shape;
    private String color;
    private boolean isPlaced;

    public Piece(int id, boolean[][] shape, String color) {
        this.id = id;
        this.shape = shape;
        this.color = color;
        this.isPlaced = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean[][] getShape() {
        return shape;
    }

    public void setShape(boolean[][] shape) {
        this.shape = shape;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isPlaced() {
        return isPlaced;
    }

    public void setPlaced(boolean placed) {
        isPlaced = placed;
    }

    public int getWidth() {
        return shape[0].length;
    }

    public int getHeight() {
        return shape.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (boolean[] row : shape) {
            for (boolean cell : row) {
                sb.append(cell ? "■" : " ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
} 