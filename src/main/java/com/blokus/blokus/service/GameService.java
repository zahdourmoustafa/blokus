package com.blokus.blokus.service;

import java.util.List;

import com.blokus.blokus.dto.GameCreateDto;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.User;

/**
 * Service pour les opérations de gestion des parties
 */
public interface GameService {
    
    /**
     * Crée une nouvelle partie avec les paramètres donnés
     * 
     * @param gameDto Les paramètres de création de la partie
     * @param creator L'utilisateur créant la partie
     * @return La partie créée
     */
    Game createGame(GameCreateDto gameDto, User creator);
    
    /**
     * Trouve une partie par son ID
     * 
     * @param id L'ID de la partie
     * @return La partie si trouvée, null sinon
     */
    Game findById(Long id);
    
    /**
     * Trouve toutes les parties disponibles (en état WAITING)
     * 
     * @return Liste des parties disponibles
     */
    List<Game> findAvailableGames();
    
    /**
     * Ajoute un utilisateur à une partie
     * 
     * @param gameId L'ID de la partie
     * @param user L'utilisateur à ajouter
     * @return La partie mise à jour
     */
    Game joinGame(Long gameId, User user);
    
    /**
     * Trouve les parties auxquelles un utilisateur participe
     * 
     * @param userId L'ID de l'utilisateur
     * @return Liste des parties de l'utilisateur
     */
    List<Game> findUserGames(Long userId);
    
    /**
     * Vérifie si une partie est prête à démarrer (assez de joueurs)
     * 
     * @param gameId L'ID de la partie
     * @return true si la partie peut démarrer, false sinon
     */
    boolean isGameReadyToStart(Long gameId);
    
    /**
     * Démarre une partie en état WAITING
     * 
     * @param gameId L'ID de la partie
     * @return La partie démarrée
     */
    Game startGame(Long gameId);
    
    /**
     * Permet à un utilisateur de quitter une partie
     * 
     * @param gameId L'ID de la partie
     * @param user L'utilisateur qui quitte
     * @return La partie mise à jour, ou null si la partie est supprimée
     */
    Game leaveGame(Long gameId, User user);
    
    /**
     * Récupère la liste des participants à une partie
     * 
     * @param gameId L'ID de la partie
     * @return Liste des participants
     */
    List<GameUser> getGameParticipants(Long gameId);
    
    /**
     * Ajoute un bot à une partie
     * 
     * @param gameId L'ID de la partie
     * @return La partie mise à jour
     */
    Game addBotToGame(Long gameId);
    
    /**
     * Annule une partie (par le créateur uniquement)
     * 
     * @param gameId L'ID de la partie
     * @param user L'utilisateur qui annule (doit être le créateur)
     * @return La partie annulée, ou null si supprimée
     */
    Game cancelGame(Long gameId, User user);
} 