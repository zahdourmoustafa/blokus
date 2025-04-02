package com.blokus.blokus.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "game_id")
    private Game game;
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Piece> pieces = new ArrayList<>();
    
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[][] gridState;
    
    @Column(name = "current_player_index")
    private int currentPlayerIndex;
    
    // Constructeur pour initialiser un plateau vide
    public Board() {
        this.gridState = new byte[20][20];
        this.currentPlayerIndex = 0;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public void setPieces(List<Piece> pieces) {
        this.pieces = pieces;
    }

    public byte[][] getGridState() {
        return gridState;
    }

    public void setGridState(byte[][] gridState) {
        this.gridState = gridState;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }
    
    // Méthodes utilitaires
    public void addPiece(Piece piece) {
        pieces.add(piece);
        piece.setBoard(this);
    }
    
    public void removePiece(Piece piece) {
        pieces.remove(piece);
        piece.setBoard(null);
    }
    
    // Méthode pour obtenir la valeur d'une cellule
    public byte getCellValue(int x, int y) {
        if (x < 0 || x >= 20 || y < 0 || y >= 20) {
            return -1; // Hors limites
        }
        return gridState[y][x];
    }
    
    // Méthode pour définir une cellule
    public void setCellValue(int x, int y, byte value) {
        if (x >= 0 && x < 20 && y >= 0 && y < 20) {
            gridState[y][x] = value;
        }
    }
    
    // Méthode pour avancer au joueur suivant
    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;
    }
} 