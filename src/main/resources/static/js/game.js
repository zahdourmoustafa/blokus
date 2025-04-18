/**
 * Blokus Game Client-side Logic
 */

// ==========================================
// GLOBAL STATE AND INITIALIZATION
// ==========================================
const GameState = {
  selectedPiece: null,
  selectedRotation: 0,
  selectedFlipped: false,
  selectedColor: null,
  usedPieceIds: new Set(),
  usedPieceByColor: {
    blue: new Set(),
    yellow: new Set(),
    red: new Set(),
    green: new Set(),
  },
  piecePreview: null,
  stompClient: null,
  gameId: null,
  currentUsername: null,
  board: Array(20)
    .fill()
    .map(() => Array(20).fill(null)),
};

// Initialize game when DOM is loaded
document.addEventListener("DOMContentLoaded", function () {
  initializeGame();
});

/**
 * Initialize all game components
 */
function initializeGame() {
  // Get game ID from URL
  const path = window.location.pathname;
  const pathParts = path.split("/");
  const gameIdIndex = pathParts.indexOf("games") + 1;

  if (gameIdIndex > 0 && gameIdIndex < pathParts.length) {
    GameState.gameId = pathParts[gameIdIndex];
  }

  // Initialize current username
  const currentUserElement = document.querySelector(".current-user");
  if (currentUserElement) {
    GameState.currentUsername =
      currentUserElement.getAttribute("data-username");
  }

  // If that didn't work, check player areas for selectable pieces
  if (!GameState.currentUsername) {
    const playerInfoElements = document.querySelectorAll(".player-info");
    playerInfoElements.forEach((element) => {
      const selectablePieces = element.parentElement.querySelectorAll(
        ".game-piece.selectable"
      );
      if (selectablePieces.length > 0) {
        GameState.currentUsername = element.textContent.trim();
      }
    });
  }

  // Initialize game components
  initializePieceSelection();
  initializeBoardHover();
  initializeWebSocket();
  initializeCurrentPlayerDisplay();
  initializePieceControls();

  // Force refresh to hide all used pieces
  hideAllUsedPieces();

  // Refresh the game state
  setTimeout(refreshGameState, 500);
}

// ==========================================
// PIECE SELECTION AND BOARD INTERACTION
// ==========================================
/**
 * Initialize piece selection functionality
 */
function initializePieceSelection() {
  // Use event delegation for piece selection
  document.addEventListener("click", function (event) {
    // Find closest .game-piece parent if the event target is inside a piece
    const piece = event.target.closest(".game-piece.selectable");

    if (piece) {
      // Remove selected class from all pieces and add to clicked piece
      document
        .querySelectorAll(".game-piece.selected")
        .forEach((p) => p.classList.remove("selected"));
      piece.classList.add("selected");

      // Store selected piece
      GameState.selectedPiece = piece;

      // Reset rotation for new selection
      GameState.selectedRotation = 0;
      // Also reset the visual rotation
      const container = piece.querySelector(".piece-container");
      if (container) {
        container.style.transform = `rotate(0deg)`;
      }

      // Get piece data
      const pieceId = piece.getAttribute("data-piece-id");
      const pieceColor = piece.getAttribute("data-piece-color");

      // Set form values if they exist
      const selectedPieceIdElement = document.getElementById("selectedPieceId");
      const selectedPieceColorElement =
        document.getElementById("selectedPieceColor");

      if (selectedPieceIdElement) selectedPieceIdElement.value = pieceId;
      if (selectedPieceColorElement)
        selectedPieceColorElement.value = pieceColor;

      // Show placement instructions
      showInstructions("Click on the board to place your piece");
    }
  });
}

/**
 * Initialize board hover effect for placement preview
 */
function initializeBoardHover() {
  const boardCells = document.querySelectorAll(".board-cell");

  boardCells.forEach((cell) => {
    // Show preview when mouse enters a cell
    cell.addEventListener("mouseenter", function () {
      if (!GameState.selectedPiece) return;
      showPlacementPreview(this);
    });

    // Remove preview when mouse leaves a cell
    cell.addEventListener("mouseleave", function () {
      removePlacementPreview();
    });

    // Add click handler for piece placement
    cell.addEventListener("click", function () {
      if (!GameState.selectedPiece) return;

      const x = parseInt(this.getAttribute("data-x"));
      const y = parseInt(this.getAttribute("data-y"));

      // Check if placement is valid before attempting to place
      if (isValidPlacement(x, y)) {
        placePiece(x, y);
      } else {
        showMessage(
          "Invalid placement. Pieces of the same color must touch at corners but not edges."
        );
      }
    });
  });
}

// ==========================================
// PIECE ORIENTATION AND VALIDATION
// ==========================================
/**
 * Fix the piece shape orientation to match the server's expected format
 */
function correctPieceOrientation(shape) {
  const height = shape.length;
  const width = shape[0] ? shape[0].length : 0;

  // Create a transposed matrix
  const correctedShape = Array(width)
    .fill()
    .map(() => Array(height).fill(false));

  // Fill with transposed values
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      correctedShape[x][y] = shape[y][x];
    }
  }

  return correctedShape;
}

/**
 * Utility: Rotate a 2D array shape by 0, 90, 180, or 270 degrees clockwise
 */
function rotateShape(shape, rotation) {
  let result = shape;
  const times = (((rotation % 360) + 360) % 360) / 90;
  for (let i = 0; i < times; i++) {
    // Rotate 90 degrees clockwise
    const rows = result.length;
    const cols = result[0].length;
    const rotated = Array.from({ length: cols }, () => Array(rows).fill(false));
    for (let r = 0; r < rows; r++) {
      for (let c = 0; c < cols; c++) {
        rotated[c][rows - 1 - r] = result[r][c];
      }
    }
    result = rotated;
  }
  return result;
}

/**
 * Patch: Get the current piece shape, rotated according to GameState.selectedRotation
 */
function getCurrentPieceShape() {
  if (!GameState.selectedPiece) return null;
  const pieceColor = GameState.selectedPiece.getAttribute("data-piece-color");
  const shape = [];
  const originalRows = GameState.selectedPiece.querySelectorAll(".piece-row");
  originalRows.forEach((row) => {
    const rowCells = [];
    row.querySelectorAll(".piece-cell").forEach((cell) => {
      rowCells.push(cell.classList.contains(`${pieceColor}`));
    });
    shape.push(rowCells);
  });
  // Fix orientation for server, then rotate
  const processedShape = correctPieceOrientation(shape);
  return rotateShape(processedShape, GameState.selectedRotation || 0);
}

/**
 * Update isValidPlacement to use rotated shape
 */
function isValidPlacement(x, y) {
  if (!GameState.selectedPiece) return false;
  const pieceColor = GameState.selectedPiece.getAttribute("data-piece-color");
  const processedShape = getCurrentPieceShape();
  if (!processedShape) return false;
  const height = processedShape.length;
  const width = processedShape[0] ? processedShape[0].length : 0;

  // Check if first piece for this color
  const isFirstPiece = isFirstPiecePlacement(pieceColor);

  // Flags to track corner and edge touches
  let touchesCorner = false;
  let touchesEdge = false;

  // Corner positions for first piece check
  const cornerPositions = [
    { x: 0, y: 0 }, // Top-left
    { x: 0, y: 19 }, // Bottom-left
    { x: 19, y: 0 }, // Top-right
    { x: 19, y: 19 }, // Bottom-right
  ];

  // Check each cell of the piece
  for (let dy = 0; dy < height; dy++) {
    for (let dx = 0; dx < width; dx++) {
      if (!processedShape[dy][dx]) continue; // Skip empty cells

      const boardX = x + dx;
      const boardY = y + dy;

      // Check if out of bounds
      if (boardX < 0 || boardX >= 20 || boardY < 0 || boardY >= 20) {
        return false;
      }

      // Check if cell is already occupied
      const targetCell = document.querySelector(
        `.board-cell[data-x="${boardX}"][data-y="${boardY}"]`
      );
      if (targetCell && targetCell.classList.contains("occupied")) {
        return false;
      }

      // First piece check: must cover a corner
      if (isFirstPiece) {
        for (const corner of cornerPositions) {
          if (boardX === corner.x && boardY === corner.y) {
            touchesCorner = true;
            break;
          }
        }
      } else {
        // For subsequent pieces, check adjacent cells and diagonals
        const adjacentCells = [
          // Edge neighbors (invalid if same color)
          { x: boardX - 1, y: boardY, isEdge: true }, // Left
          { x: boardX + 1, y: boardY, isEdge: true }, // Right
          { x: boardX, y: boardY - 1, isEdge: true }, // Top
          { x: boardX, y: boardY + 1, isEdge: true }, // Bottom
          // Corner neighbors (valid if same color)
          { x: boardX - 1, y: boardY - 1, isEdge: false }, // Top-Left
          { x: boardX + 1, y: boardY - 1, isEdge: false }, // Top-Right
          { x: boardX - 1, y: boardY + 1, isEdge: false }, // Bottom-Left
          { x: boardX + 1, y: boardY + 1, isEdge: false }, // Bottom-Right
        ];

        adjacentCells.forEach((adj) => {
          if (adj.x >= 0 && adj.x < 20 && adj.y >= 0 && adj.y < 20) {
            const adjCell = document.querySelector(
              `.board-cell[data-x="${adj.x}"][data-y="${adj.y}"]`
            );
            if (adjCell && adjCell.classList.contains("occupied")) {
              // Check if same color
              const adjColor = adjCell.getAttribute("data-color");
              if (adjColor === pieceColor) {
                if (adj.isEdge) {
                  touchesEdge = true; // Invalid - touching same color at edge
                } else {
                  touchesCorner = true; // Valid - touching same color at corner
                }
              }
            }
          }
        });
      }
    }
  }

  // Return validation result based on piece placement rules
  if (isFirstPiece) {
    return touchesCorner; // First piece must touch a board corner
  } else {
    return touchesCorner && !touchesEdge; // Must touch corner but not edges of same color
  }
}

/**
 * Show placement preview at the given board cell
 */
function showPlacementPreview(cell) {
  // Remove any existing preview
  removePlacementPreview();

  if (!GameState.selectedPiece) return;

  // Get cell coordinates
  const x = parseInt(cell.getAttribute("data-x"));
  const y = parseInt(cell.getAttribute("data-y"));

  // Get the board cell size to ensure preview matches board scale
  const cellSize = cell.offsetWidth;

  // Create the preview
  const pieceColor = GameState.selectedPiece.getAttribute("data-piece-color");
  const processedShape = getCurrentPieceShape();

  // Create preview container
  GameState.piecePreview = document.createElement("div");
  GameState.piecePreview.className = "piece-preview";
  GameState.piecePreview.style.position = "absolute";
  GameState.piecePreview.style.pointerEvents = "none";
  GameState.piecePreview.style.zIndex = "1000";

  // Position preview at cell location
  const cellRect = cell.getBoundingClientRect();
  GameState.piecePreview.style.left = `${cellRect.left}px`;
  GameState.piecePreview.style.top = `${cellRect.top}px`;

  // Create grid structure for preview
  const height = processedShape.length;
  const width = processedShape[0] ? processedShape[0].length : 0;

  const previewGrid = document.createElement("div");
  previewGrid.style.display = "grid";
  previewGrid.style.gridTemplateColumns = `repeat(${width}, ${cellSize}px)`;
  previewGrid.style.gridTemplateRows = `repeat(${height}, ${cellSize}px)`;

  // Create cells in the preview
  for (let row = 0; row < height; row++) {
    for (let col = 0; col < width; col++) {
      const previewCell = document.createElement("div");
      previewCell.style.width = `${cellSize}px`;
      previewCell.style.height = `${cellSize}px`;
      previewCell.style.boxSizing = "border-box";
      previewCell.style.border = "1px solid #eee";

      if (processedShape[row][col]) {
        previewCell.style.backgroundColor = pieceColor;
        previewCell.style.opacity = "0.6";
      } else {
        previewCell.style.backgroundColor = "transparent";
      }

      previewGrid.appendChild(previewCell);
    }
  }

  GameState.piecePreview.appendChild(previewGrid);
  document.body.appendChild(GameState.piecePreview);

  // Set appearance based on validity
  const isValid = isValidPlacement(x, y);
  if (isValid) {
    GameState.piecePreview.classList.add("valid-placement");
  } else {
    GameState.piecePreview.classList.add("invalid-placement");
    GameState.piecePreview.style.opacity = "0.4";
  }
}

/**
 * Remove placement preview
 */
function removePlacementPreview() {
  if (GameState.piecePreview) {
    GameState.piecePreview.remove();
    GameState.piecePreview = null;
  }
}

/**
 * Handle piece placement
 */
function placePiece(x, y) {
  if (!GameState.selectedPiece) {
    showMessage("Please select a piece first.");
    return;
  }

  // Get piece details
  const pieceId = GameState.selectedPiece.getAttribute("data-piece-id");
  const pieceColor = GameState.selectedPiece.getAttribute("data-piece-color");

  if (!pieceId || !pieceColor) {
    showMessage("Error: Piece information is incomplete.");
    return;
  }

  // Show waiting message
  showInstructions("Placing piece... please wait");

  // Immediately mark the piece as used in the UI to prevent double clicks
  GameState.selectedPiece.classList.add("used");
  GameState.selectedPiece.classList.remove("selectable");
  GameState.selectedPiece.style.display = "none";

  // Disable all pieces during the server request
  document.querySelectorAll(".game-piece.selectable").forEach((piece) => {
    piece.classList.remove("selectable");
    piece.classList.add("disabled");
  });

  // Prepare the form data
  const formData = new FormData();
  formData.append("x", x);
  formData.append("y", y);
  formData.append("rotation", GameState.selectedRotation || 0);
  formData.append("flipped", false); // Always false for now
  formData.append("pieceId", pieceId);
  formData.append("pieceColor", pieceColor);

  // Get CSRF token if Spring Security is enabled
  const csrfToken = document
    .querySelector("meta[name='_csrf']")
    ?.getAttribute("content");
  const csrfHeader = document
    .querySelector("meta[name='_csrf_header']")
    ?.getAttribute("content");

  // Show the piece on the board immediately for better UX
  addPieceToBoard(pieceId, pieceColor, x, y, GameState.selectedRotation || 0);

  // Clear the selection
  GameState.selectedPiece = null;
  document
    .querySelectorAll(".game-piece.selected")
    .forEach((p) => p.classList.remove("selected"));
  showInstructions("Waiting for other players...");

  // Send the placement request to the server
  fetch(`/games/${GameState.gameId}/api/place-piece`, {
    method: "POST",
    body: formData,
    headers: {
      "X-Requested-With": "XMLHttpRequest",
      ...(csrfHeader ? { [csrfHeader]: csrfToken } : {}),
    },
    credentials: "same-origin",
  })
    .then((response) => {
      if (response.redirected) {
        window.location.href = response.url;
        return;
      }

      if (!response.ok) {
        return response.text().then((text) => {
          try {
            const errorJson = JSON.parse(text);
            throw new Error(
              errorJson.error || `Server error: ${response.status}`
            );
          } catch (jsonError) {
            throw new Error(text || `Server error: ${response.status}`);
          }
        });
      }

      // Set a fallback timeout in case WebSocket update doesn't arrive
      const wsUpdateTimeout = setTimeout(() => {
        refreshGameState();
      }, 2000);

      window.lastWsUpdateTimeout = wsUpdateTimeout;
      return response.text();
    })
    .catch((error) => {
      showMessage("Error placing piece: " + error.message);
      showInstructions("Something went wrong. Please try again.");

      // Re-enable pieces in case of error
      updatePieceSelectionState(true);
    });
}

/**
 * Update addPieceToBoard to accept a rotation parameter
 */
function addPieceToBoard(pieceId, pieceColor, x, y, rotation) {
  if (!pieceId || !pieceColor || x === undefined || y === undefined) {
    return;
  }
  // If this is the currently selected piece and no explicit rotation is given, use the rotated shape
  let pieceShape = null;
  let useRotation = rotation;
  if (typeof useRotation !== "number") {
    // Use the current selected rotation if not provided
    useRotation =
      GameState.selectedPiece &&
      GameState.selectedPiece.getAttribute("data-piece-id") === pieceId &&
      GameState.selectedPiece.getAttribute("data-piece-color") === pieceColor
        ? GameState.selectedRotation || 0
        : 0;
  }
  const targetPieceElement = document.querySelector(
    `.game-piece[data-piece-id="${pieceId}"][data-piece-color="${pieceColor}"]`
  );
  if (targetPieceElement) {
    const shape = [];
    const rows = targetPieceElement.querySelectorAll(".piece-row");
    rows.forEach((row) => {
      const rowCells = [];
      row.querySelectorAll(".piece-cell").forEach((cell) => {
        rowCells.push(cell.classList.contains(`${pieceColor}`));
      });
      shape.push(rowCells);
    });
    let processedShape = correctPieceOrientation(shape);
    processedShape = rotateShape(processedShape, useRotation);
    pieceShape = processedShape;
  }
  if (pieceShape) {
    applyShapeToBoard(pieceShape, x, y, pieceColor, pieceId);
  } else {
    applyBasicPiece(x, y, pieceColor, pieceId);
  }
  // Track that this piece is used
  GameState.usedPieceIds.add(pieceId);
  if (GameState.usedPieceByColor[pieceColor]) {
    GameState.usedPieceByColor[pieceColor].add(pieceId);
  }
}

/**
 * Applies a basic piece (single cell) at the specified coordinates
 */
function applyBasicPiece(x, y, color, pieceId) {
  // Make sure coordinates are numbers
  x = parseInt(x);
  y = parseInt(y);

  // Find the target cell
  const cell = document.querySelector(
    `.board-cell[data-x="${x}"][data-y="${y}"]`
  );
  if (!cell) return;

  // Set properties on the cell
  cell.style.backgroundColor = color.toLowerCase();
  cell.classList.add("occupied");

  // Use monomino pieceId (1) for single-cell pieces if needed
  if (pieceId !== "1" && (!pieceId || pieceId.length === 0)) {
    pieceId = "1";
  }

  // Store data attributes
  if (pieceId) cell.setAttribute("data-piece-id", pieceId);
  cell.setAttribute("data-color", color.toLowerCase());

  // Ensure this piece is tracked
  if (color && GameState.usedPieceByColor[color.toLowerCase()]) {
    GameState.usedPieceByColor[color.toLowerCase()].add(pieceId);
  }
}

/**
 * Applies a piece shape to the board starting at the specified coordinates
 */
function applyShapeToBoard(shape, startX, startY, color, pieceId) {
  // Make sure coordinates are numbers
  startX = parseInt(startX);
  startY = parseInt(startY);

  for (let y = 0; y < shape.length; y++) {
    for (let x = 0; x < shape[y].length; x++) {
      // If this cell in the shape is filled
      if (shape[y][x]) {
        // Calculate board coordinates
        const boardX = startX + x;
        const boardY = startY + y;

        // Get the corresponding cell on the board
        const cell = document.querySelector(
          `.board-cell[data-x="${boardX}"][data-y="${boardY}"]`
        );

        // If cell exists, set its properties
        if (cell) {
          cell.style.backgroundColor = color.toLowerCase();
          cell.classList.add("occupied");
          cell.setAttribute("data-piece-id", pieceId);
          cell.setAttribute("data-color", color.toLowerCase());
        }
      }
    }
  }
}

/**
 * Check if it's the first piece for this color
 */
function isFirstPiecePlacement(color) {
  // Look for pieces with this color on the board
  const colorPieces = document.querySelectorAll(
    `.board-cell.occupied[data-color="${color}"]`
  );
  return colorPieces.length === 0;
}

// ==========================================
// WEBSOCKET AND SERVER COMMUNICATION
// ==========================================

/**
 * Initialize WebSocket connection
 */
function initializeWebSocket() {
  if (!GameState.gameId) {
    return;
  }

  const socket = new SockJS("/ws-blokus");
  GameState.stompClient = Stomp.over(socket);

  // Turn off debug logging
  GameState.stompClient.debug = null;

  GameState.stompClient.connect(
    {},
    function (frame) {
      // Subscribe to game updates
      GameState.stompClient.subscribe(
        `/topic/games/${GameState.gameId}`,
        function (message) {
          try {
            const gameUpdate = JSON.parse(message.body);

            // Handle different types of updates
            switch (gameUpdate.type) {
              case "GAME_STATE":
                handleGameStateUpdate(gameUpdate);
                break;
              case "NEXT_TURN":
                handleNextTurn(gameUpdate);
                break;
              case "PIECE_PLACED":
                handlePiecePlacement(gameUpdate);
                break;
              case "GAME_OVER":
                handleGameOver(gameUpdate.data);
                break;
              default:
                break;
            }
          } catch (error) {
            console.error("Error handling WebSocket message:", error);
          }
        }
      );

      // Send a connection message
      GameState.stompClient.send(
        `/app/games/${GameState.gameId}/connect`,
        {},
        JSON.stringify({
          type: "CONNECT",
          gameId: GameState.gameId,
        })
      );
    },
    function (error) {
      // Connection error handler
      showMessage("Connection lost. Reconnecting in 5 seconds...");

      // Try to reconnect after a delay
      setTimeout(initializeWebSocket, 5000);
    }
  );
}

/**
 * Handle a game state update from the server
 */
function handleGameStateUpdate(gameUpdate) {
  // Update the board if provided
  if (gameUpdate.board) {
    updateBoard(gameUpdate.board);
  }

  // Update current player
  if (gameUpdate.currentPlayer) {
    updateCurrentPlayer(gameUpdate.currentPlayer);
  }

  // Update piece availability
  if (gameUpdate.availablePieces) {
    updateAvailablePieces(gameUpdate.availablePieces);
  }

  // Update game status
  if (gameUpdate.gameStatus) {
    updateGameStatus(gameUpdate.gameStatus);
  }
}

/**
 * Handle a next turn update from the server
 */
function handleNextTurn(turnUpdate) {
  // Clear any pending WebSocket update timeout
  if (window.lastWsUpdateTimeout) {
    clearTimeout(window.lastWsUpdateTimeout);
    window.lastWsUpdateTimeout = null;
  }

  // Extract next player information
  const nextPlayerUsername = turnUpdate.data.nextPlayerUsername;

  // Update current player display
  const currentPlayerSpan = document.getElementById("current-player");
  if (currentPlayerSpan) {
    currentPlayerSpan.textContent = nextPlayerUsername;
    currentPlayerSpan.classList.remove("current-player-active");

    if (nextPlayerUsername === GameState.currentUsername) {
      // It's the current user's turn
      currentPlayerSpan.classList.add("current-player-active");
      showMessage("It's your turn!");
      updatePieceSelectionState(true);
    } else if (nextPlayerUsername.startsWith("Bot ")) {
      // It's a bot's turn
      showMessage("Bot is making a move...");
      updatePieceSelectionState(false);

      // For bot turns, refresh game state after a short delay
      setTimeout(() => {
        refreshGameState();
      }, 1000);
    } else {
      // It's another human player's turn
      showMessage("It's " + nextPlayerUsername + "'s turn");
      updatePieceSelectionState(false);
    }

    // Hide used pieces
    hideAllUsedPieces();
  }
}

/**
 * Handle a piece placement update from the server
 */
function handlePiecePlacement(placementUpdate) {
  // Clear any pending WebSocket update timeout
  if (window.lastWsUpdateTimeout) {
    clearTimeout(window.lastWsUpdateTimeout);
    window.lastWsUpdateTimeout = null;
  }
  // Extract placement data
  const data = placementUpdate.data;
  const isBot = data.username && data.username.toLowerCase().includes("bot");
  // Add the piece to the board using server rotation
  addPieceToBoard(data.pieceId, data.pieceColor, data.x, data.y, data.rotation);
  // Track that this piece has been used
  GameState.usedPieceIds.add(data.pieceId);
  if (data.pieceColor && GameState.usedPieceByColor[data.pieceColor]) {
    GameState.usedPieceByColor[data.pieceColor].add(data.pieceId);
  }
  // Hide the corresponding piece in the player's box
  const pieceElements = document.querySelectorAll(
    `.game-piece[data-piece-id="${data.pieceId}"][data-piece-color="${data.pieceColor}"]`
  );
  pieceElements.forEach((pieceElement) => {
    pieceElement.classList.add("used");
    pieceElement.classList.remove("selectable");
    pieceElement.style.display = "none";
  });
  // For bot moves, ensure the correct piece ID is on the board
  if (isBot) {
    ensureCorrectPieceIdOnBoard(data.pieceId, data.pieceColor, data.x, data.y);
  }
  // Refresh the UI to ensure all pieces are properly hidden
  hideAllUsedPieces();
}

/**
 * Handle a game over update
 */
function handleGameOver(data) {
  // Display game over and final scores
  const gameStatus = document.querySelector(".game-status");
  if (gameStatus) {
    let scoresHtml = "<h2>Game Over! Winner: " + data.winnerUsername + "</h2>";
    scoresHtml += '<ul class="final-scores">';

    for (const [player, score] of Object.entries(data.scores)) {
      scoresHtml += `<li>${player}: ${score} points</li>`;
    }

    scoresHtml += "</ul>";
    gameStatus.innerHTML = scoresHtml;
  }
}

/**
 * Refresh the game state from the server
 */
function refreshGameState() {
  if (!GameState.gameId) {
    return;
  }

  fetch(`/games/${GameState.gameId}/api/state`)
    .then((response) => {
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      return response.json();
    })
    .then((gameState) => {
      // Reset tracking data
      GameState.usedPieceIds.clear();
      Object.keys(GameState.usedPieceByColor).forEach((color) => {
        GameState.usedPieceByColor[color].clear();
      });

      // Update game components based on server data
      if (gameState.currentPlayer) {
        updateCurrentPlayer(gameState.currentPlayer);
      }

      if (gameState.board) {
        updateBoard(gameState.board);
      }

      // Validate and fix board piece IDs
      validateAndFixBoardPieceIds();

      // Update available pieces
      if (gameState.availablePieces) {
        updateAvailablePieces(gameState.availablePieces);
      }

      // Update game status
      if (gameState.status) {
        updateGameStatus(gameState.status);
      }

      // Update player data
      if (gameState.players) {
        updatePlayers(gameState.players);
      }

      // Finally, hide used pieces
      hideAllUsedPieces();

      // Run an additional check after a short delay
      setTimeout(() => {
        validateAndFixBoardPieceIds();
        updateHiddenPiecesBasedOnBoard();
      }, 300);
    })
    .catch((error) => {
      console.error("Error refreshing game state:", error);
    });
}

// ==========================================
// UI AND DISPLAY FUNCTIONS
// ==========================================

/**
 * Show a message
 */
function showMessage(message) {
  const messageElement = document.querySelector(".game-message");
  if (!messageElement) {
    // Create message element if it doesn't exist
    const newMessageElement = document.createElement("div");
    newMessageElement.className = "game-message";
    document.body.appendChild(newMessageElement);

    // Use the newly created element
    newMessageElement.textContent = message;
    newMessageElement.style.display = "block";

    // Hide after 3 seconds
    setTimeout(() => {
      newMessageElement.style.display = "none";
    }, 3000);
  } else {
    // Use existing message element
    messageElement.textContent = message;
    messageElement.style.display = "block";

    // Hide after 3 seconds
    setTimeout(() => {
      messageElement.style.display = "none";
    }, 3000);
  }
}

/**
 * Show game instructions to the user
 */
function showInstructions(message) {
  const instructionsElement = document.querySelector(".game-instructions");
  if (instructionsElement) {
    instructionsElement.textContent = message;
  }
}

/**
 * Initialize the current player display
 */
function initializeCurrentPlayerDisplay() {
  // Get the current player element
  const currentPlayerSpan = document.querySelector(".game-info span");
  if (!currentPlayerSpan) return;

  const playerName = currentPlayerSpan.textContent.trim();

  // Try to get the current user's username if not already set
  if (!GameState.currentUsername) {
    const currentUserElement = document.querySelector(".current-user");
    if (currentUserElement) {
      GameState.currentUsername =
        currentUserElement.getAttribute("data-username");
    }
  }

  // Check if it's the current user's turn
  if (GameState.currentUsername && playerName === GameState.currentUsername) {
    currentPlayerSpan.classList.add("current-player-active");
    showMessage("It's your turn!");
  } else if (playerName.startsWith("Bot ")) {
    showMessage(playerName + " is making a move...");
  } else {
    showMessage("It's " + playerName + "'s turn");
  }

  // Update piece selection state
  updatePieceSelectionState(true);
}

/**
 * Update piece selection state based on whose turn it is
 */
function updatePieceSelectionState(enable) {
  // Get all player areas
  const playerAreas = document.querySelectorAll(".player-area");

  // If currentUsername is not set, try to get it
  if (!GameState.currentUsername) {
    const currentUserElement = document.querySelector(".current-user");
    if (currentUserElement) {
      GameState.currentUsername =
        currentUserElement.getAttribute("data-username");
    }
  }

  // If still no username, look for active player
  if (!GameState.currentUsername) {
    const activePlayer = document.querySelector(".current-player-active");
    if (activePlayer) {
      GameState.currentUsername = activePlayer.textContent.trim();
    }
  }

  playerAreas.forEach((area) => {
    // Get player info from the area
    const playerInfo = area.querySelector(".player-info");
    if (!playerInfo) return;

    const playerName = playerInfo.textContent.trim();

    // Special case: If this is the only non-bot player and we don't have a username,
    // assume this is the current user
    if (
      !GameState.currentUsername &&
      playerName &&
      !playerName.includes("Bot")
    ) {
      GameState.currentUsername = playerName;
    }

    const isCurrentUserArea = playerName === GameState.currentUsername;

    // Find all unused pieces in this area
    const unusedPieces = area.querySelectorAll(".game-piece:not(.used)");

    // If forced enable, or it's the current user's turn
    const shouldEnable = enable || isCurrentUserArea;

    unusedPieces.forEach((piece) => {
      if (shouldEnable) {
        // Enable selection for pieces
        piece.classList.add("selectable");
        piece.classList.remove("disabled");
      } else {
        // Disable selection for pieces
        piece.classList.remove("selectable");
        piece.classList.add("disabled");
      }
    });
  });

  // Try to force pieces to be selectable for the current player
  if (GameState.currentUsername) {
    document.querySelectorAll(".player-info").forEach((info) => {
      if (info.textContent.trim() === GameState.currentUsername) {
        const playerArea = info.closest(".player-area");
        if (playerArea) {
          const pieces = playerArea.querySelectorAll(".game-piece:not(.used)");
          pieces.forEach((piece) => {
            piece.classList.add("selectable");
            piece.classList.remove("disabled");
          });
        }
      }
    });
  }
}

/**
 * Update the board based on server data
 */
function updateBoard(boardData) {
  boardData.forEach((cell) => {
    const { x, y, color, pieceId, rotation } = cell;
    if (color) {
      addPieceToBoard(pieceId, color, x, y, rotation);
    }
  });
}

/**
 * Update current player information
 */
function updateCurrentPlayer(playerData) {
  const currentPlayerSpan = document.getElementById("current-player");
  if (!currentPlayerSpan) return;

  currentPlayerSpan.textContent = playerData.username;
  currentPlayerSpan.style.color = playerData.color.toLowerCase();
}

/**
 * Update available pieces based on server data
 */
function updateAvailablePieces(availablePieces) {
  Object.entries(availablePieces).forEach(([playerColor, pieces]) => {
    pieces.forEach((pieceId) => {
      // Find pieces that match this ID and color
      const pieceElements = document.querySelectorAll(
        `.game-piece[data-piece-id="${pieceId}"][data-piece-color="${playerColor}"]`
      );

      // Mark them as available
      pieceElements.forEach((pieceElement) => {
        pieceElement.classList.remove("used");
      });
    });
  });
}

/**
 * Update game status (e.g., game over)
 */
function updateGameStatus(gameStatus) {
  if (gameStatus === "GAME_OVER") {
    showMessage("Game Over!");

    // Disable all piece selection
    updatePieceSelectionState(false);
  }
}

/**
 * Update player information based on server data
 */
function updatePlayers(players) {
  players.forEach((player) => {
    const { username, color, usedPieces, score } = player;
    const isBot = username && username.toLowerCase().includes("bot");

    // Find the player area for this player
    const playerInfoElements = document.querySelectorAll(".player-info");
    playerInfoElements.forEach((element) => {
      if (element.textContent.trim() === username) {
        const playerArea = element.closest(".player-area");
        if (!playerArea) return;

        // Update score if available
        if (score !== undefined) {
          const scoreElement = playerArea.querySelector(".player-score");
          if (scoreElement) {
            scoreElement.textContent = `Score: ${score}`;
          }
        }

        // For bots, only track pieces that are on the board
        if (isBot) {
          // We let the board scanning functions handle bot pieces
          return;
        }

        // For human players, mark used pieces from server data
        if (usedPieces && usedPieces.length > 0 && color) {
          usedPieces.forEach((pieceId) => {
            // Find piece by ID and color in this player's area
            const pieceElement = playerArea.querySelector(
              `.game-piece[data-piece-id="${pieceId}"][data-piece-color="${color}"]`
            );

            if (pieceElement) {
              pieceElement.classList.add("used");
              pieceElement.classList.remove("selectable");
              pieceElement.style.display = "none";

              // Track in global state
              GameState.usedPieceIds.add(pieceId);
              if (GameState.usedPieceByColor[color]) {
                GameState.usedPieceByColor[color].add(pieceId);
              }
            }
          });
        }
      }
    });
  });
}

// ==========================================
// BOARD STATE VALIDATION AND CORRECTION
// ==========================================

/**
 * Ensure that the correct piece ID from the server is set on board cells
 */
function ensureCorrectPieceIdOnBoard(pieceId, pieceColor, startX, startY) {
  const cell = document.querySelector(
    `.board-cell[data-x="${startX}"][data-y="${startY}"]`
  );

  if (!cell || cell.getAttribute("data-color") !== pieceColor) return;

  // Use flood fill algorithm to find connected cells of same color
  const cellsToProcess = [cell];
  const processedCells = new Set();

  while (cellsToProcess.length > 0) {
    const currentCell = cellsToProcess.pop();
    const cellX = parseInt(currentCell.getAttribute("data-x"));
    const cellY = parseInt(currentCell.getAttribute("data-y"));
    const cellKey = `${cellX},${cellY}`;

    // Skip if already processed
    if (processedCells.has(cellKey)) continue;

    // Mark as processed
    processedCells.add(cellKey);

    // Set the correct piece ID
    if (currentCell.getAttribute("data-piece-id") !== pieceId) {
      currentCell.setAttribute("data-piece-id", pieceId);
    }

    // Check adjacent cells (4-connected neighborhood)
    const adjacentPositions = [
      { x: cellX + 1, y: cellY }, // right
      { x: cellX - 1, y: cellY }, // left
      { x: cellX, y: cellY + 1 }, // down
      { x: cellX, y: cellY - 1 }, // up
    ];

    for (const pos of adjacentPositions) {
      const adjacentCell = document.querySelector(
        `.board-cell[data-x="${pos.x}"][data-y="${pos.y}"]`
      );

      if (
        adjacentCell &&
        adjacentCell.getAttribute("data-color") === pieceColor &&
        adjacentCell.classList.contains("occupied") &&
        !processedCells.has(`${pos.x},${pos.y}`)
      ) {
        cellsToProcess.push(adjacentCell);
      }
    }
  }
}

/**
 * Validate and fix board piece IDs to match what was actually placed
 */
function validateAndFixBoardPieceIds() {
  // Get all occupied cells on the board
  const occupiedCells = document.querySelectorAll(".board-cell.occupied");

  // Create a map to track pieces by color
  const boardPiecesByColor = {
    red: new Set(),
    green: new Set(),
    blue: new Set(),
    yellow: new Set(),
  };

  // Process each cell to build the map
  occupiedCells.forEach((cell) => {
    const pieceId = cell.getAttribute("data-piece-id");
    const pieceColor = cell.getAttribute("data-color");

    if (pieceId && pieceColor && boardPiecesByColor[pieceColor]) {
      boardPiecesByColor[pieceColor].add(pieceId);
    }
  });

  // Validate piece groups for consistency
  const pieceGroups = new Map();
  occupiedCells.forEach((cell) => {
    const pieceId = cell.getAttribute("data-piece-id");
    const pieceColor = cell.getAttribute("data-color");
    const x = parseInt(cell.getAttribute("data-x"));
    const y = parseInt(cell.getAttribute("data-y"));

    if (pieceId && pieceColor) {
      const key = `${pieceColor}-${pieceId}`;
      if (!pieceGroups.has(key)) {
        pieceGroups.set(key, []);
      }
      pieceGroups.get(key).push({ cell, x, y });
    }
  });

  // Fix monominos (single cell pieces should have ID 1)
  pieceGroups.forEach((cells, key) => {
    const [color, pieceId] = key.split("-");

    // For pieces with just 1 cell, it must be piece ID 1 (the monomino)
    if (cells.length === 1) {
      const cell = cells[0].cell;

      // If this is a single cell and it's not marked as piece 1, fix it
      if (pieceId !== "1") {
        cell.setAttribute("data-piece-id", "1");

        // Update tracking data structures
        if (GameState.usedPieceByColor[color]) {
          GameState.usedPieceByColor[color].delete(pieceId);
          GameState.usedPieceByColor[color].add("1");

          // Also update boardPiecesByColor
          if (boardPiecesByColor[color]) {
            boardPiecesByColor[color].delete(pieceId);
            boardPiecesByColor[color].add("1");
          }
        }
      }

      // Ensure monomino is properly tracked
      if (pieceId === "1" || cell.getAttribute("data-piece-id") === "1") {
        GameState.usedPieceIds.add("1");
        if (GameState.usedPieceByColor[color]) {
          GameState.usedPieceByColor[color].add("1");
        }
      }
    }
  });

  // Sync board state with tracking data
  Object.keys(GameState.usedPieceByColor).forEach((color) => {
    const boardPieces = boardPiecesByColor[color] || new Set();
    const trackedPieces = GameState.usedPieceByColor[color] || new Set();

    // Add pieces on board but not in tracking
    boardPieces.forEach((pieceId) => {
      if (!trackedPieces.has(pieceId)) {
        GameState.usedPieceByColor[color].add(pieceId);
      }
    });

    // For bot colors, remove pieces in tracking but not on board
    if (color === "red" || color === "green") {
      trackedPieces.forEach((pieceId) => {
        if (
          !boardPieces.has(pieceId) &&
          !pieceOnBoard(pieceId, color) &&
          pieceId !== "1"
        ) {
          GameState.usedPieceByColor[color].delete(pieceId);
        }
      });
    }
  });

  // Ensure all pieces on the board are hidden in player areas
  updateHiddenPiecesBasedOnBoard();
}

/**
 * Checks if a piece is actually on the board
 */
function pieceOnBoard(pieceId, color) {
  const cells = document.querySelectorAll(
    `.board-cell.occupied[data-piece-id="${pieceId}"][data-color="${color}"]`
  );
  return cells.length > 0;
}

/**
 * Update hidden pieces in player areas based on what's on the board
 */
function updateHiddenPiecesBasedOnBoard() {
  // Create a map to track pieces on board by color
  const piecesOnBoardByColor = {
    red: new Set(),
    green: new Set(),
    blue: new Set(),
    yellow: new Set(),
  };

  // Find all pieces on the board
  const occupiedCells = document.querySelectorAll(".board-cell.occupied");
  occupiedCells.forEach((cell) => {
    const pieceId = cell.getAttribute("data-piece-id");
    const pieceColor = cell.getAttribute("data-color");

    if (pieceId && pieceColor && piecesOnBoardByColor[pieceColor]) {
      piecesOnBoardByColor[pieceColor].add(pieceId);
    }
  });

  // Go through each player area and hide pieces that are on the board
  const playerAreas = document.querySelectorAll(".player-area");
  playerAreas.forEach((area) => {
    const playerInfo = area.querySelector(".player-info");
    if (!playerInfo) return;

    const playerName = playerInfo.textContent.trim();
    const playerPieces = area.querySelector(".player-pieces");
    if (!playerPieces) return;

    const playerColor = playerPieces.getAttribute("data-player-color");
    if (!playerColor) return;

    // Get pieces that should be hidden for this color
    const piecesToHide = piecesOnBoardByColor[playerColor] || new Set();

    // Find pieces in this player's area
    const pieces = area.querySelectorAll(".game-piece");

    // Check each piece
    pieces.forEach((piece) => {
      const pieceId = piece.getAttribute("data-piece-id");
      const pieceColor = piece.getAttribute("data-piece-color");

      // Skip pieces that don't match this player's color
      if (!pieceId || !pieceColor || pieceColor !== playerColor) return;

      // Check if this piece should be hidden
      if (piecesToHide.has(pieceId)) {
        piece.classList.add("used");
        piece.classList.remove("selectable");
        piece.style.display = "none";
      }
    });
  });
}

/**
 * Combined function to hide used pieces across all player areas
 */
function hideAllUsedPieces() {
  // Get all occupied cells on the board
  const occupiedCells = document.querySelectorAll(".board-cell.occupied");

  // Track pieces on board by color
  const piecesOnBoardByColor = {
    blue: new Set(),
    yellow: new Set(),
    red: new Set(),
    green: new Set(),
  };

  // Find all unique pieces on the board
  occupiedCells.forEach((cell) => {
    const pieceId = cell.getAttribute("data-piece-id");
    const pieceColor = cell.getAttribute("data-color");

    if (pieceId && pieceColor && piecesOnBoardByColor[pieceColor]) {
      piecesOnBoardByColor[pieceColor].add(pieceId);
    }
  });

  // Process each player area
  const playerAreas = document.querySelectorAll(".player-area");

  playerAreas.forEach((area) => {
    const playerInfo = area.querySelector(".player-info");
    if (!playerInfo) return;

    const playerName = playerInfo.textContent.trim();
    const playerPieces = area.querySelector(".player-pieces");
    if (!playerPieces) return;

    const playerColor = playerPieces.getAttribute("data-player-color");
    const isBot = playerName.toLowerCase().includes("bot");

    // Get pieces that should be hidden
    const piecesOnBoard = piecesOnBoardByColor[playerColor] || new Set();
    const trackedPieces = GameState.usedPieceByColor[playerColor] || new Set();

    // For bots only use what's on board, for humans use combined data
    const piecesToHide = isBot
      ? new Set([...piecesOnBoard])
      : new Set([...piecesOnBoard, ...trackedPieces]);

    // Hide each piece
    area.querySelectorAll(".game-piece").forEach((piece) => {
      const pieceId = piece.getAttribute("data-piece-id");
      const pieceColor = piece.getAttribute("data-piece-color");

      // Skip pieces that don't match this player's color
      if (!pieceId || !pieceColor || pieceColor !== playerColor) return;

      // Hide piece if it should be hidden
      if (piecesToHide.has(pieceId)) {
        piece.classList.add("used");
        piece.classList.remove("selectable");
        piece.style.display = "none";
      }
    });
  });
}

// ==========================================
// PIECE CONTROLS
// ==========================================

/**
 * Initialize piece controls (rotate, flip)
 */
function initializePieceControls() {
  // Get control elements
  const rotateLeftButton = document.getElementById("rotate-left");
  const rotateRightButton = document.getElementById("rotate-right");
  const flipButton = document.getElementById("flip");

  // Helper to update the selected piece's visual rotation
  function updateSelectedPieceRotation() {
    if (GameState.selectedPiece) {
      const container =
        GameState.selectedPiece.querySelector(".piece-container");
      if (container) {
        container.style.transform = `rotate(${GameState.selectedRotation}deg)`;
      }
    }
  }

  // Add rotation and flip functionality
  if (rotateLeftButton) {
    rotateLeftButton.addEventListener("click", function () {
      if (!GameState.selectedPiece) return;
      GameState.selectedRotation =
        (GameState.selectedRotation - 90 + 360) % 360;
      updateSelectedPieceRotation();
    });
  }

  if (rotateRightButton) {
    rotateRightButton.addEventListener("click", function () {
      if (!GameState.selectedPiece) return;
      GameState.selectedRotation = (GameState.selectedRotation + 90) % 360;
      updateSelectedPieceRotation();
    });
  }

  if (flipButton) {
    flipButton.addEventListener("click", function () {
      // Implement flip later if needed
    });
  }

  // Add keyboard controls
  document.addEventListener("keydown", function (event) {
    if (!GameState.selectedPiece) return;

    // Left arrow for rotate left
    if (event.key === "ArrowLeft") {
      GameState.selectedRotation =
        (GameState.selectedRotation - 90 + 360) % 360;
      updateSelectedPieceRotation();
      event.preventDefault();
    }
    // Right arrow for rotate right
    else if (event.key === "ArrowRight") {
      GameState.selectedRotation = (GameState.selectedRotation + 90) % 360;
      updateSelectedPieceRotation();
      event.preventDefault();
    }
    // F for flip
    else if (event.key === "f" || event.key === "F") {
      // Implement flip later if needed
      event.preventDefault();
    }
  });
}
