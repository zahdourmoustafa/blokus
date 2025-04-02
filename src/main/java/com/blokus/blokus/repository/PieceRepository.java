package com.blokus.blokus.repository;

import com.blokus.blokus.model.Piece;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PieceRepository extends JpaRepository<Piece, Long> {
    List<Piece> findByBoardId(Long boardId);
    List<Piece> findByUserId(Long userId);
    List<Piece> findByBoardIdAndUserId(Long boardId, Long userId);
} 