package com.blokus.blokus.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
public class Board implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "game_id")
    private Game game;
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Piece> pieces = new ArrayList<>();
    
    // Changed from byte[][] to byte[] for compatibility with JPA/JDBC
    @Lob
    @Column(name = "grid_state", columnDefinition = "BLOB")
    private byte[] gridState;
    
    @Column(name = "current_player_index")
    private int currentPlayerIndex;
    
    // Transient field to hold the 2D grid in memory
    @Transient
    private byte[][] grid;
    
    // Constructeur pour initialiser un plateau vide
    public Board() {
        this.grid = new byte[20][20];
        this.currentPlayerIndex = 0;
        this.serializeGrid(); // Initialize gridState
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

    // Getter for the actual 2D grid
    public byte[][] getGrid() {
        if (grid == null && gridState != null) {
            deserializeGrid();
        }
        return grid;
    }

    public void setGrid(byte[][] grid) {
        this.grid = grid;
        serializeGrid();
    }
    
    // Direct access to the serialized state (should be used only by JPA)
    public byte[] getGridState() {
        return gridState;
    }

    public void setGridState(byte[] gridState) {
        this.gridState = gridState;
        if (gridState != null) {
            deserializeGrid();
        }
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
        if (grid == null) {
            deserializeGrid();
        }
        return grid[y][x];
    }
    
    // Méthode pour définir une cellule
    public void setCellValue(int x, int y, byte value) {
        if (x >= 0 && x < 20 && y >= 0 && y < 20) {
            if (grid == null) {
                deserializeGrid();
            }
            grid[y][x] = value;
            serializeGrid(); // Update serialized state
        }
    }
    
    // Méthode pour avancer au joueur suivant
    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;
    }
    
    // Helper method to serialize the 2D grid into a 1D byte array
    private void serializeGrid() {
        if (grid == null) {
            return;
        }
        
        // Serialize the 2D array into a 1D array
        gridState = new byte[20 * 20];
        for (int y = 0; y < 20; y++) {
            // Use System.arraycopy for better performance
            System.arraycopy(grid[y], 0, gridState, y * 20, 20);
        }
    }
    
    // Helper method to deserialize the 1D byte array back into a 2D grid
    private void deserializeGrid() {
        if (gridState == null) {
            grid = new byte[20][20];
            return;
        }
        
        // Deserialize the 1D array back into a 2D array
        grid = new byte[20][20];
        for (int y = 0; y < 20; y++) {
            if ((y + 1) * 20 <= gridState.length) {
                // Use System.arraycopy for better performance
                System.arraycopy(gridState, y * 20, grid[y], 0, 20);
            }
        }
    }
    
    // JPA lifecycle methods
    @PrePersist
    @PreUpdate
    public void onSave() {
        serializeGrid();
    }
    
    @PostLoad
    public void onLoad() {
        deserializeGrid();
    }
} 