package com.blokus.blokus.controller;

// Removed Map, List, HashMap, Collectors imports if no longer needed after method removal
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Removed unused service imports
// import com.blokus.blokus.service.GameLogicService;
// import com.blokus.blokus.service.GameService;
// import com.blokus.blokus.service.UserService;

// Removed Valid import if MoveDto is no longer used
// import jakarta.validation.Valid;

/**
 * REST controller for game state.
 * Endpoints related to the removed board/piece implementation have been deleted.
 */
@RestController
@RequestMapping("/api/games")
public class GameStateRestController {

    // Removed unused service fields
    // private final GameService gameService;
    // private final GameLogicService gameLogicService;
    // private final UserService userService;

    // Removed constructor as there are no fields to initialize
    // public GameStateRestController(GameService gameService, GameLogicService gameLogicService,
    //                               UserService userService) {
    //     this.gameService = gameService;
    //     this.gameLogicService = gameLogicService;
    //     this.userService = userService;
    // }

    // Methods removed previously: 
    // getGameState, getAvailablePieces, makeMove, initializeGame, checkMove
    
    // Keep the controller structure in case other game state related REST endpoints
    // (not tied to the removed board) are needed later.

} 