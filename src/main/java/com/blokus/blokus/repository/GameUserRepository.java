package com.blokus.blokus.repository;

import com.blokus.blokus.model.GameUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameUserRepository extends JpaRepository<GameUser, Long> {
    List<GameUser> findByGameId(Long gameId);
    List<GameUser> findByUserId(Long userId);
    Optional<GameUser> findByGameIdAndUserId(Long gameId, Long userId);
    int countByGameId(Long gameId);
    
    /**
     * Find all players in a game ordered by their ID (for turn order)
     */
    List<GameUser> findByGameIdOrderById(Long gameId);
} 