/**
 * Blokus Game Client-side Logic
 * Handles piece selection and placement
 */

// Global variables
let selectedPiece = null;
let piecePreview = null;
let stompClient = null;
let gameId = null;

document.addEventListener("DOMContentLoaded", function () {
  // Get game ID from the URL
  const path = window.location.pathname;
  const pathParts = path.split("/");
  const gameIdIndex = pathParts.indexOf("games") + 1;

  if (gameIdIndex > 0 && gameIdIndex < pathParts.length) {
    gameId = pathParts[gameIdIndex];
  }

  // Initialize piece selection event listeners
  initializePieceSelection();

  // Initialize board hover effect for placement preview
  initializeBoardHover();

  // Initialize WebSocket for real-time updates
  initializeWebSocket();
});

/**
 * Initialize piece selection functionality
 */
function initializePieceSelection() {
  const selectablePieces = document.querySelectorAll(".game-piece.selectable");

  selectablePieces.forEach((piece) => {
    piece.addEventListener("click", function () {
      // Remove selected class from all pieces
      document
        .querySelectorAll(".game-piece.selected")
        .forEach((p) => p.classList.remove("selected"));

      // Add selected class to clicked piece
      this.classList.add("selected");

      // Store selected piece
      selectedPiece = this;

      // Get piece data
      const pieceId = this.getAttribute("data-piece-id");
      const pieceColor = this.getAttribute("data-piece-color");

      // Set form values
      const selectedPieceIdElement = document.getElementById("selectedPieceId");
      const selectedPieceColorElement =
        document.getElementById("selectedPieceColor");

      if (selectedPieceIdElement) {
        selectedPieceIdElement.value = pieceId;
      }

      if (selectedPieceColorElement) {
        selectedPieceColorElement.value = pieceColor;
      }

      // Show placement instructions
      showInstructions("Click on the board to place your piece");

      console.log("Piece selected:", pieceId, "color:", pieceColor);
    });
  });
}

/**
 * Initialize board hover effect for placement preview
 */
function initializeBoardHover() {
  const boardCells = document.querySelectorAll(".board-cell");

  boardCells.forEach((cell) => {
    cell.addEventListener("mouseenter", function () {
      if (!selectedPiece) return;

      // Show placement preview
      showPlacementPreview(this);
    });

    cell.addEventListener("mouseleave", function () {
      // Remove placement preview
      removePlacementPreview();
    });

    // Add click handler for piece placement
    cell.addEventListener("click", function () {
      if (!selectedPiece) return;

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

/**
 * Fix the piece shape orientation to match the server's expected format
 * This corrects for the HTML template's transposition of rows and columns
 */
function correctPieceOrientation(shape) {
  // Create a new shape with the right dimensions
  const height = shape.length;
  const width = shape[0] ? shape[0].length : 0;

  // Create an empty matrix with width and height swapped
  const correctedShape = Array(width)
    .fill()
    .map(() => Array(height).fill(false));

  // Fill in the values with transposed coordinates
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      // Transpose coordinates: [y][x] becomes [x][y]
      correctedShape[x][y] = shape[y][x];
    }
  }

  return correctedShape;
}

/**
 * Check if the placement is valid according to Blokus rules
 * 1. First piece must be placed in a corner
 * 2. Subsequent pieces must touch at least one piece of the same color at corners
 * 3. Pieces can't touch pieces of the same color at edges
 * 4. Pieces can't overlap with existing pieces
 */
function isValidPlacement(x, y) {
  if (!selectedPiece) return false;

  const pieceColor = selectedPiece.getAttribute("data-piece-color");
  console.log(
    `Checking placement validity for color ${pieceColor} at position (${x},${y})`
  );

  // Get the piece shape - this extracts the shape as it appears in the DOM
  const pieceElement = selectedPiece;
  const shape = [];
  const originalRows = pieceElement.querySelectorAll(".piece-row");
  originalRows.forEach((row) => {
    const rowCells = [];
    row.querySelectorAll(".piece-cell").forEach((cell) => {
      rowCells.push(cell.classList.contains(`${pieceColor}`));
    });
    shape.push(rowCells);
  });

  // Fix the orientation to match server's expected format
  const processedShape = correctPieceOrientation(shape);

  // Log the shape for debugging
  console.log("Original shape:", shape);
  console.log("Corrected shape for validation:", processedShape);

  // Check if any cell in the shape would overlap with an existing piece
  const height = processedShape.length;
  const width = processedShape[0] ? processedShape[0].length : 0;

  // Check for basic game start (first move must be in a corner)
  const isFirstPiece =
    document.querySelectorAll(".board-cell.occupied").length === 0;
  console.log(`Is first piece placement: ${isFirstPiece}`);

  // Flag to track if the piece touches at least one piece of the same color at a corner
  let touchesCorner = false;

  // Flag to track if the piece touches an edge of the same color (invalid)
  let touchesEdge = false;

  // Keep track of corner positions for first piece check
  const cornerPositions = [
    { x: 0, y: 0 }, // Top-left
    { x: 0, y: 19 }, // Bottom-left
    { x: 19, y: 0 }, // Top-right
    { x: 19, y: 19 }, // Bottom-right
  ];

  // Check each cell of the piece
  for (let dy = 0; dy < height; dy++) {
    for (let dx = 0; dx < width; dx++) {
      if (!processedShape[dy][dx]) continue; // Skip empty cells in the piece shape

      const boardX = x + dx;
      const boardY = y + dy;

      // Check if out of bounds
      if (boardX < 0 || boardX >= 20 || boardY < 0 || boardY >= 20) {
        console.log(`Placement out of bounds at (${boardX},${boardY})`);
        return false;
      }

      // Check if the cell is already occupied
      const targetCell = document.querySelector(
        `.board-cell[data-x="${boardX}"][data-y="${boardY}"]`
      );
      if (targetCell && targetCell.classList.contains("occupied")) {
        console.log(`Cell already occupied at (${boardX},${boardY})`);
        return false; // Can't place on an occupied cell
      }

      // First piece check: see if any part of the piece covers a corner
      if (isFirstPiece) {
        for (const corner of cornerPositions) {
          if (boardX === corner.x && boardY === corner.y) {
            console.log(
              `First piece touches corner at (${corner.x},${corner.y})`
            );
            touchesCorner = true;
            break;
          }
        }
      } else {
        // For subsequent pieces, check adjacent cells and diagonals
        // Check the 8 surrounding cells
        const adjacentCells = [
          { x: boardX - 1, y: boardY, isEdge: true }, // Left
          { x: boardX + 1, y: boardY, isEdge: true }, // Right
          { x: boardX, y: boardY - 1, isEdge: true }, // Top
          { x: boardX, y: boardY + 1, isEdge: true }, // Bottom
          { x: boardX - 1, y: boardY - 1, isEdge: false }, // Top-Left (corner)
          { x: boardX + 1, y: boardY - 1, isEdge: false }, // Top-Right (corner)
          { x: boardX - 1, y: boardY + 1, isEdge: false }, // Bottom-Left (corner)
          { x: boardX + 1, y: boardY + 1, isEdge: false }, // Bottom-Right (corner)
        ];

        adjacentCells.forEach((adj) => {
          if (adj.x >= 0 && adj.x < 20 && adj.y >= 0 && adj.y < 20) {
            const adjCell = document.querySelector(
              `.board-cell[data-x="${adj.x}"][data-y="${adj.y}"]`
            );
            if (adjCell && adjCell.classList.contains("occupied")) {
              // Check if it's the same color
              const adjColor = adjCell.getAttribute("data-color");
              if (adjColor === pieceColor) {
                if (adj.isEdge) {
                  console.log(
                    `Piece touches same color at edge (${adj.x},${adj.y})`
                  );
                  touchesEdge = true; // Touching same color at edge (invalid)
                } else {
                  console.log(
                    `Piece touches same color at corner (${adj.x},${adj.y})`
                  );
                  touchesCorner = true; // Touching same color at corner (valid)
                }
              }
            }
          }
        });
      }
    }
  }

  // For the first piece, it must touch a corner
  if (isFirstPiece) {
    const result = touchesCorner;
    console.log(`First piece placement valid: ${result}`);
    // For debugging purposes, add more information if it fails
    if (!result) {
      console.log(
        "First piece must touch one of the corners: (0,0), (0,19), (19,0), or (19,19)"
      );
    }
    return result;
  }

  // For subsequent pieces, must touch at least one same-color piece at a corner
  // but not touch any same-color piece at an edge
  const result = touchesCorner && !touchesEdge;
  console.log(
    `Subsequent piece placement valid: ${result} (touchesCorner: ${touchesCorner}, touchesEdge: ${touchesEdge})`
  );
  return result;
}

/**
 * Show placement preview at the given board cell
 */
function showPlacementPreview(cell) {
  // Remove any existing preview
  removePlacementPreview();

  if (!selectedPiece) return;

  // Get cell coordinates
  const x = parseInt(cell.getAttribute("data-x"));
  const y = parseInt(cell.getAttribute("data-y"));

  // Get the board cell size to ensure preview matches board scale
  const cellSize = cell.offsetWidth; // Get the width of the board cell

  // Create the preview directly using the shape, not the visuals
  const pieceColor = selectedPiece.getAttribute("data-piece-color");

  // Get the piece shape - this extracts the shape as it appears in the DOM
  const shape = [];
  const originalRows = selectedPiece.querySelectorAll(".piece-row");
  originalRows.forEach((row) => {
    const rowCells = [];
    row.querySelectorAll(".piece-cell").forEach((cell) => {
      rowCells.push(cell.classList.contains(`${pieceColor}`));
    });
    shape.push(rowCells);
  });

  // Fix the orientation to match server's expected format
  const processedShape = correctPieceOrientation(shape);

  // Create a new container for the preview that will match board scale
  piecePreview = document.createElement("div");
  piecePreview.className = "piece-preview";
  piecePreview.style.position = "absolute";
  piecePreview.style.pointerEvents = "none";
  piecePreview.style.zIndex = "1000";

  // Get board position for proper alignment
  const cellRect = cell.getBoundingClientRect();
  piecePreview.style.left = `${cellRect.left}px`;
  piecePreview.style.top = `${cellRect.top}px`;

  // Create a grid matching the piece shape at board scale
  const height = processedShape.length;
  const width = processedShape[0] ? processedShape[0].length : 0;

  // Create the overlay grid
  const previewGrid = document.createElement("div");
  previewGrid.style.display = "grid";
  previewGrid.style.gridTemplateColumns = `repeat(${width}, ${cellSize}px)`;
  previewGrid.style.gridTemplateRows = `repeat(${height}, ${cellSize}px)`;

  // Create each cell in the preview
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

  piecePreview.appendChild(previewGrid);
  document.body.appendChild(piecePreview);

  // Check if placement is valid and set appropriate class
  const isValid = isValidPlacement(x, y);
  if (isValid) {
    piecePreview.classList.add("valid-placement");
  } else {
    piecePreview.classList.add("invalid-placement");
    piecePreview.style.opacity = "0.4"; // Make invalid placements more transparent
  }
}

/**
 * Remove placement preview
 */
function removePlacementPreview() {
  if (piecePreview) {
    piecePreview.remove();
    piecePreview = null;
  }
}

/**
 * Handle piece placement
 */
function placePiece(x, y) {
  if (!selectedPiece) {
    console.error("No piece selected for placement");
    showMessage("Please select a piece first.");
    return;
  }

  console.log("Placing piece at:", x, y);

  // Get piece details
  const pieceId = selectedPiece.getAttribute("data-piece-id");
  const pieceColor = selectedPiece.getAttribute("data-piece-color");

  if (!pieceId || !pieceColor) {
    console.error("Piece data is missing:", { pieceId, pieceColor });
    showMessage("Error: Piece information is incomplete.");
    return;
  }

  console.log("Piece details:", {
    id: pieceId,
    color: pieceColor,
  });

  // Show waiting message
  showInstructions("Placing piece... please wait");

  // Prepare the form data
  const formData = new FormData();
  formData.append("x", x);
  formData.append("y", y);
  formData.append("rotation", 0); // Always 0
  formData.append("flipped", false); // Always false
  formData.append("pieceId", pieceId); // Add piece ID directly to request
  formData.append("pieceColor", pieceColor); // Add piece color directly to request

  // Get the CSRF token if Spring Security is enabled
  const csrfToken = document
    .querySelector("meta[name='_csrf']")
    ?.getAttribute("content");
  const csrfHeader = document
    .querySelector("meta[name='_csrf_header']")
    ?.getAttribute("content");

  // Keep reference to selected piece for later use in case it becomes null during async operation
  const currentSelectedPiece = selectedPiece;

  console.log(
    `Sending place-piece request to /games/${gameId}/place-piece with:`,
    {
      x,
      y,
      pieceId,
      pieceColor,
      rotation: 0,
      flipped: false,
    }
  );

  // Send the request via fetch instead of form submission
  fetch(`/games/${gameId}/place-piece`, {
    method: "POST",
    body: formData,
    headers: {
      "X-Requested-With": "XMLHttpRequest",
      ...(csrfHeader ? { [csrfHeader]: csrfToken } : {}),
    },
    credentials: "same-origin",
  })
    .then((response) => {
      console.log("Place piece response status:", response.status);

      if (response.redirected) {
        // If the server indicates a redirect, follow it
        window.location.href = response.url;
        return;
      }

      if (!response.ok) {
        throw new Error(`Server error: ${response.status}`);
      }

      // Successful placement - update the UI without reloading
      console.log("Piece placed successfully");

      // Mark the piece as used in the inventory (if available)
      if (currentSelectedPiece && currentSelectedPiece.classList) {
        currentSelectedPiece.classList.add("used");
      } else {
        // If selectedPiece is null, try to find it by ID and color
        console.log(
          "Selected piece not available, trying to find it in the DOM"
        );
        const pieceToMark = document.querySelector(
          `.game-piece[data-piece-id="${pieceId}"][data-piece-color="${pieceColor}"]`
        );
        if (pieceToMark) {
          pieceToMark.classList.add("used");
        }
      }

      // Add the piece to the board immediately
      addPieceToBoard(pieceId, pieceColor, x, y);

      // Clear the selection
      selectedPiece = null;
      showInstructions("Select a piece from your available pieces");
      document
        .querySelectorAll(".game-piece.selected")
        .forEach((p) => p.classList.remove("selected"));

      return response.text();
    })
    .catch((error) => {
      console.error("Error placing piece:", error);
      showMessage("Error placing piece: " + error.message);
      showInstructions("Something went wrong. Please try again.");
    });
}

/**
 * Initialize WebSocket connection
 */
function initializeWebSocket() {
  if (!gameId) return;

  // Connect directly since scripts are now loaded in the HTML
  connectWebSocket();
}

/**
 * Connect to WebSocket
 */
function connectWebSocket() {
  console.log("Attempting to connect to WebSocket...");

  try {
    const socket = new SockJS("/ws-blokus");
    console.log("SockJS instance created");

    stompClient = Stomp.over(socket);
    console.log("STOMP client created");

    // Debug mode for STOMP client
    stompClient.debug = function (str) {
      console.log("STOMP Debug:", str);
    };

    console.log("Connecting to STOMP...");
    stompClient.connect(
      {},
      function (frame) {
        console.log("Connected to WebSocket: " + frame);

        // Subscribe to game updates
        const subscription = "/topic/games/" + gameId;
        console.log("Subscribing to:", subscription);

        stompClient.subscribe(subscription, function (message) {
          console.log("Received WebSocket message:", message);
          try {
            const parsedData = JSON.parse(message.body);
            handleGameUpdate(parsedData);
          } catch (e) {
            console.error("Error parsing WebSocket message:", e);
          }
        });

        console.log("WebSocket setup complete");
      },
      function (error) {
        console.error("WebSocket connection error:", error);
        // Reconnect after a delay
        setTimeout(connectWebSocket, 5000);
      }
    );
  } catch (e) {
    console.error("Exception in WebSocket setup:", e);
  }
}

/**
 * Handle game updates received via WebSocket
 */
function handleGameUpdate(update) {
  console.log("Received game update:", update);

  switch (update.type) {
    case "PIECE_PLACED":
      // Only apply updates from other players, since we already see our own moves
      const currentUserName = document
        .querySelector(".current-user")
        ?.getAttribute("data-username");
      if (update.data.playerUsername !== currentUserName) {
        handlePiecePlaced(update.data);
      }
      break;
    case "NEXT_TURN":
      handleNextTurn(update.data);
      break;
    case "GAME_OVER":
      handleGameOver(update.data);
      break;
    default:
      console.log("Unknown update type:", update.type);
  }

  // Show update message
  showMessage(update.message);
}

/**
 * Handle a piece placed update
 */
function handlePiecePlaced(data) {
  // Mark the piece as used in the inventory
  const pieceElements = document.querySelectorAll(
    `.game-piece[data-piece-id="${data.pieceId}"][data-piece-color="${data.pieceColor}"]`
  );
  pieceElements.forEach((piece) => {
    piece.classList.add("used");
  });

  // Add the piece to the board (ignore rotation and flip values)
  addPieceToBoard(data.pieceId, data.pieceColor, data.x, data.y);

  console.log(
    `Piece ${data.pieceId} placed by ${data.playerUsername} at (${data.x},${data.y})`
  );
}

/**
 * Add a piece to the board
 */
function addPieceToBoard(pieceId, pieceColor, x, y) {
  console.log(`Adding piece ${pieceId} to board at (${x},${y})`);

  try {
    // Find the piece shape
    const pieceElement = document.querySelector(
      `.game-piece[data-piece-id="${pieceId}"][data-piece-color="${pieceColor}"]`
    );

    if (!pieceElement) {
      console.warn(
        `Could not find piece with id ${pieceId} and color ${pieceColor}. Creating a fallback piece.`
      );
      // Create a basic square piece as fallback
      applyBasicPiece(x, y, pieceColor);
      return;
    }

    // Find the target cell
    const targetCell = document.querySelector(
      `.board-cell[data-x="${x}"][data-y="${y}"]`
    );

    if (!targetCell) {
      console.error(`Could not find cell at coordinates (${x},${y})`);
      return;
    }

    // Get the piece shape from the original piece
    const shape = [];
    const originalRows = pieceElement.querySelectorAll(".piece-row");
    originalRows.forEach((row) => {
      const rowCells = [];
      row.querySelectorAll(".piece-cell").forEach((cell) => {
        rowCells.push(cell.classList.contains(`${pieceColor}`));
      });
      shape.push(rowCells);
    });

    console.log("Original piece shape:", shape);

    // Fix the orientation to match server's expected format
    const processedShape = correctPieceOrientation(shape);
    console.log("Corrected piece shape:", processedShape);

    // Apply the shape to the board
    applyShapeToBoard(processedShape, x, y, pieceColor);

    console.log(
      `Piece ${pieceId} (${pieceColor}) successfully placed at (${x},${y})`
    );
  } catch (error) {
    console.error("Error adding piece to board:", error);
    // Try to place a fallback piece at least
    applyBasicPiece(x, y, pieceColor);
  }
}

/**
 * Apply a basic piece (1x1) to the board as a fallback
 */
function applyBasicPiece(x, y, color) {
  // Find the cell at the given coordinates
  const cell = document.querySelector(
    `.board-cell[data-x="${x}"][data-y="${y}"]`
  );

  if (cell) {
    // Mark the cell as occupied
    cell.classList.add("occupied");
    cell.classList.add(color);
    console.log(`Applied fallback piece at (${x},${y}) with color ${color}`);
  }
}

/**
 * Apply a shape to the board starting from the specified coordinates
 */
function applyShapeToBoard(shape, startX, startY, color) {
  const height = shape.length;
  const width = shape[0] ? shape[0].length : 0;

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      if (shape[y][x]) {
        const boardX = startX + x;
        const boardY = startY + y;

        // Find the cell at these coordinates
        const cell = document.querySelector(
          `.board-cell[data-x="${boardX}"][data-y="${boardY}"]`
        );

        if (cell) {
          cell.style.backgroundColor = color;
          cell.classList.add("occupied");

          // Store the color on the cell as a data attribute for validation
          cell.setAttribute("data-color", color);
        }
      }
    }
  }
}

/**
 * Handle a next turn update
 */
function handleNextTurn(data) {
  // Update the current player display
  const currentPlayerSpan = document.querySelector(".game-info span");
  if (currentPlayerSpan) {
    currentPlayerSpan.textContent = data.nextPlayerUsername;

    // Check if it's the current user's turn
    const currentUser = document
      .querySelector(".current-user")
      ?.getAttribute("data-username");
    if (currentUser === data.nextPlayerUsername) {
      currentPlayerSpan.classList.add("current-player-active");
    } else {
      currentPlayerSpan.classList.remove("current-player-active");
    }
  }
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
 * Show a message
 */
function showMessage(message) {
  // Create or update a message element
  let messageElement = document.querySelector(".game-message");

  if (!messageElement) {
    messageElement = document.createElement("div");
    messageElement.className = "game-message";
    document.querySelector(".game-container").appendChild(messageElement);
  }

  messageElement.textContent = message;
  messageElement.style.display = "block";

  // Hide the message after a delay
  setTimeout(() => {
    messageElement.style.display = "none";
  }, 5000);
}

/**
 * Show game instructions to the user based on current state
 */
function showInstructions(message) {
  const instructionsElement = document.querySelector(".game-instructions");
  if (instructionsElement) {
    instructionsElement.textContent = message;
  }
}
