package com.blokus.blokus.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.blokus.blokus.dto.GameCreateDto;
import com.blokus.blokus.model.Game;
import com.blokus.blokus.model.GameUser;
import com.blokus.blokus.model.User;
import com.blokus.blokus.repository.GameUserRepository;
import com.blokus.blokus.service.GameService;
import com.blokus.blokus.service.UserService;

import jakarta.validation.Valid;

/**
 * Contrôleur pour la gestion des parties
 */
@Controller
@RequestMapping("/games")
public class GameController {
    
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    
    private final GameService gameService;
    private final UserService userService;
    private final GameUserRepository gameUserRepository;
    
    public GameController(GameService gameService, UserService userService,
            GameUserRepository gameUserRepository) {
        this.gameService = gameService;
        this.userService = userService;
        this.gameUserRepository = gameUserRepository;
    }
    
    /**
     * Affiche la page de liste des parties
     */
    @GetMapping
    public String listGames(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {
        // Get current authenticated user
        User user = userService.findByUsername(userDetails.getUsername());
        
        // Get available games and user's games
        List<Game> availableGames = gameService.findAvailableGames();
        List<Game> userGames = gameService.findUserGames(user.getId());
        
        logger.info("Utilisateur {} a consulté la liste des parties", user.getUsername());
        
        model.addAttribute("availableGames", availableGames);
        model.addAttribute("userGames", userGames);
        model.addAttribute("user", user);
        
        return "game/list";
    }
    
    /**
     * Affiche le formulaire de création de partie
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("gameCreateDto", new GameCreateDto());
        return "game/create";
    }
    
    /**
     * Crée une nouvelle partie
     */
    @PostMapping("/create")
    public String createGame(@Valid @ModelAttribute("gameCreateDto") GameCreateDto gameCreateDto,
            BindingResult bindingResult, Model model,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "game/create";
        }
        
        try {
            // Get current authenticated user
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Create the game
            Game game = gameService.createGame(gameCreateDto, user);
            
            redirectAttributes.addFlashAttribute("successMessage", "Partie créée avec succès!");
            return "redirect:/games/" + game.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors de la création de la partie: " + e.getMessage());
            return "game/create";
        }
    }
    
    /**
     * Affiche la page de détail d'une partie
     */
    @GetMapping("/{id}")
    public String gameDetail(@PathVariable Long id, Model model,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            RedirectAttributes redirectAttributes) {
        
        // Get current authenticated user
        User user = userService.findByUsername(userDetails.getUsername());
        
        try {
            // Get the game
            Game game = gameService.findById(id);
            List<GameUser> participants = gameUserRepository.findByGameId(id);
            
            // Check if the user is already in the game
            boolean isInGame = participants.stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()));
            
            // Check if user is creator (first player)
            boolean isCreator = !participants.isEmpty() 
                    && participants.get(0).getUser() != null 
                    && participants.get(0).getUser().getId().equals(user.getId());
            
            model.addAttribute("game", game);
            model.addAttribute("participants", participants);
            model.addAttribute("user", user);
            model.addAttribute("isInGame", isInGame);
            model.addAttribute("isCreator", isCreator);
            
            return "game/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur: " + e.getMessage());
            return "redirect:/games";
        }
    }
    
    /**
     * Permet à un utilisateur de rejoindre une partie
     */
    @PostMapping("/{id}/join")
    public String joinGame(@PathVariable Long id, 
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Get current authenticated user
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Join the game
            gameService.joinGame(id, user);
            
            redirectAttributes.addFlashAttribute("successMessage", "Vous avez rejoint la partie!");
            return "redirect:/games/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur: " + e.getMessage());
            return "redirect:/games/" + id;
        }
    }
    
    /**
     * Permet à un utilisateur de quitter une partie
     */
    @PostMapping("/{id}/leave")
    public String leaveGame(@PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Récupérer l'utilisateur authentifié
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Quitter la partie
            Game game = gameService.leaveGame(id, user);
            
            if (game == null) {
                // La partie a été supprimée (créateur qui quitte)
                redirectAttributes.addFlashAttribute("successMessage", "La partie a été supprimée.");
                return "redirect:/games";
            } else {
                // L'utilisateur a quitté la partie
                redirectAttributes.addFlashAttribute("successMessage", "Vous avez quitté la partie.");
                return "redirect:/games";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur: " + e.getMessage());
            return "redirect:/games/" + id;
        }
    }
    
    /**
     * Permet d'ajouter un bot à une partie
     */
    @PostMapping("/{id}/add-bot")
    public String addBotToGame(@PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Récupérer l'utilisateur authentifié
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Vérifier si l'utilisateur est le créateur
            List<GameUser> participants = gameService.getGameParticipants(id);
            
            if (participants.isEmpty() || 
                participants.get(0).getUser() == null || 
                !participants.get(0).getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Seul le créateur peut ajouter un bot.");
                return "redirect:/games/" + id;
            }
            
            // Ajouter un bot
            gameService.addBotToGame(id);
            
            logger.info("Utilisateur {} a ajouté un bot à la partie {}", user.getUsername(), id);
            
            redirectAttributes.addFlashAttribute("successMessage", "Bot ajouté avec succès!");
            return "redirect:/games/" + id;
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout d'un bot à la partie {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur: " + e.getMessage());
            return "redirect:/games/" + id;
        }
    }
    
    /**
     * Permet au créateur d'annuler une partie
     */
    @PostMapping("/{id}/cancel")
    public String cancelGame(@PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Récupérer l'utilisateur authentifié
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Annuler la partie
            gameService.cancelGame(id, user);
            
            logger.info("Utilisateur {} a annulé la partie {}", user.getUsername(), id);
            
            redirectAttributes.addFlashAttribute("successMessage", "La partie a été annulée.");
            return "redirect:/games";
        } catch (Exception e) {
            logger.error("Erreur lors de l'annulation de la partie {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur: " + e.getMessage());
            return "redirect:/games/" + id;
        }
    }
    
    /**
     * Permet au créateur de démarrer manuellement une partie
     */
    @PostMapping("/{id}/start")
    public String startGame(@PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Récupérer l'utilisateur authentifié
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Récupérer la partie et vérifier son état
            Game game = gameService.findById(id);
            if (game.getStatus() != Game.GameStatus.WAITING) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cette partie ne peut pas être démarrée car elle n'est pas en attente.");
                return "redirect:/games/" + id;
            }
            
            List<GameUser> participants = gameService.getGameParticipants(id);
            
            if (participants.isEmpty() || 
                participants.get(0).getUser() == null || 
                !participants.get(0).getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Seul le créateur peut démarrer la partie.");
                return "redirect:/games/" + id;
            }
            
            // Vérifier qu'il y a au moins 2 joueurs
            if (participants.size() < 2) {
                redirectAttributes.addFlashAttribute("errorMessage", "Il faut au moins 2 joueurs pour démarrer une partie.");
                return "redirect:/games/" + id;
            }
            
            // Démarrer la partie
            gameService.startGame(id);
            
            redirectAttributes.addFlashAttribute("successMessage", "La partie a été démarrée!");
            return "redirect:/games/" + id + "/gameplay";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur: " + e.getMessage());
            return "redirect:/games/" + id;
        }
    }
    
    /**
     * Affiche l'interface de jeu
     */
    @GetMapping("/{id}/play")
    public String playGame(@PathVariable Long id, Model model,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Récupérer l'utilisateur authentifié
            User user = userService.findByUsername(userDetails.getUsername());
            
            // Récupérer la partie
            Game game = gameService.findById(id);
            
            // Vérifier que la partie est en cours
            if (game.getStatus() != Game.GameStatus.PLAYING) {
                return "redirect:/games/" + id;
            }
            
            // Vérifier que l'utilisateur participe à la partie
            List<GameUser> participants = gameService.getGameParticipants(id);
            boolean isParticipant = participants.stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()));
            
            if (!isParticipant) {
                return "redirect:/games/" + id;
            }
            
            // Rediriger vers la nouvelle URL pour éviter le conflit
            return "redirect:/games/" + id + "/gameplay";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur: " + e.getMessage());
            return "redirect:/games";
        }
    }
} 