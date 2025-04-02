package com.blokus.blokus.controller;

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
import com.blokus.blokus.service.GameService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Show list of available games
     */
    @GetMapping
    public String listGames(Model model) {
        model.addAttribute("availableGames", gameService.findAvailableGames());
        model.addAttribute("myGames", gameService.findMyGames());
        return "game/list";
    }

    /**
     * Show form to create a new game
     */
    @GetMapping("/create")
    public String createGameForm(Model model) {
        model.addAttribute("gameCreateDto", new GameCreateDto());
        model.addAttribute("gameModes", Game.GameMode.values());
        return "game/create";
    }

    /**
     * Process game creation form
     */
    @PostMapping("/create")
    public String createGame(@Valid @ModelAttribute("gameCreateDto") GameCreateDto gameDto,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "game/create";
        }
        
        Game game = gameService.createGame(gameDto);
        redirectAttributes.addFlashAttribute("success", "Game created successfully");
        return "redirect:/games/" + game.getId();
    }

    /**
     * Show detail of a specific game
     */
    @GetMapping("/{id}")
    public String gameDetail(@PathVariable Long id, Model model) {
        Game game = gameService.findById(id);
        model.addAttribute("game", game);
        return "game/detail";
    }

    /**
     * Join a game
     */
    @PostMapping("/{id}/join")
    public String joinGame(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Game game = gameService.joinGame(id);
            redirectAttributes.addFlashAttribute("success", "You have joined the game successfully");
            
            // If the game is now in PLAYING status, redirect to play page
            if (game.getStatus() == Game.GameStatus.PLAYING) {
                return "redirect:/games/" + id + "/play";
            }
            
            return "redirect:/games/" + id;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/games";
        }
    }

    /**
     * Start a game manually
     */
    @PostMapping("/{id}/start")
    public String startGame(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            gameService.startGame(id);
            redirectAttributes.addFlashAttribute("success", "Game started successfully");
            return "redirect:/games/" + id + "/play";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/games/" + id;
        }
    }

    /**
     * Play game page - the actual game board
     */
    @GetMapping("/{id}/play")
    public String playGame(@PathVariable Long id, Model model) {
        Game game = gameService.findById(id);
        
        // Only allow access to the play page if the game is in PLAYING status
        if (game.getStatus() != Game.GameStatus.PLAYING) {
            return "redirect:/games/" + id;
        }
        
        model.addAttribute("game", game);
        return "game/play";
    }
} 