package com.blokus.blokus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating the 21 standard Blokus game pieces
 */
public class PieceFactory {

    /**
     * Creates all 21 Blokus pieces with the specified color
     * 
     * @param color The color for the pieces
     * @return List of all 21 Blokus pieces
     */
    public static List<Piece> createPieces(String color) {
        List<Piece> pieces = new ArrayList<>();
        
        // Piece 1
        pieces.add(new Piece(1, new boolean[][] {
            {true}
        }, color));
        
        // Piece 2
        pieces.add(new Piece(2, new boolean[][] {
            {true},
            {true}
        }, color));
        
        // Piece 3
        pieces.add(new Piece(3, new boolean[][] {
            {true},
            {true},
            {true}
        }, color));
        
        // Piece 4
        pieces.add(new Piece(4, new boolean[][] {
            {true, false},
            {true, true}
        }, color));
        
        // Piece 5
        pieces.add(new Piece(5, new boolean[][] {
            {true},
            {true},
            {true},
            {true}
        }, color));
        
        // Piece 6
        pieces.add(new Piece(6, new boolean[][] {
            {false, true},
            {false, true},
            {true, true}
        }, color));
        
        // Piece 7
        pieces.add(new Piece(7, new boolean[][] {
            {true, false},
            {true, true},
            {true, false}
        }, color));
        
        // Piece 8
        pieces.add(new Piece(8, new boolean[][] {
            {true, true},
            {true, true}
        }, color));
        
        // Piece 9
        pieces.add(new Piece(9, new boolean[][] {
            {true, true, false},
            {false, true, true}
        }, color));
        
        // Piece 10
        pieces.add(new Piece(10, new boolean[][] {
            {true},
            {true},
            {true},
            {true},
            {true}
        }, color));
        
        // Piece 11
        pieces.add(new Piece(11, new boolean[][] {
            {false, true},
            {false, true},
            {false, true},
            {true, true,}
        }, color));
        
        // Piece 12
        pieces.add(new Piece(12, new boolean[][] {
            {false, true},
            {false, true},
            {true, true},
            {true, false}

        }, color));
        
        // Piece 13
        pieces.add(new Piece(13, new boolean[][] {
            {false, true},
            {true, true},
            {true, true}
        }, color));
        
        // Piece 14
        pieces.add(new Piece(14, new boolean[][] {
            {true, true},
            {false, true},
            {true, true}

        }, color));
        
        // Piece 15
        pieces.add(new Piece(15, new boolean[][] {
            {true, false},
            {true, true},
            {true, false},
            {true, false}

        }, color));
        
        // Piece 16
        pieces.add(new Piece(16, new boolean[][] {
            {false, true, false},
            {false, true, false},
            {true, true, true}
        }, color));
        
        // Piece 17
        pieces.add(new Piece(17, new boolean[][] {
            {true, false, false},
            {true, false, false},
            {true, true, true}
        }, color));
        
        // Piece 18
        pieces.add(new Piece(18, new boolean[][] {
            {true, true, false},
            {false, true, true},
            {false, false, true}
        }, color));
        
        // Piece 19
        pieces.add(new Piece(19, new boolean[][] {
            {true, false, false},
            {true, true, true},
            {false, false, true}
        }, color));
        
        // Piece 20
        pieces.add(new Piece(20, new boolean[][] {
            {true, false, false},
            {true, true, true},
            {false, true,false}
        }, color));
        
        // Piece 21
        pieces.add(new Piece(21, new boolean[][] {
            {false, true, false},
            {true, true, true},
            {false, true, false}
        }, color));
        
        return pieces;
    }
} 