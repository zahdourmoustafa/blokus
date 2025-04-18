package com.blokus.blokus.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for transferring move information from client to server
 */
public class MoveDto {
    
    @NotNull
    private Long pieceId;
    
    @NotNull
    private Integer x;
    
    @NotNull
    private Integer y;
    
    @NotNull
    private Integer rotation;
    
    private boolean flipped;
    
    // Constructors
    public MoveDto() {
    }
    
    public MoveDto(Long pieceId, Integer x, Integer y, Integer rotation, boolean flipped) {
        this.pieceId = pieceId;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.flipped = flipped;
    }
    
    // Getters and Setters
    public Long getPieceId() {
        return pieceId;
    }
    
    public void setPieceId(Long pieceId) {
        this.pieceId = pieceId;
    }
    
    public Integer getX() {
        return x;
    }
    
    public void setX(Integer x) {
        this.x = x;
    }
    
    public Integer getY() {
        return y;
    }
    
    public void setY(Integer y) {
        this.y = y;
    }
    
    public Integer getRotation() {
        return rotation;
    }
    
    public void setRotation(Integer rotation) {
        this.rotation = rotation;
    }
    
    public boolean isFlipped() {
        return flipped;
    }
    
    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }
} 