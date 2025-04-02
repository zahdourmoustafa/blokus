package com.blokus.blokus.repository;

import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.Game.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByStatus(GameStatus status);
    
    @Query("SELECT g FROM Game g WHERE g.status = com.blokus.blokus.model.Game.GameStatus.WAITING")
    List<Game> findAvailableGames();
    
    @Query("SELECT g FROM Game g JOIN g.players p WHERE p.user.id = :userId")
    List<Game> findGamesByUserId(Long userId);
} 