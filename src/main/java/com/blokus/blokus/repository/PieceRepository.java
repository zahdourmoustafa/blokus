package com.blokus.blokus.repository;

import com.blokus.blokus.model.Piece;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PieceRepository extends JpaRepository<Piece, Long> {
    List<Piece> findByBoardId(Long boardId);
    List<Piece> findByUserId(Long userId);
    List<Piece> findByBoardIdAndUserId(Long boardId, Long userId);
    
    /**
     * Find all pieces for a specific game and user
     */
    @Query("SELECT p FROM Piece p WHERE p.board.game.id = ?1 AND p.user.id = ?2")
    List<Piece> findByBoardGameIdAndUserId(Long gameId, Long userId);
    
    /**
     * Find all pieces for a specific game and user that have not been placed
     * (with posX less than the specified value, typically 0 for unplaced pieces)
     */
    @Query("SELECT p FROM Piece p WHERE p.board.game.id = ?1 AND p.user.id = ?2 AND p.posX < ?3")
    List<Piece> findByBoardGameIdAndUserIdAndPosXLessThan(Long gameId, Long userId, int posX);
} 