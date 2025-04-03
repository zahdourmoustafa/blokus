package com.blokus.blokus.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blokus.blokus.model.Board;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.Game.GameStatus;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.GameUser.PlayerColor;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.model.Piece.PieceType;
import com.blokus.blokus.repository.BoardRepository;
import com.blokus.blokus.repository.GameRepository;
import com.blokus.blokus.repository.GameUserRepository;
import com.blokus.blokus.repository.PieceRepository;
import com.blokus.blokus.service.GameLogicService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implementation of game logic service
 */
@Service
public class GameLogicServiceImpl implements GameLogicService {

    private final GameRepository gameRepository;
    private final GameUserRepository gameUserRepository;
    private final BoardRepository boardRepository;
    private final PieceRepository pieceRepository;

    // Constants for the board size
    private static final int BOARD_SIZE = 20;
    
    // Directions for checking adjacency (diagonals only)
    private static final int[][] DIAGONAL_DIRS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    
    // Directions for checking side adjacency (used in validation)
    private static final int[][] SIDE_DIRS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    public GameLogicServiceImpl(GameRepository gameRepository, GameUserRepository gameUserRepository,
            BoardRepository boardRepository, PieceRepository pieceRepository) {
        this.gameRepository = gameRepository;
        this.gameUserRepository = gameUserRepository;
        this.boardRepository = boardRepository;
        this.pieceRepository = pieceRepository;
    }

    @Override
    @Transactional
    public List<Piece> initializePieces(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
        
        List<GameUser> players = gameUserRepository.findByGameId(gameId);
        List<Piece> allPieces = new ArrayList<>();
        
        // Get the board
        Board board = game.getBoard();
        
        // For each player, create all 21 pieces
        for (GameUser player : players) {
            List<Piece> playerPieces = createPlayerPieces(player, board);
            allPieces.addAll(playerPieces);
        }
        
        return pieceRepository.saveAll(allPieces);
    }
    
    /**
     * Create all 21 pieces for a player
     */
    private List<Piece> createPlayerPieces(GameUser player, Board board) {
        List<Piece> pieces = new ArrayList<>();
        
        // Create one of each piece type for the player
        for (PieceType type : PieceType.values()) {
            Piece piece = new Piece();
            piece.setType(type);
            piece.setUser(player.getUser());
            piece.setColor(player.getColor());
            piece.setBoard(board);
            piece.setRotation(0);
            piece.setFlipped(false);
            pieces.add(piece);
        }
        
        return pieces;
    }

    @Override
    public boolean isValidMove(Long gameId, Long pieceId, int x, int y, int rotation, boolean flipped) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
        
        Piece piece = pieceRepository.findById(pieceId)
                .orElseThrow(() -> new EntityNotFoundException("Piece not found with id: " + pieceId));
        
        // Check game is in progress
        if (game.getStatus() != GameStatus.PLAYING) {
            return false;
        }
        
        // Check if it's the player's turn
        GameUser currentPlayer = getCurrentPlayer(gameId);
        if (!currentPlayer.getUser().getId().equals(piece.getUser().getId())) {
            return false;
        }
        
        // Check if piece has already been placed
        if (piece.getBoard() != null && piece.getPosX() >= 0 && piece.getPosY() >= 0) {
            return false;
        }
        
        // Apply rotation and flip to get the piece shape
        piece.setRotation(rotation);
        piece.setFlipped(flipped);
        boolean[][] shape = piece.getShape();
        
        // Get the board state
        Board board = game.getBoard();
        byte[][] grid = board.getGrid();
        
        // First check: is the piece within bounds?
        if (!isWithinBounds(shape, x, y)) {
            return false;
        }
        
        // Second check: does the piece overlap with another piece?
        if (hasOverlap(grid, shape, x, y)) {
            return false;
        }
        
        // Get the player's color as byte value (1 for first player, 2 for second, etc.)
        byte playerColorValue = getColorValue(currentPlayer.getColor());
        
        // Check if this is the first move for this player
        boolean isFirstMove = isFirstMoveForPlayer(grid, playerColorValue);
        
        // Third check: for first move, piece must touch player's starting corner
        if (isFirstMove) {
            int[] corner = getStartCorner(currentPlayer.getColor());
            return touchesCorner(shape, x, y, corner[0], corner[1]);
        }
        
        // Fourth check: piece must touch at least one corner of the player's pieces
        // but not touch any side of the player's pieces
        return (connectsByCorner(grid, shape, x, y, playerColorValue) && 
                !touchesSameColorSide(grid, shape, x, y, playerColorValue));
    }
    
    /**
     * Check if a piece is within the bounds of the board
     */
    private boolean isWithinBounds(boolean[][] shape, int startX, int startY) {
        int height = shape.length;
        int width = shape[0].length;
        
        return startX >= 0 && startY >= 0 && 
               (startX + width) <= BOARD_SIZE && 
               (startY + height) <= BOARD_SIZE;
    }
    
    /**
     * Check if a piece overlaps with any existing piece on the board
     */
    private boolean hasOverlap(byte[][] grid, boolean[][] shape, int startX, int startY) {
        int height = shape.length;
        int width = shape[0].length;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (shape[y][x] && grid[startY + y][startX + x] != 0) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if this is the first move for a player
     */
    private boolean isFirstMoveForPlayer(byte[][] grid, byte playerColorValue) {
        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                if (grid[y][x] == playerColorValue) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if a piece touches a specific corner
     */
    private boolean touchesCorner(boolean[][] shape, int startX, int startY, int cornerX, int cornerY) {
        int height = shape.length;
        int width = shape[0].length;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (shape[y][x] && (startX + x) == cornerX && (startY + y) == cornerY) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if a piece connects by corner with at least one other piece of the same color
     */
    private boolean connectsByCorner(byte[][] grid, boolean[][] shape, int startX, int startY, 
                                     byte playerColorValue) {
        int height = shape.length;
        int width = shape[0].length;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (shape[y][x]) {
                    int pieceX = startX + x;
                    int pieceY = startY + y;
                    
                    // Check all diagonal directions
                    for (int[] dir : DIAGONAL_DIRS) {
                        int checkX = pieceX + dir[0];
                        int checkY = pieceY + dir[1];
                        
                        if (isValidCoordinate(checkX, checkY) && 
                            grid[checkY][checkX] == playerColorValue) {
                                return true;
                            }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if a piece touches the side of another piece of the same color
     */
    private boolean touchesSameColorSide(byte[][] grid, boolean[][] shape, int startX, int startY,
                                         byte playerColorValue) {
        int height = shape.length;
        int width = shape[0].length;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (shape[y][x]) {
                    int pieceX = startX + x;
                    int pieceY = startY + y;
                    
                    // Check all adjacent sides
                    for (int[] dir : SIDE_DIRS) {
                        int checkX = pieceX + dir[0];
                        int checkY = pieceY + dir[1];
                        
                        if (isValidCoordinate(checkX, checkY) && 
                            grid[checkY][checkX] == playerColorValue) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if coordinates are valid on the board
     */
    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }
    
    /**
     * Convert PlayerColor enum to byte value for the grid
     */
    private byte getColorValue(PlayerColor color) {
        return switch (color) {
            case BLUE -> 1;
            case YELLOW -> 2;
            case RED -> 3;
            case GREEN -> 4;
        };
    }

    @Override
    @Transactional
    public Board placePiece(Long gameId, Long pieceId, int x, int y, int rotation, boolean flipped) {
        if (!isValidMove(gameId, pieceId, x, y, rotation, flipped)) {
            throw new IllegalStateException("Invalid move");
        }
        
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
        
        Piece piece = pieceRepository.findById(pieceId)
                .orElseThrow(() -> new EntityNotFoundException("Piece not found with id: " + pieceId));
        
        // Set piece properties
        piece.setPosX(x);
        piece.setPosY(y);
        piece.setRotation(rotation);
        piece.setFlipped(flipped);
        
        // Save the piece
        pieceRepository.save(piece);
        
        // Update the board state
        Board board = game.getBoard();
        byte[][] grid = board.getGrid();
        
        // Get the player's color as byte value
        byte playerColorValue = getColorValue(piece.getColor());
        
        // Apply the piece to the grid
        boolean[][] shape = piece.getShape();
        for (int shapeY = 0; shapeY < shape.length; shapeY++) {
            for (int shapeX = 0; shapeX < shape[0].length; shapeX++) {
                if (shape[shapeY][shapeX]) {
                    grid[y + shapeY][x + shapeX] = playerColorValue;
                }
            }
        }
        
        // Update the board
        board.setGrid(grid);
        
        // Move to next player
        nextTurn(gameId);
        
        // Check if game is over
        if (isGameOver(gameId)) {
            game.setStatus(GameStatus.FINISHED);
            calculateScores(gameId);
            gameRepository.save(game);
        }
        
        return boardRepository.save(board);
    }

    @Override
    public List<Piece> getAvailablePieces(Long gameId, Long userId) {
        return pieceRepository.findByBoardGameIdAndUserIdAndPosXLessThan(gameId, userId, 0);
    }

    @Override
    public GameUser getCurrentPlayer(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
        
        Board board = game.getBoard();
        int currentPlayerIndex = board.getCurrentPlayerIndex();
        
        List<GameUser> players = gameUserRepository.findByGameIdOrderById(gameId);
        if (currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            throw new IllegalStateException("Invalid player index");
        }
        
        return players.get(currentPlayerIndex);
    }

    @Override
    public boolean canPlayerMove(Long gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
        
        // Get all available pieces for this user
        List<Piece> availablePieces = getAvailablePieces(gameId, userId);
        if (availablePieces.isEmpty()) {
            return false;
        }
        
        // Get the board state
        Board board = game.getBoard();
        byte[][] grid = board.getGrid();
        
        // Get player color
        Optional<GameUser> gameUser = gameUserRepository.findByGameIdAndUserId(gameId, userId);
        if (gameUser.isEmpty()) {
            return false;
        }
        
        PlayerColor playerColor = gameUser.get().getColor();
        byte playerColorValue = getColorValue(playerColor);
        
        // Check if this is the first move
        boolean isFirstMove = isFirstMoveForPlayer(grid, playerColorValue);
        if (isFirstMove) {
            // If first move, check if any piece can be placed at starting corner
            int[] corner = getStartCorner(playerColor);
            for (Piece piece : availablePieces) {
                // Try all rotations and flips
                for (int rotation = 0; rotation < 4; rotation++) {
                    for (boolean flipped : new boolean[]{false, true}) {
                        piece.setRotation(rotation);
                        piece.setFlipped(flipped);
                        boolean[][] shape = piece.getShape();
                        
                        // Try all possible positions that could touch the corner
                        for (int y = Math.max(0, corner[1] - shape.length + 1); 
                             y <= Math.min(corner[1], BOARD_SIZE - shape.length); y++) {
                            for (int x = Math.max(0, corner[0] - shape[0].length + 1); 
                                 x <= Math.min(corner[0], BOARD_SIZE - shape[0].length); x++) {
                                
                                if (isValidMove(gameId, piece.getId(), x, y, rotation, flipped)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            
            // If we get here, no valid first move
            return false;
        }
        
        // For subsequent moves, first find all corners of same color pieces
        List<int[]> corners = findPlayerCorners(grid, playerColorValue);
        
        // For each corner, try to place each available piece
        for (int[] corner : corners) {
            for (Piece piece : availablePieces) {
            // Try all rotations and flips
            for (int rotation = 0; rotation < 4; rotation++) {
                for (boolean flipped : new boolean[]{false, true}) {
                    piece.setRotation(rotation);
                    piece.setFlipped(flipped);
                        boolean[][] shape = piece.getShape();
                        
                        // Try all possible positions that could connect to this corner
                        for (int y = Math.max(0, corner[1] - shape.length - 1); 
                             y <= Math.min(corner[1] + 1, BOARD_SIZE - shape.length); y++) {
                            for (int x = Math.max(0, corner[0] - shape[0].length - 1); 
                                 x <= Math.min(corner[0] + 1, BOARD_SIZE - shape[0].length); x++) {
                                
                                if (isValidMove(gameId, piece.getId(), x, y, rotation, flipped)) {
                                return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // If we get here, no valid move
        return false;
    }

    /**
     * Find all corner positions of pieces of a given color
     */
    private List<int[]> findPlayerCorners(byte[][] grid, byte playerColorValue) {
        List<int[]> corners = new ArrayList<>();
        
        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                if (grid[y][x] == playerColorValue) {
                    // For each piece, check all its corners and add if not occupied
                    for (int[] dir : DIAGONAL_DIRS) {
                        int cornerX = x + dir[0];
                        int cornerY = y + dir[1];
                        
                        if (isValidCoordinate(cornerX, cornerY) && grid[cornerY][cornerX] == 0) {
                            // Make sure it's a true corner (diagonal only)
                            boolean isCorner = true;
                            for (int[] sideDir : SIDE_DIRS) {
                                int sideX = cornerX + sideDir[0];
                                int sideY = cornerY + sideDir[1];
                                
                                if (isValidCoordinate(sideX, sideY) && 
                                    grid[sideY][sideX] == playerColorValue &&
                                    !(sideX == x && sideY == y)) {
                                    isCorner = false;
                                    break;
                                }
                            }
                            
                            if (isCorner) {
                                corners.add(new int[]{cornerX, cornerY});
                            }
                        }
                    }
                }
            }
        }
        
        return corners;
    }

    @Override
    @Transactional
    public GameUser nextTurn(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
        
        Board board = game.getBoard();
        int currentPlayerIndex = board.getCurrentPlayerIndex();
        
        List<GameUser> players = gameUserRepository.findByGameIdOrderById(gameId);
        if (players.isEmpty()) {
            throw new IllegalStateException("No players in game");
        }
        
        // Find the next player who can make a move
        int nextPlayerIndex = (currentPlayerIndex + 1) % players.size();
        int startingIndex = nextPlayerIndex;
        
        do {
            GameUser nextPlayer = players.get(nextPlayerIndex);
            if (canPlayerMove(gameId, nextPlayer.getUser().getId())) {
                board.setCurrentPlayerIndex(nextPlayerIndex);
                boardRepository.save(board);
                return nextPlayer;
            }
            
            // Try the next player
            nextPlayerIndex = (nextPlayerIndex + 1) % players.size();
        } while (nextPlayerIndex != startingIndex);
        
        // If we get here, no player can move, game is over
        game.setStatus(GameStatus.FINISHED);
        gameRepository.save(game);
        
        // Return current player (no change)
        return players.get(currentPlayerIndex);
    }

    @Override
    public boolean isGameOver(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
        
        if (game.getStatus() == GameStatus.FINISHED) {
                return true;
        }
        
        List<GameUser> players = gameUserRepository.findByGameId(gameId);
        
        // Game is over if no player can make a move
        for (GameUser player : players) {
            if (canPlayerMove(gameId, player.getUser().getId())) {
                    return false;
            }
        }
        
        return true;
    }

    @Override
    @Transactional
    public Game calculateScores(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Game not found with id: " + gameId));
        
        List<GameUser> players = gameUserRepository.findByGameId(gameId);
        
        // Calculate score for each player
        for (GameUser player : players) {
            int score = calculatePlayerScore(gameId, player);
            player.setScore(score);
            gameUserRepository.save(player);
        }
        
        return game;
    }
    
    /**
     * Calculate score for a single player
     */
    private int calculatePlayerScore(Long gameId, GameUser player) {
        // Get all pieces for this player
        List<Piece> allPieces = pieceRepository.findByBoardGameIdAndUserId(gameId, player.getUser().getId());
        
        // Get unused pieces (not placed on board)
        List<Piece> unusedPieces = allPieces.stream()
                .filter(p -> p.getPosX() < 0 || p.getPosY() < 0)
                .collect(Collectors.toList());
        
        // Calculate penalty for unused pieces (sum of squares in each piece)
        int unusedSquares = 0;
        for (Piece piece : unusedPieces) {
            boolean[][] shape = piece.getShape();
            for (boolean[] row : shape) {
                for (boolean cell : row) {
                    if (cell) {
                        unusedSquares++;
                    }
                }
            }
        }
        
        // Base score: -1 for each unused square
        int score = -unusedSquares;
        
        // Bonus: +15 if all pieces are used
        if (unusedPieces.isEmpty()) {
            score += 15;
            
            // Extra bonus: +5 if the 1x1 piece was the last piece placed
            Optional<Piece> oneSquarePiece = allPieces.stream()
                    .filter(p -> p.getType() == PieceType.I1)
                    .findFirst();
            
            if (oneSquarePiece.isPresent() && oneSquarePiece.get().getPosX() >= 0) {
                score += 5;
            }
        }
        
        return score;
    }

    @Override
    public int[] getStartCorner(PlayerColor color) {
        return switch (color) {
            case BLUE -> new int[]{0, 0};            // Top-left
            case YELLOW -> new int[]{BOARD_SIZE - 1, 0}; // Top-right
            case RED -> new int[]{BOARD_SIZE - 1, BOARD_SIZE - 1}; // Bottom-right
            case GREEN -> new int[]{0, BOARD_SIZE - 1}; // Bottom-left
        };
    }
} 