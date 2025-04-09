# Blokus Game Board Implementation Plan (Java-Centric Approach)

## Overview

This document outlines the strategic implementation plan for the Blokus game board page, leveraging Java and Spring MVC for all core functionality. The implementation minimizes client-side JavaScript in favor of server-side processing, following a traditional form submission approach with Thymeleaf templating.

## 1. Game Board Structure

### 1.1 Board Components

- **Main Board**: 20x20 grid where players place their pieces
- **Player Areas**: 4 sections (one per player) to display:
  - Available pieces (21 per player)
  - Player information (name, score, time remaining if timed mode)
  - Indicators for current player's turn
- **Game Controls**: Form buttons for rotating pieces, flipping pieces, passing turn, and surrendering
- **Game Information**: Display of game status, current player, and remaining pieces per player

### 1.2 Visual Representation

- Each cell in the grid will be a 25x25px square, rendered via Thymeleaf templates
- Each player's pieces will have a distinct color (Blue, Red, Green, Yellow)
- Corner cells will be marked with the corresponding player's color
- The board will be rendered using HTML tables or CSS grid via Thymeleaf
- Server-side validation will provide visual feedback on valid/invalid positions

## 2. Player Pieces

### 2.1 Piece Set (21 pieces per player)

As shown in the reference image, each player has the following pieces:

- 1 Monomino (1 square)
- 1 Domino (2 squares)
- 2 Triominoes (L-shape and I-shape)
- 5 Tetrominoes (I, O, T, L, Z shapes)
- 12 Pentominoes (I, L, U, Z, T, X, V, W, P, F, Y, N shapes)

### 2.2 Piece Representation

- Each piece will be represented as a 2D boolean array in Java
- Piece rotations and flips will be processed server-side
- Each piece will be rendered as an HTML element with a unique identifier
- Pieces will be selectable via form controls
- Available pieces will be displayed in the player's area using Thymeleaf iteration
- Placed pieces will be displayed on the main board based on the server-side board state

## 3. Technical Implementation

### 3.1 Data Model Enhancements

- **Board Entity**:
  - Grid state stored as a serialized 2D array
  - Current player index
  - Mapping to game entity
- **Piece Entity**:
  - Shape type (enum with all 21 possible shapes)
  - Position (x, y coordinates on board)
  - Rotation (0-3 for 0°, 90°, 180°, 270°)
  - Flipped state (boolean)
  - Reference to player/user

### 3.2 Server-Side Components

- **GameLogicService**:
  - Initialize pieces for each player
  - Validate move legality (first move in corner, touching corners only)
  - Place pieces on board
  - Calculate valid placement positions
  - Check for game end conditions
  - Calculate scores
- **GamePlayController**:
  - Handle HTTP form submissions for piece placement
  - Process player turn management
  - Render board state updates via Thymeleaf
  - Manage game flow (start, end, player turns)
  - Provide pre-calculated valid positions for piece placement

### 3.3 Java-Centric Components

- **Thymeleaf Templates**:
  - Server-rendered HTML for the board grid
  - Templates for pieces with different shapes
  - Player information panels
  - Form controls for game actions
- **CSS Styling**:
  - Grid styling and cell dimensions
  - Piece styling with appropriate colors
  - State-based styling for valid/invalid placements
- **Form-Based Interaction**:
  - Simple two-step piece placement process:
    1. Select a piece via radio buttons or dropdown
    2. Select coordinates on the board via form inputs
  - Separate submit buttons for different actions (rotate, flip, place)
  - Auto-refresh or server-sent events for state updates
  - Hidden form fields for maintaining state between submissions

## 4. Interaction Flow

### 4.1 Game Initialization

1. Server creates game instance with joined players
2. Server initializes board (20x20 empty grid)
3. Server generates 21 pieces for each player
4. Server renders the initial board and player areas with pieces via Thymeleaf
5. Server determines starting player (Blue starts first)
6. Initial page render highlights current player's turn

### 4.2 Turn Execution (Server-Side Approach)

1. Active player selects a piece from their collection using radio buttons or a dropdown menu
2. Player uses form buttons to rotate or flip the selected piece (each action is a form submission)
3. After each rotation/flip, the page refreshes with updated piece preview
4. Player enters X and Y coordinates in form inputs to specify where to place the piece
5. Alternatively, the board is rendered as a grid of clickable cells (each cell is a form submit button with coordinates)
6. On submission, server validates move and updates board state if valid
7. Server re-renders the page with updated board state
8. If the move is invalid, server returns an error message displayed via Thymeleaf
9. Server advances to next player's turn
10. Process repeats until game end conditions

### 4.3 Game End

1. Game ends when no player can place any more pieces
2. Server calculates final scores:
   - Each unused square: -1 point
   - All pieces placed: +15 points
   - Last piece is monomino: +5 additional points
3. Server updates player statistics
4. Server renders the final scores and winner
5. Option to play again or return to lobby via form buttons

## 5. Technical Challenges and Solutions

### 5.1 Real-time Updates Without JavaScript

- **Challenge**: Providing timely updates without client-side scripting
- **Solution**: Implement one of these approaches:
  - Server-Sent Events (SSE) with minimal JavaScript
  - Meta refresh tags for periodic page reloading
  - HTML polling via `<meta http-equiv="refresh">` with configurable intervals
  - Optional WebSocket integration if minimal JavaScript is acceptable

### 5.2 Move Validation

- **Challenge**: Providing intuitive feedback for legal moves without client-side validation
- **Solution**:
  - Pre-calculate and render valid placement positions on the server
  - Highlight valid cells on the board using Thymeleaf conditionals and CSS
  - Return clear error messages for invalid moves
  - Use form validation for basic input sanitation

### 5.3 Piece Manipulation

- **Challenge**: Intuitive piece rotation and flipping without JavaScript
- **Solution**:
  - Implement individual form submissions for rotation and flipping actions
  - Maintain piece state in session between requests
  - Render piece preview after each transformation
  - Use hidden form fields to track piece orientation

### 5.4 Game State Management

- **Challenge**: Maintaining consistent game state across multiple users
- **Solution**:
  - Store authoritative game state in the database
  - Use HTTP session to track player-specific information
  - Implement polling or server-sent events for state updates
  - Use optimistic locking to prevent concurrent modification issues

## 6. Implementation Phases

### Phase 1: Basic Board Rendering

- Create server-side board representation
- Implement Thymeleaf templates for board rendering
- Display static player information

### Phase 2: Piece Representation

- Define piece shapes and types in Java
- Implement server-side piece transformation logic
- Create templates for piece rendering

### Phase 3: Game Mechanics

- Implement turn management
- Develop move validation logic
- Create form-based piece placement functionality

### Phase 4: State Synchronization

- Implement page refresh mechanism
- Add meta refresh tags or optional server-sent events
- Ensure consistent state across players

### Phase 5: UI Enhancements

- Improve form usability
- Add clear visual indicators for game state
- Enhance accessibility and responsiveness

### Phase 6: Testing and Refinement

- Test with multiple players
- Optimize server performance
- Fix bugs and edge cases

## 7. UI/UX Considerations for Form-Based Approach

### 7.1 Layout

- Simple, accessible form controls
- Clear visual hierarchy with focus on the game board
- Logical tab order for keyboard navigation
- Proper form labeling for accessibility

### 7.2 Feedback

- Server-generated messages for valid/invalid moves
- Visual indicators for current player's turn
- Clear highlighting of selected pieces and potential positions
- Confirmation messages for game actions

### 7.3 Controls

- Simple piece selection mechanism:
  - Radio button grid for selecting pieces (visual representation)
  - Each piece has a unique ID and is selectable via a radio button
- Straightforward placement mechanism:
  - Numeric input fields for X and Y coordinates
  - Or clickable board cells implemented as form buttons
- Clearly labeled buttons for actions (rotate, flip, place)
- Proper input validation with helpful error messages
- Confirmation for irreversible actions

### 7.4 Simplified Two-Step Placement Process

1. **Step 1: Piece Selection**

   - Player selects a piece from their available pieces
   - Pieces displayed in a grid, each with a radio button
   - Selected piece is highlighted and shown in a preview area
   - Rotation and flip buttons modify the preview

2. **Step 2: Position Selection**
   - Player specifies position using one of these methods:
     - Option A: Enter X,Y coordinates in numeric input fields
     - Option B: Click on a cell in the board (each cell is a submit button)
   - Submit button sends both the piece selection and position to server
   - Server validates and places the piece or returns an error

## 8. Testing Strategy

### 8.1 Unit Tests

- Test piece shape definitions and transformations
- Test move validation logic
- Test score calculation

### 8.2 Integration Tests

- Test form submission and processing
- Test turn progression
- Test game end conditions

### 8.3 System Tests

- Test complete game flow with form submissions
- Test multiple concurrent games
- Test browser compatibility for form-based interaction

### 8.4 User Tests

- Gather feedback on form usability
- Identify pain points in the non-JavaScript interface
- Validate game rule implementation

## 9. Technical Specifications

### 9.1 Spring MVC Components

- **Controller**: GamePlayController handles HTTP form requests
- **Service**: GameLogicService implements game rules and state management
- **Repository**: BoardRepository and PieceRepository for data persistence
- **Model**: Board and Piece entities for state representation
- **View**: Thymeleaf templates for server-side rendering

### 9.2 Server-Side Technologies

- **Java 17** for all business logic
- **Thymeleaf** for server-side templating
- **Spring MVC** for request handling
- **HTML/CSS** for presentation
- **HTTP Session** for maintaining state between requests
- **Optional**: Minimal JavaScript only if absolutely necessary for basic interactivity

## 10. Accommodating Minimal JavaScript (If Required)

If minimal JavaScript is acceptable, the following enhancements could be made:

1. **Auto-refresh**: Use JavaScript to periodically check for game updates
2. **Form submission**: Use JavaScript to submit forms without full page reload
3. **Piece selection**: Enhance piece selection with click events
4. **Visual feedback**: Provide immediate feedback on valid/invalid placements

These enhancements would be implemented as progressive enhancements, ensuring the application works even without JavaScript enabled.

## 11. Conclusion

This implementation plan provides a comprehensive roadmap for developing the Blokus game board using a Java-centric, server-side approach with minimal to no JavaScript. By leveraging Spring MVC and Thymeleaf templating, we can create an interactive game experience while maintaining a pure Java implementation.

The form-based approach may sacrifice some UI fluidity compared to a JavaScript implementation but offers advantages in terms of simplicity, accessibility, and alignment with a pure Java architecture.

## 12. Implementation Example: Simplified Piece Placement

### 12.1 Piece Selection Form (Thymeleaf Template)

```html
<form
  th:action="@{/games/{gameId}/select-piece(gameId=${game.id})}"
  method="post"
>
  <div class="pieces-container">
    <div th:each="piece : ${availablePieces}" class="piece-option">
      <input
        type="radio"
        th:id="'piece-' + ${piece.id}"
        name="pieceId"
        th:value="${piece.id}"
        th:checked="${selectedPieceId == piece.id}"
      />
      <label th:for="'piece-' + ${piece.id}" class="piece-label">
        <!-- Visual representation of the piece -->
        <div
          th:class="'piece-shape ' + ${piece.type}"
          th:attr="data-piece-id=${piece.id}"
        >
          <!-- Piece shape rendered server-side -->
        </div>
      </label>
    </div>
  </div>
  <button type="submit">Select Piece</button>
</form>

<!-- If a piece is selected, show rotation/flip controls -->
<div th:if="${selectedPiece != null}" class="piece-controls">
  <div
    class="piece-preview"
    th:attr="data-rotation=${selectedPiece.rotation}, data-flipped=${selectedPiece.flipped}"
  >
    <!-- Preview of the currently selected piece -->
  </div>

  <form
    th:action="@{/games/{gameId}/rotate-piece(gameId=${game.id})}"
    method="post"
  >
    <input type="hidden" name="pieceId" th:value="${selectedPiece.id}" />
    <button type="submit">Rotate</button>
  </form>

  <form
    th:action="@{/games/{gameId}/flip-piece(gameId=${game.id})}"
    method="post"
  >
    <input type="hidden" name="pieceId" th:value="${selectedPiece.id}" />
    <button type="submit">Flip</button>
  </form>
</div>
```

### 12.2 Board Placement Form (Two Options)

#### Option A: Coordinate Input

```html
<form
  th:if="${selectedPiece != null}"
  th:action="@{/games/{gameId}/place-piece(gameId=${game.id})}"
  method="post"
>
  <input type="hidden" name="pieceId" th:value="${selectedPiece.id}" />
  <input type="hidden" name="rotation" th:value="${selectedPiece.rotation}" />
  <input type="hidden" name="flipped" th:value="${selectedPiece.flipped}" />

  <div class="coordinate-inputs">
    <label for="posX">X Position:</label>
    <input type="number" id="posX" name="posX" min="0" max="19" required />

    <label for="posY">Y Position:</label>
    <input type="number" id="posY" name="posY" min="0" max="19" required />
  </div>

  <button type="submit">Place Piece</button>
</form>
```

#### Option B: Clickable Board Cells

```html
<div th:if="${selectedPiece != null}" class="board-placement">
  <table class="game-board">
    <tr th:each="row, rowStat : ${board.grid}">
      <td
        th:each="cell, colStat : ${row}"
        class="board-cell"
        th:classappend="${cell > 0 ? 'occupied-' + cell : 'empty'}"
      >
        <form
          th:action="@{/games/{gameId}/place-piece(gameId=${game.id})}"
          method="post"
          th:if="${cell == 0}"
        >
          <input type="hidden" name="pieceId" th:value="${selectedPiece.id}" />
          <input
            type="hidden"
            name="rotation"
            th:value="${selectedPiece.rotation}"
          />
          <input
            type="hidden"
            name="flipped"
            th:value="${selectedPiece.flipped}"
          />
          <input type="hidden" name="posX" th:value="${colStat.index}" />
          <input type="hidden" name="posY" th:value="${rowStat.index}" />
          <button
            type="submit"
            class="cell-button"
            th:classappend="${validPositions != null && validPositions.contains(colStat.index + '-' + rowStat.index) ? 'valid-position' : 'invalid-position'}"
          >
            <!-- Empty or display a small + sign -->
          </button>
        </form>
        <div
          th:if="${cell > 0}"
          class="placed-piece"
          th:attr="data-color=${cell}"
        ></div>
      </td>
    </tr>
  </table>
</div>
```

### 12.3 Controller Methods

```java
@Controller
@RequestMapping("/games/{gameId}")
public class GamePlayController {

    private final GameService gameService;
    private final GameLogicService gameLogicService;

    // Constructor with dependency injection

    @PostMapping("/select-piece")
    public String selectPiece(@PathVariable Long gameId,
                              @RequestParam Long pieceId,
                              HttpSession session) {
        session.setAttribute("selectedPieceId", pieceId);
        return "redirect:/games/" + gameId + "/play";
    }

    @PostMapping("/rotate-piece")
    public String rotatePiece(@PathVariable Long gameId,
                              @RequestParam Long pieceId,
                              HttpSession session) {
        // Get current rotation from session or default to 0
        Integer rotation = (Integer) session.getAttribute("pieceRotation_" + pieceId);
        rotation = (rotation == null) ? 0 : (rotation + 1) % 4;

        // Store rotation in session
        session.setAttribute("pieceRotation_" + pieceId, rotation);
        return "redirect:/games/" + gameId + "/play";
    }

    @PostMapping("/flip-piece")
    public String flipPiece(@PathVariable Long gameId,
                            @RequestParam Long pieceId,
                            HttpSession session) {
        // Toggle flipped state
        Boolean flipped = (Boolean) session.getAttribute("pieceFlipped_" + pieceId);
        flipped = (flipped == null) ? true : !flipped;

        // Store flipped state in session
        session.setAttribute("pieceFlipped_" + pieceId, flipped);
        return "redirect:/games/" + gameId + "/play";
    }

    @PostMapping("/place-piece")
    public String placePiece(@PathVariable Long gameId,
                             @RequestParam Long pieceId,
                             @RequestParam int posX,
                             @RequestParam int posY,
                             @RequestParam(required = false) Integer rotation,
                             @RequestParam(required = false) Boolean flipped,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        // Use session values if not provided in request
        if (rotation == null) {
            rotation = (Integer) session.getAttribute("pieceRotation_" + pieceId);
            rotation = (rotation == null) ? 0 : rotation;
        }

        if (flipped == null) {
            flipped = (Boolean) session.getAttribute("pieceFlipped_" + pieceId);
            flipped = (flipped == null) ? false : flipped;
        }

        try {
            // Validate and place the piece
            if (gameLogicService.isValidMove(gameId, pieceId, posX, posY, rotation, flipped)) {
                gameLogicService.placePiece(gameId, pieceId, posX, posY, rotation, flipped);

                // Clear selection after successful placement
                session.removeAttribute("selectedPieceId");
                session.removeAttribute("pieceRotation_" + pieceId);
                session.removeAttribute("pieceFlipped_" + pieceId);

                // Move to next player's turn
                gameLogicService.nextTurn(gameId);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid move. Please try another position.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error placing piece: " + e.getMessage());
        }

        return "redirect:/games/" + gameId + "/play";
    }

    @GetMapping("/play")
    public String showGameBoard(@PathVariable Long gameId,
                               Model model,
                               HttpSession session) {
        Game game = gameService.findById(gameId);

        if (game == null) {
            return "redirect:/games";
        }

        model.addAttribute("game", game);
        model.addAttribute("board", game.getBoard());

        // Get current user from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User currentUser = userService.findByUsername(username);

        // Get player's pieces
        List<Piece> availablePieces = gameLogicService.getAvailablePieces(gameId, currentUser.getId());
        model.addAttribute("availablePieces", availablePieces);

        // Get selected piece from session
        Long selectedPieceId = (Long) session.getAttribute("selectedPieceId");
        if (selectedPieceId != null) {
            Piece selectedPiece = availablePieces.stream()
                .filter(p -> p.getId().equals(selectedPieceId))
                .findFirst()
                .orElse(null);

            if (selectedPiece != null) {
                // Apply rotation and flip from session
                Integer rotation = (Integer) session.getAttribute("pieceRotation_" + selectedPieceId);
                Boolean flipped = (Boolean) session.getAttribute("pieceFlipped_" + selectedPieceId);

                selectedPiece.setRotation(rotation != null ? rotation : 0);
                selectedPiece.setFlipped(flipped != null ? flipped : false);

                model.addAttribute("selectedPiece", selectedPiece);

                // Calculate valid positions for this piece
                List<String> validPositions = gameLogicService.getValidPositions(gameId, selectedPieceId,
                    selectedPiece.getRotation(), selectedPiece.isFlipped());
                model.addAttribute("validPositions", validPositions);
            }
        }

        // Get any error messages
        if (model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", model.getAttribute("errorMessage"));
        }

        return "game/play";
    }
}
```

## 13. Piece Shape Implementation

### 13.1 Piece Shape Definitions

The piece shapes will be implemented according to the reference image, which shows all 21 unique Blokus pieces. Each piece will be defined in Java using a 2D array representation and rendered in the UI using HTML/CSS.

```java
public enum PieceType {
    // Monomino (1 square)
    I1(new boolean[][] {{true}}),

    // Domino (2 squares)
    I2(new boolean[][] {{true, true}}),

    // Triominoes (3 squares)
    I3(new boolean[][] {{true, true, true}}),
    L3(new boolean[][] {{true, false}, {true, true}}),

    // Tetrominoes (4 squares)
    I4(new boolean[][] {{true, true, true, true}}),
    L4(new boolean[][] {{true, false, false}, {true, true, true}}),
    T4(new boolean[][] {{true, true, true}, {false, true, false}}),
    Z4(new boolean[][] {{true, true, false}, {false, true, true}}),
    O4(new boolean[][] {{true, true}, {true, true}}),

    // Pentominoes (12 shapes with 5 squares each)
    I5(new boolean[][] {{true, true, true, true, true}}),
    L5(new boolean[][] {{true, false, false, false}, {true, true, true, true}}),
    U5(new boolean[][] {{true, false, true}, {true, true, true}}),
    Z5(new boolean[][] {{true, true, false}, {false, true, false}, {false, true, true}}),
    T5(new boolean[][] {{true, true, true}, {false, true, false}, {false, true, false}}),
    X5(new boolean[][] {{false, true, false}, {true, true, true}, {false, true, false}}),
    V5(new boolean[][] {{true, false, false}, {true, false, false}, {true, true, true}}),
    W5(new boolean[][] {{true, false, false}, {true, true, false}, {false, true, true}}),
    P5(new boolean[][] {{true, true}, {true, true}, {true, false}}),
    F5(new boolean[][] {{false, true, true}, {true, true, false}, {false, true, false}}),
    Y5(new boolean[][] {{false, true}, {true, true}, {false, true}, {false, true}}),
    N5(new boolean[][] {{true, true, false, false}, {false, true, true, true}});

    private final boolean[][] shape;

    PieceType(boolean[][] shape) {
        this.shape = shape;
    }

    public boolean[][] getShape() {
        return shape;
    }

    // Method to apply rotation and flipping
    public boolean[][] getTransformedShape(int rotation, boolean flipped) {
        boolean[][] result = this.shape;

        // Apply flip if needed
        if (flipped) {
            result = flipHorizontally(result);
        }

        // Apply rotation (0, 1, 2, 3 = 0°, 90°, 180°, 270°)
        for (int i = 0; i < rotation; i++) {
            result = rotate90Degrees(result);
        }

        return result;
    }

    // Helper method to flip a shape horizontally
    private boolean[][] flipHorizontally(boolean[][] shape) {
        int rows = shape.length;
        int cols = shape[0].length;
        boolean[][] flipped = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                flipped[r][c] = shape[r][cols - 1 - c];
            }
        }

        return flipped;
    }

    // Helper method to rotate a shape 90 degrees clockwise
    private boolean[][] rotate90Degrees(boolean[][] shape) {
        int rows = shape.length;
        int cols = shape[0].length;
        boolean[][] rotated = new boolean[cols][rows];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = shape[r][c];
            }
        }

        return rotated;
    }
}
```

### 13.2 CSS for Piece Rendering

The following CSS will be used to render the pieces in the UI:

```css
/* Base styling for all pieces */
.piece-grid {
  display: inline-grid;
  grid-gap: 1px;
}

/* Individual square styling */
.piece-cell {
  width: 20px;
  height: 20px;
  border: 1px solid rgba(0, 0, 0, 0.3);
}

/* Color classes for each player */
.piece-cell.blue {
  background-color: #0000ff;
}

.piece-cell.red {
  background-color: #ff0000;
}

.piece-cell.green {
  background-color: #00bb00;
}

.piece-cell.yellow {
  background-color: #ffff00;
}

/* Piece container */
.piece-option {
  display: inline-block;
  margin: 10px;
  cursor: pointer;
}

.piece-option:hover {
  transform: scale(1.1);
  transition: transform 0.2s;
}

/* Selected piece styling */
.piece-option input[type="radio"]:checked + .piece-label {
  outline: 3px solid #ff9900;
  box-shadow: 0 0 8px rgba(255, 153, 0, 0.7);
}

/* Piece preview area */
.piece-preview {
  margin: 20px 0;
  padding: 10px;
  border: 2px dashed #999;
  display: inline-block;
}

/* Valid and invalid positions */
.valid-position {
  background-color: rgba(0, 255, 0, 0.3);
}

.invalid-position {
  background-color: rgba(255, 0, 0, 0.2);
}

/* Cell button styling */
.cell-button {
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
}

.cell-button:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

/* Placed pieces on the board */
.placed-piece {
  width: 100%;
  height: 100%;
}

.placed-piece[data-color="1"] {
  background-color: #0000ff; /* Blue */
}

.placed-piece[data-color="2"] {
  background-color: #ff0000; /* Red */
}

.placed-piece[data-color="3"] {
  background-color: #00bb00; /* Green */
}

.placed-piece[data-color="4"] {
  background-color: #ffff00; /* Yellow */
}
```

### 13.3 Thymeleaf Fragment for Piece Rendering

Create a Thymeleaf fragment for rendering pieces that can be reused throughout the application:

```html
<!-- fragments/pieces.html -->
<div
  th:fragment="render-piece(piece, color)"
  class="piece-grid"
  th:with="shape=${piece.getTransformedShape(piece.rotation, piece.flipped)},
              rows=${shape.length},
              cols=${shape[0].length}"
  th:style="'grid-template-rows: repeat(' + ${rows} + ', 20px); grid-template-columns: repeat(' + ${cols} + ', 20px);'"
>
  <div
    th:each="row, rowStat : ${shape}"
    th:each-cell="${cell, colStat : row}"
    th:if="${cell}"
    class="piece-cell"
    th:classappend="${color}"
  ></div>
</div>
```

### 13.4 Updated Piece Selection Implementation

Using the fragment for piece selection display:

```html
<form
  th:action="@{/games/{gameId}/select-piece(gameId=${game.id})}"
  method="post"
>
  <div class="pieces-container">
    <div th:each="piece : ${availablePieces}" class="piece-option">
      <input
        type="radio"
        th:id="'piece-' + ${piece.id}"
        name="pieceId"
        th:value="${piece.id}"
        th:checked="${selectedPieceId == piece.id}"
      />
      <label th:for="'piece-' + ${piece.id}" class="piece-label">
        <!-- Use the piece rendering fragment -->
        <div
          th:replace="fragments/pieces :: render-piece(${piece}, ${gameUser.color.toString().toLowerCase()})"
        ></div>
      </label>
    </div>
  </div>
  <button type="submit">Select Piece</button>
</form>
```

### 13.5 Piece Preview with Rotation and Flip

Using the fragment for the piece preview:

```html
<div th:if="${selectedPiece != null}" class="piece-controls">
  <div class="piece-preview">
    <!-- Use the piece rendering fragment for the preview -->
    <div
      th:replace="fragments/pieces :: render-piece(${selectedPiece}, ${gameUser.color.toString().toLowerCase()})"
    ></div>
  </div>

  <div class="piece-actions">
    <form
      th:action="@{/games/{gameId}/rotate-piece(gameId=${game.id})}"
      method="post"
    >
      <input type="hidden" name="pieceId" th:value="${selectedPiece.id}" />
      <button type="submit">Rotate</button>
    </form>

    <form
      th:action="@{/games/{gameId}/flip-piece(gameId=${game.id})}"
      method="post"
    >
      <input type="hidden" name="pieceId" th:value="${selectedPiece.id}" />
      <button type="submit">Flip</button>
    </form>
  </div>
</div>
```

### 13.6 Java Service Method to Get Piece Shapes

In the `GameLogicService`, implement a method to calculate all valid positions for a piece:

```java
/**
 * Get all valid positions for a piece on the board
 *
 * @param gameId The game ID
 * @param pieceId The piece ID
 * @param rotation The rotation (0-3)
 * @param flipped Whether the piece is flipped
 * @return List of strings in format "x-y" representing valid positions
 */
List<String> getValidPositions(Long gameId, Long pieceId, int rotation, boolean flipped) {
    Game game = gameRepository.findById(gameId).orElseThrow();
    Board board = game.getBoard();
    Piece piece = pieceRepository.findById(pieceId).orElseThrow();

    // Get transformed shape based on rotation and flip
    boolean[][] shape = piece.getType().getTransformedShape(rotation, flipped);

    List<String> validPositions = new ArrayList<>();

    // For each position on the board
    for (int y = 0; y < 20; y++) {
        for (int x = 0; x < 20; x++) {
            // Check if piece can be placed at this position
            if (isValidMove(gameId, pieceId, x, y, rotation, flipped)) {
                validPositions.add(x + "-" + y);
            }
        }
    }

    return validPositions;
}
```

### 13.7 Complete Rendering of All 21 Blokus Pieces

The pieces will be rendered in a grid layout matching the reference image, with numbers identifying each piece:

```html
<div class="all-pieces-display">
  <!-- Row 1: Monomino (1) -->
  <div class="piece-row">
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('I1')}, 'red')"
      ></div>
      <div class="piece-number">1</div>
    </div>
    <div class="pieces-count">1</div>
  </div>

  <!-- Row 2: Domino (2) -->
  <div class="piece-row">
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('I2')}, 'red')"
      ></div>
      <div class="piece-number">2</div>
    </div>
    <div class="pieces-count">1</div>
  </div>

  <!-- Row 3: Triominoes (3-4) -->
  <div class="piece-row">
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('I3')}, 'red')"
      ></div>
      <div class="piece-number">3</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('L3')}, 'red')"
      ></div>
      <div class="piece-number">4</div>
    </div>
    <div class="pieces-count">2</div>
  </div>

  <!-- Row 4: Tetrominoes (5-9) -->
  <div class="piece-row">
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('I4')}, 'red')"
      ></div>
      <div class="piece-number">5</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('L4')}, 'red')"
      ></div>
      <div class="piece-number">6</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('T4')}, 'red')"
      ></div>
      <div class="piece-number">7</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('Z4')}, 'red')"
      ></div>
      <div class="piece-number">8</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('O4')}, 'red')"
      ></div>
      <div class="piece-number">9</div>
    </div>
    <div class="pieces-count">5</div>
  </div>

  <!-- Row 5: Pentominoes (10-21) -->
  <div class="piece-row">
    <!-- First set (10-16) -->
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('I5')}, 'red')"
      ></div>
      <div class="piece-number">10</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('L5')}, 'red')"
      ></div>
      <div class="piece-number">11</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('U5')}, 'red')"
      ></div>
      <div class="piece-number">12</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('Z5')}, 'red')"
      ></div>
      <div class="piece-number">13</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('T5')}, 'red')"
      ></div>
      <div class="piece-number">14</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('X5')}, 'red')"
      ></div>
      <div class="piece-number">15</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('V5')}, 'red')"
      ></div>
      <div class="piece-number">16</div>
    </div>
  </div>

  <!-- Second set (17-21) -->
  <div class="piece-row">
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('W5')}, 'red')"
      ></div>
      <div class="piece-number">17</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('P5')}, 'red')"
      ></div>
      <div class="piece-number">18</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('F5')}, 'red')"
      ></div>
      <div class="piece-number">19</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('Y5')}, 'red')"
      ></div>
      <div class="piece-number">20</div>
    </div>
    <div class="piece-with-label">
      <div
        th:replace="fragments/pieces :: render-piece(${pieceTypes.get('N5')}, 'red')"
      ></div>
      <div class="piece-number">21</div>
    </div>
    <div class="pieces-count">12</div>
  </div>
</div>
```

### 13.8 Additional CSS for Piece Display

```css
/* Piece display grid styling */
.all-pieces-display {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 20px 0;
  padding: 10px;
  background-color: #f0f0f0;
  border-radius: 8px;
}

.piece-row {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px;
  background-color: #e0e0e0;
  border-radius: 4px;
}

.piece-with-label {
  position: relative;
  display: inline-block;
  margin-right: 15px;
}

.piece-number {
  position: absolute;
  bottom: -5px;
  right: -5px;
  background-color: #333;
  color: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  font-size: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.pieces-count {
  margin-left: auto;
  font-size: 20px;
  font-weight: bold;
  background-color: #ccc;
  color: #333;
  padding: 5px 12px;
  border-radius: 4px;
}
```

### 13.9 Controller Enhancement to Provide All Piece Types

Enhance the controller to provide all piece types for rendering:

```java
@GetMapping("/play")
public String showGameBoard(@PathVariable Long gameId,
                           Model model,
                           HttpSession session) {
    // Existing code...

    // Add all piece types to the model for reference display
    Map<String, PieceType> pieceTypes = new HashMap<>();
    for (PieceType type : PieceType.values()) {
        pieceTypes.put(type.name(), type);
    }
    model.addAttribute("pieceTypes", pieceTypes);

    // Rest of the existing code...

    return "game/play";
}
```

This implementation provides a complete solution for rendering and interacting with the 21 unique Blokus piece shapes as shown in the reference image. The pieces are defined as 2D arrays that accurately represent their shapes, and the rendering system uses Thymeleaf to create a visual representation that matches the image.
