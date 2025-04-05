package com.blokus.blokus.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blokus.blokus.model.Board;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.Game.GameStatus;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.Piece;
import com.blokus.blokus.service.BotService;
import com.blokus.blokus.service.GameLogicService;
import com.blokus.blokus.service.GameService;

/**
 * Implementation of bot service for AI moves
 */
@Service
public class BotServiceImpl implements BotService {

    private static final Logger logger = LoggerFactory.getLogger(BotServiceImpl.class);
    private static final Random random = new Random();
    
    private final GameLogicService gameLogicService;
    private final GameService gameService;
    
    public BotServiceImpl(GameLogicService gameLogicService, GameService gameService) {
        this.gameLogicService = gameLogicService;
        this.gameService = gameService;
    }

    @Override
    @Transactional
    public boolean makeBotMove(Long gameId, GameUser bot) {
        logger.info("Bot {} (color: {}) is making a move in game {}", 
                bot.getId(), bot.getColor(), gameId);
        
        // Get available pieces for the bot
        List<Piece> availablePieces = gameLogicService.getAvailablePieces(gameId, bot.getUser().getId());
        
        if (availablePieces.isEmpty()) {
            logger.info("Bot {} has no available pieces, skipping turn", bot.getId());
            gameLogicService.nextTurn(gameId);
            return false;
        }
        
        // Sort pieces by size (prefer larger pieces first for better strategy)
        Collections.sort(availablePieces, (p1, p2) -> {
            // Count cells in each piece (number of true values)
            int size1 = countCells(p1.getShape());
            int size2 = countCells(p2.getShape());
            // Larger pieces first (descending)
            return Integer.compare(size2, size1);
        });
        
        // Game board data
        Board board = gameService.findById(gameId).getBoard();
        int boardSize = board.getGrid().length;
        
        // Find all valid moves for all pieces
        List<BotMove> validMoves = new ArrayList<>();
        
        for (Piece piece : availablePieces) {
            // Try all positions
            for (int y = 0; y < boardSize; y++) {
                for (int x = 0; x < boardSize; x++) {
                    // Try all rotations
                    for (int rotation = 0; rotation < 4; rotation++) {
                        // Try both flip states
                        for (boolean flipped : new boolean[]{false, true}) {
                            // Check if move is valid
                            if (gameLogicService.isValidMove(gameId, piece.getId(), x, y, rotation, flipped)) {
                                // Calculate a score for this move (based on distance to center)
                                int score = calculateMoveScore(piece, x, y, boardSize);
                                
                                validMoves.add(new BotMove(piece.getId(), x, y, rotation, flipped, score));
                            }
                        }
                    }
                }
            }
        }
        
        // If no valid moves, skip turn
        if (validMoves.isEmpty()) {
            logger.info("Bot {} has no valid moves, skipping turn", bot.getId());
            gameLogicService.nextTurn(gameId);
            return false;
        }
        
        // Sort moves by score (higher first)
        Collections.sort(validMoves, Comparator.comparing(BotMove::getScore).reversed());
        
        // Choose one of the top moves with some randomness
        int topMoveCount = Math.min(5, validMoves.size());
        int moveIndex = random.nextInt(topMoveCount);
        BotMove selectedMove = validMoves.get(moveIndex);
        
        // Make the move
        logger.info("Bot {} is placing piece {} at position ({},{}) with rotation {} and flipped={}",
                bot.getId(), selectedMove.pieceId, selectedMove.x, selectedMove.y, 
                selectedMove.rotation, selectedMove.flipped);
        
        gameLogicService.placePiece(
                gameId, 
                selectedMove.pieceId, 
                selectedMove.x, 
                selectedMove.y, 
                selectedMove.rotation, 
                selectedMove.flipped
        );
        
        return true;
    }

    /**
     * Calculate a score for a potential move
     * 
     * @param piece The piece to place
     * @param x X coordinate
     * @param y Y coordinate
     * @param boardSize Size of the board
     * @return A score value (higher is better)
     */
    private int calculateMoveScore(Piece piece, int x, int y, int boardSize) {
        // Calculate center distance - prefer moves closer to corners initially
        // and moves toward center in mid-to-late game
        int center = boardSize / 2;
        int distanceFromCenter = Math.abs(x - center) + Math.abs(y - center);
        
        // Count cells in the piece
        boolean[][] shape = piece.getShape();
        int pieceSize = countCells(shape);
        
        // For now, a simple scoring based on piece size and position
        // Larger pieces get higher scores
        // Positions near corners initially get higher scores
        return pieceSize * 10 + distanceFromCenter;
    }
    
    /**
     * Count the number of cells (true values) in a piece shape
     */
    private int countCells(boolean[][] shape) {
        int count = 0;
        for (boolean[] row : shape) {
            for (boolean cell : row) {
                if (cell) count++;
            }
        }
        return count;
    }

    @Override
    @Transactional
    public boolean handleBotTurn(Long gameId) {
        Game game = gameService.findById(gameId);
        
        // Check if game is in progress
        if (game.getStatus() != GameStatus.PLAYING) {
            return false;
        }
        
        // Get current player
        GameUser currentPlayer = gameLogicService.getCurrentPlayer(gameId);
        
        // Check if current player is a bot
        if (currentPlayer.isBot()) {
            // Make the bot move
            return makeBotMove(gameId, currentPlayer);
        }
        
        return false;
    }

    @Override
    @Transactional
    public int processBotRound(Game game) {
        int movesPerformed = 0;
        
        // Check if game is in progress
        if (game.getStatus() != GameStatus.PLAYING) {
            return movesPerformed;
        }
        
        // Process bot moves until it's a human player's turn
        boolean botTurn = true;
        
        while (botTurn && game.getStatus() == GameStatus.PLAYING) {
            GameUser currentPlayer = gameLogicService.getCurrentPlayer(game.getId());
            
            if (currentPlayer.isBot()) {
                // Make a bot move
                boolean moveSuccessful = makeBotMove(game.getId(), currentPlayer);
                
                if (moveSuccessful) {
                    movesPerformed++;
                }
                
                // Check if game is now over
                if (gameLogicService.isGameOver(game.getId())) {
                    game.setStatus(GameStatus.FINISHED);
                    gameLogicService.calculateScores(game.getId());
                    logger.info("Game {} is over after bot moves", game.getId());
                    return movesPerformed;
                }
            } else {
                // It's a human player's turn, stop the bot round
                botTurn = false;
            }
        }
        
        return movesPerformed;
    }
    
    /**
     * Internal class to represent a potential bot move with a score
     */
    private static class BotMove {
        private final Long pieceId;
        private final int x;
        private final int y;
        private final int rotation;
        private final boolean flipped;
        private final int score;
        
        public BotMove(Long pieceId, int x, int y, int rotation, boolean flipped, int score) {
            this.pieceId = pieceId;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.flipped = flipped;
            this.score = score;
        }
        
        public int getScore() {
            return score;
        }
    }
} 