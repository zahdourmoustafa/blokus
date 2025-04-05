package com.blokus.blokus.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pieces")
public class Piece {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "board_id")
    private Board board;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    private PieceType type;
    
    @Enumerated(EnumType.STRING)
    private GameUser.PlayerColor color;
    
    private int posX;
    private int posY;
    private int rotation; // 0, 1, 2, 3 (multiples de 90 degrés)
    private boolean flipped;
    
    // Enum pour les types de pièces
    public enum PieceType {
        // Monomino (1 carreau)
        I1,
        
        // Dominos (2 carreaux)
        I2,
        
        // Triominos (3 carreaux)
        I3, L3,
        
        // Tétrominos (4 carreaux)
        I4, L4, Z4, T4, O4,
        
        // Pentominos (5 carreaux)
        I5, L5, U5, Z5, T5, X5, V5, W5, P5, F5, Y5, N5
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PieceType getType() {
        return type;
    }

    public void setType(PieceType type) {
        this.type = type;
    }

    public GameUser.PlayerColor getColor() {
        return color;
    }

    public void setColor(GameUser.PlayerColor color) {
        this.color = color;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }
    
    // Méthode pour obtenir la forme de la pièce en tant que tableau 2D
    public boolean[][] getShape() {
        boolean[][] baseShape = getBaseShape();
        
        // Appliquer la rotation et le retournement
        return transformShape(baseShape, rotation, flipped);
    }
    
    // Méthode pour obtenir la forme de base de la pièce selon son type
    private boolean[][] getBaseShape() {
        return switch (type) {
            // Monomino (1 square) - #1
            case I1 -> new boolean[][] {
                {true}
            };
            
            // Domino (2 squares) - #2
            case I2 -> new boolean[][] {
                {false,true, true}
            };
            
            // Triominoes (3 squares) - #3-4
            case I3 -> new boolean[][] {
                {true, true, true}
            };
            case L3 -> new boolean[][] {
                {false, false},
                {true, false},
                {true, true}
            };
            
            // Tetrominoes (4 squares) - #5-9
            case I4 -> new boolean[][] {
                {true,false,false,false},
                {true,false,false,false},
                {true,false,false,false},
                {true,false,false,false}
            };
            case L4 -> new boolean[][] {
                {false, false, true},
                {false, false, true},
                {false, true, true}
            };
            case Z4 -> new boolean[][] {
                {false, false, false},
                {true, true, false},
                {false, true, true}
            };
            case T4 -> new boolean[][] {
                {true, true},
                {false, true},
                {true, true}

            };
            case O4 -> new boolean[][] {
                {false, false},
                {true, true},
                {true, true}
            };
            
            // Pentominoes (5 squares) - #10-21
            case I5 -> new boolean[][] {
                {true, true, false},
                {false, true, true},
                {false, false, true}
                
            };
            case L5 -> new boolean[][] {
                {true, false, false},
                {true, false, false},
                {true, true, true}
            };
            case U5 -> new boolean[][] {
                {true, false, true},
                {true, true, true}
            };
            case Z5 -> new boolean[][] {
                {true, true, false},
                {false, true, false},
                {false, true, true}
            };
            case T5 -> new boolean[][] {
                {true, false, false},
                {true, true, true},
                {false, true, false}
            };
            case X5 -> new boolean[][] {
                {false, true, false},
                {true, true, true},
                {false, true, false}
            };
            case V5 -> new boolean[][] {
                {false, true, false, false},
                {false, true, false, false},
                {false, true, false, false},
                {true, true, false, false}
            };
            case W5 -> new boolean[][] {
                {true, false,false,false,false},
                {true, false,false,false,false},
                {true, false,false,false,false},
                {true, false,false,false,false},
                {true, false,false,false,false}
            };
            case P5 -> new boolean[][] {
                {false, true},
                {true, true},
                {true, true}
            };
            case F5 -> new boolean[][] {
                {true, false, false, false},
                {true, true, false, false},
                {true, false, false, false},
                {true, false, false, false}
            };
            case Y5 -> new boolean[][] {
                {true, false},
                {true, true},
                {true, false}
            };
            case N5 -> new boolean[][] {
                {false, true, false, false},
                {false, true, false, false},
                {true, true, false, false},
                {true, false, false, false}
            };
        };
    }
    
    // Méthode pour transformer la forme (rotation, flip)
    private boolean[][] transformShape(boolean[][] shape, int rotation, boolean flipped) {
        // Version basique d'implémentation
        int height = shape.length;
        int width = shape[0].length;
        boolean[][] result = shape;
        
        // Appliquer le retournement si nécessaire
        if (flipped) {
            result = new boolean[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result[y][width - 1 - x] = shape[y][x];
                }
            }
        }
        
        // Appliquer la rotation (0, 1, 2, 3 = 0°, 90°, 180°, 270°)
        if (rotation > 0) {
            for (int r = 0; r < rotation; r++) {
                // Rotation de 90 degrés à chaque itération
                boolean[][] rotated = new boolean[width][height];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        rotated[x][height - 1 - y] = result[y][x];
                    }
                }
                result = rotated;
                
                // Échanger les dimensions après rotation
                int temp = height;
                height = width;
                width = temp;
            }
        }
        
        return result;
    }
} 