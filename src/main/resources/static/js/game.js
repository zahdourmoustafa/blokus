// Game board and pieces functionality
document.addEventListener("DOMContentLoaded", function () {
  // Get game ID from URL - Fix extraction to get the numeric ID
  const path = window.location.pathname;
  const segments = path.split("/").filter(Boolean);
  // Find the game ID (segment before "play")
  const gameId = segments[segments.indexOf("play") - 1];

  console.log("Game ID extracted:", gameId);

  // Board and pieces containers
  const gameBoard = document.getElementById("game-board");
  const piecesContainer = document.getElementById("pieces-container");

  // Piece manipulation
  let selectedPiece = null;
  let selectedPieceId = null;
  let rotationValue = 0;
  let isFlipped = false;

  // Player variables
  let playerPieces = [];
  let playerIndex = 0; // Default to first player (blue)
  let playerColor = "blue";

  // Define standard Blokus pieces for each color if API doesn't return them
  const standardPieces = [
    // 1-square pieces (monomino)
    { id: 1, shape: [[1]], color: "blue", type: "I1" },

    // 2-square pieces (domino)
    { id: 2, shape: [[0], [1], [1]], color: "blue", type: "I2" }, // Fixed to be vertical like in image

    // 3-square pieces (triominos)
    { id: 3, shape: [[1], [1], [1]], color: "blue", type: "I3" }, // Vertical
    {
      id: 4,
      shape: [
        [0, 0],
        [1, 0],
        [1, 1],
      ],
      color: "blue",
      type: "L3",
    },

    // 4-square pieces (tetrominos)
    {
      id: 5,
      shape: [
        [1, 0, 0, 0],
        [1, 0, 0, 0],
        [1, 0, 0, 0],
        [1, 0, 0, 0],
      ],
      color: "blue",
      type: "I4",
    }, // Modified for 4x4 grid
    {
      id: 6,
      shape: [
        [0, 0, 1],
        [0, 0, 1],
        [0, 1, 1],
      ],
      color: "blue",
      type: "L4",
    },
    {
      id: 7,
      shape: [
        [0, 0],
        [1, 1],
        [1, 1],
      ],
      color: "blue",
      type: "O4",
    },
    {
      id: 8,
      shape: [
        [0, 0, 0],
        [1, 1, 0],
        [0, 1, 1],
      ],
      color: "blue",
      type: "Z4",
    },
    {
      id: 9,
      shape: [
        [1, 1],
        [0, 1],
        [1, 1],
      ],
      color: "blue",
      type: "T4",
    },

    // 5-square pieces (pentominos)
    {
      id: 10,
      shape: [
        [0, 1, 0],
        [0, 1, 0],
        [1, 1, 1],
      ],
      color: "blue",
      type: "I5",
    }, // Modified to fit in 3x3
    {
      id: 11,
      shape: [
        [1, 0, 0],
        [1, 0, 0],
        [1, 1, 1],
      ],
      color: "blue",
      type: "L5",
    },
    {
      id: 12,
      shape: [
        [1, 1, 0],
        [0, 1, 1],
        [0, 0, 1],
      ],
      color: "blue",
      type: "U5",
    },
    {
      id: 13,
      shape: [
        [1, 0, 0],
        [1, 1, 1],
        [0, 0, 1],
      ],
      color: "blue",
      type: "T5",
    },
    {
      id: 14,
      shape: [
        [1, 0, 0],
        [1, 1, 1],
        [0, 1, 0],
      ],
      color: "blue",
      type: "Z5",
    },
    {
      id: 15,
      shape: [
        [0, 1, 0, 0],
        [0, 1, 0, 0],
        [1, 1, 0, 0],
        [1, 0, 0, 0],
      ],
      color: "blue",
      type: "N5",
    },
    {
      id: 16,
      shape: [
        [0, 1],
        [1, 1],
        [1, 1],
      ],
      color: "blue",
      type: "P5",
    },
    {
      id: 17,
      shape: [
        [1, 0, 0, 0, 0],
        [1, 0, 0, 0, 0],
        [1, 0, 0, 0, 0],
        [1, 0, 0, 0, 0],
        [1, 0, 0, 0, 0],
      ],
      color: "blue",
      type: "W5",
    },
    {
      id: 18,
      shape: [
        [1, 0, 0, 0],
        [1, 1, 0, 0],
        [1, 0, 0, 0],
        [1, 0, 0, 0],
      ],
      color: "blue",
      type: "F5",
    },
    {
      id: 19,
      shape: [
        [0, 1, 0],
        [1, 1, 1],
        [0, 1, 0],
      ],
      color: "blue",
      type: "X5",
    },
    {
      id: 20,
      shape: [
        [1, 0],
        [1, 1],
        [1, 0],
      ],
      color: "blue",
      type: "Y5",
    },
    {
      id: 21,
      shape: [
        [0, 1, 0, 0],
        [0, 1, 0, 0],
        [0, 1, 0, 0],
        [1, 1, 0, 0],
      ],
      color: "blue",
      type: "V5",
    }, // 4x4 grid for V5
  ];

  // Initialize the game state
  initializeGame();

  // Function to initialize the game
  function initializeGame() {
    // Create empty game board immediately
    createEmptyGameBoard();

    // Try to fetch game state
    fetchGameState();

    // Setup event listeners for rotation, flip and pass buttons
    const rotateBtn = document.getElementById("rotate-btn");
    const flipBtn = document.getElementById("flip-btn");
    const passBtn = document.getElementById("pass-btn");

    if (rotateBtn) rotateBtn.addEventListener("click", rotatePiece);
    if (flipBtn) flipBtn.addEventListener("click", flipPiece);
    if (passBtn) passBtn.addEventListener("click", passTurn);

    // Try to initialize pieces if needed
    initializePiecesIfNeeded();

    // Setup interval to refresh game state
    setInterval(fetchGameState, 5000);
  }

  // Function to initialize pieces if they haven't been initialized yet
  function initializePiecesIfNeeded() {
    // First try to fetch pieces
    fetch(`/api/games/${gameId}/pieces`)
      .then((response) => response.json())
      .then((data) => {
        console.log("Checking if pieces initialization is needed:", data);
        // If no pieces are found, call the initialize endpoint
        if (!data || data.length === 0 || data.error) {
          console.log("No pieces found, initializing game...");
          return fetch(`/api/games/${gameId}/initialize`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
            },
          });
        }
        return null;
      })
      .then((response) => {
        if (response) return response.json();
        return null;
      })
      .then((data) => {
        if (data) {
          console.log("Game initialized:", data);
          // Fetch game state again after initialization
          fetchGameState();
        }
      })
      .catch((error) => {
        console.error("Error initializing pieces:", error);
      });
  }

  // Function to create an empty game board
  function createEmptyGameBoard() {
    if (!gameBoard) return;

    // Clear existing board
    gameBoard.innerHTML = "";

    // Create empty grid
    for (let y = 0; y < 20; y++) {
      for (let x = 0; x < 20; x++) {
        const cell = document.createElement("div");
        cell.classList.add("cell");
        cell.dataset.x = x;
        cell.dataset.y = y;

        // Keep the click event for piece placement
        cell.addEventListener("click", function () {
          if (selectedPiece) {
            placePiece(x, y);
          }
        });

        gameBoard.appendChild(cell);
      }
    }
  }

  // Function to fetch game state from server
  function fetchGameState() {
    fetch(`/api/games/${gameId}/state`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Network response was not ok");
        }
        return response.json();
      })
      .then((data) => {
        updateGameBoard(data.boardState);
        updatePlayerInfo(
          data.currentPlayer,
          data.currentPlayerColor,
          data.isPlayerTurn
        );
        if (data.isPlayerTurn) {
          fetchPlayerPieces();
        }
      })
      .catch((error) => {
        console.error("Error fetching game state:", error);
      });
  }

  // Function to fetch player pieces
  function fetchPlayerPieces() {
    console.log("Fetching player pieces for game ID:", gameId);

    fetch(`/api/games/${gameId}/state`)
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        return response.json();
      })
      .then((data) => {
        console.log("Received game state data:", data);

        // Check if we have allPlayerPieces data
        if (data && data.allPlayerPieces && data.allPlayerPieces.length > 0) {
          // Update each player's pieces
          data.allPlayerPieces.forEach((playerData) => {
            const color = playerData.color.toLowerCase();
            const container = document.getElementById(
              `${color}-pieces-container`
            );

            if (container) {
              container.innerHTML = "";

              // Skip if no available pieces
              if (
                !playerData.availablePieces ||
                playerData.availablePieces.length === 0
              ) {
                return;
              }

              // Create piece elements
              playerData.availablePieces.forEach((piece) => {
                const pieceElement = createPieceElement(piece, color);
                container.appendChild(pieceElement);
              });
            }
          });
        } else if (data.isPlayerTurn) {
          // Fallback to getting just the current player's pieces
          fetch(`/api/games/${gameId}/pieces`)
            .then((response) => response.json())
            .then((pieces) => {
              console.log("Received pieces data:", pieces);

              if (pieces && pieces.length > 0) {
                playerPieces = pieces;
                updatePiecesDisplay();
              } else {
                console.log(
                  "No pieces received from API, using standard pieces"
                );
                updatePiecesDisplayWithStandard();
              }
            })
            .catch((error) => {
              console.error("Error fetching pieces:", error);
              updatePiecesDisplayWithStandard();
            });
        }
      })
      .catch((error) => {
        console.error("Error fetching game state:", error);
        // Fallback to standard pieces
        updatePiecesDisplayWithStandard();
      });
  }

  // Helper function to create a piece element
  function createPieceElement(piece, color) {
    const pieceElement = document.createElement("div");
    pieceElement.className = `piece piece-${color}`;
    pieceElement.dataset.pieceId = piece.id;
    pieceElement.dataset.pieceType = piece.type || "Unknown";

    // Make piece clickable with proper styling
    pieceElement.style.cursor = "pointer";

    // Create a grid for the piece shape
    const shape = piece.shape || [];
    const maxRows = shape.length;
    const maxCols = Math.max(...shape.map((row) => row.length));

    // Use a 4x4 grid for specified pieces, 3x3 for others
    const specialPieces = ["I4", "N5", "F5", "V5", "W5"];
    const gridSize =
      piece.type === "W5" ? 5 : specialPieces.includes(piece.type) ? 4 : 3;

    // Set the grid size based on shape
    pieceElement.style.gridTemplateRows = `repeat(${gridSize}, 1fr)`;
    pieceElement.style.gridTemplateColumns = `repeat(${gridSize}, 1fr)`;

    // Create the cells
    for (let y = 0; y < gridSize; y++) {
      for (let x = 0; x < gridSize; x++) {
        const cell = document.createElement("div");
        cell.className = "piece-cell";

        // Center or scale the piece in the grid
        let offsetY = 0;
        let offsetX = 0;

        if (maxRows <= 3 && maxCols <= 3) {
          // Center smaller pieces
          offsetY = Math.floor((gridSize - maxRows) / 2);
          offsetX = Math.floor((gridSize - maxCols) / 2);
        }

        // Scale down larger pieces or center smaller ones
        const scaledY = Math.floor((y * maxRows) / gridSize);
        const scaledX = Math.floor((x * maxCols) / gridSize);

        // For pieces that fit within 3x3
        if (maxRows <= 3 && maxCols <= 3) {
          // Add filled class if the cell is part of the piece shape
          if (
            y >= offsetY &&
            y < offsetY + maxRows &&
            x >= offsetX &&
            x < offsetX + maxCols &&
            shape[y - offsetY] &&
            shape[y - offsetY][x - offsetX] === 1
          ) {
            cell.classList.add("piece-cell-filled");
          }
        } else {
          // For larger pieces, scale down to fit
          if (
            scaledY < maxRows &&
            scaledX < maxCols &&
            shape[scaledY] &&
            shape[scaledY][scaledX] === 1
          ) {
            cell.classList.add("piece-cell-filled");
          }
        }

        pieceElement.appendChild(cell);
      }
    }

    // Add click event only if it's the current player's container
    if (color === playerColor) {
      pieceElement.addEventListener("click", () => {
        document
          .querySelectorAll(".piece")
          .forEach((p) => p.classList.remove("selected"));
        pieceElement.classList.add("selected");
        selectedPiece = pieceElement;
        selectedPieceId = piece.id;
        console.log(
          `Selected piece ${selectedPieceId} of type ${piece.type || "Unknown"}`
        );
      });
    }

    return pieceElement;
  }

  // Function to update pieces display with standard pieces
  function updatePiecesDisplayWithStandard() {
    console.log("Displaying standard pieces");
    // Clone the standard pieces and assign the player's color
    const gameColors = ["blue", "yellow", "red", "green"];
    const playerColorToUse = playerColor || gameColors[playerIndex] || "blue";

    console.log("Using color for pieces:", playerColorToUse);

    playerPieces = standardPieces.map((piece) => {
      return {
        ...piece,
        color: playerColorToUse,
        id: piece.id,
      };
    });

    // Make sure we display the pieces in the correct container
    updatePiecesDisplay();
  }

  // Function to update game board
  function updateGameBoard(boardState) {
    if (!gameBoard) return;

    // Clear existing board
    gameBoard.innerHTML = "";

    // Create cell for each position on the board
    for (let y = 0; y < 20; y++) {
      for (let x = 0; x < 20; x++) {
        const cell = document.createElement("div");
        cell.classList.add("cell");

        // Set color based on board state
        const cellValue = boardState[y][x];
        if (cellValue === 1) {
          cell.classList.add("cell-blue");
        } else if (cellValue === 2) {
          cell.classList.add("cell-yellow");
        } else if (cellValue === 3) {
          cell.classList.add("cell-red");
        } else if (cellValue === 4) {
          cell.classList.add("cell-green");
        }

        // Add data attributes for position
        cell.dataset.x = x;
        cell.dataset.y = y;

        // Keep the click event for piece placement
        cell.addEventListener("click", function () {
          if (selectedPiece) {
            placePiece(x, y);
          }
        });

        gameBoard.appendChild(cell);
      }
    }
  }

  // Function to update player information
  function updatePlayerInfo(currentPlayer, currentPlayerColor, isPlayerTurn) {
    // Update current player indicator
    const playerCards = document.querySelectorAll(".player-card");
    playerCards.forEach((card) => {
      card.classList.remove("current");
    });

    const currentPlayerCard = document.querySelector(
      `.player-${currentPlayerColor.toLowerCase()}`
    );
    if (currentPlayerCard) {
      currentPlayerCard.classList.add("current");
    }

    // Determine player index and color based on current player
    const gameColors = ["blue", "yellow", "red", "green"];
    playerColor = currentPlayerColor.toLowerCase();
    playerIndex = gameColors.indexOf(playerColor);
    if (playerIndex === -1) playerIndex = 0;

    console.log(
      "Current player color:",
      playerColor,
      "Player index:",
      playerIndex
    );

    // Update turn status
    const turnStatus = document.getElementById("turn-status");
    if (turnStatus) {
      if (isPlayerTurn) {
        turnStatus.textContent = "Votre tour";
        turnStatus.classList.add("text-success");
        turnStatus.classList.remove("text-danger");
        const controlsContainer = document.getElementById("controls-container");
        if (controlsContainer) controlsContainer.classList.remove("d-none");

        // Fetch pieces when it's our turn
        fetchPlayerPieces();
      } else {
        turnStatus.textContent = `En attente de ${currentPlayer}`;
        turnStatus.classList.add("text-danger");
        turnStatus.classList.remove("text-success");
        const controlsContainer = document.getElementById("controls-container");
        if (controlsContainer) controlsContainer.classList.add("d-none");
      }
    }
  }

  // Function to update pieces display
  function updatePiecesDisplay() {
    // Instead of looking for a single container, get the player-specific container
    const playerColorToUse = playerColor || "blue";
    const piecesContainer = document.getElementById(
      `${playerColorToUse}-pieces-container`
    );

    console.log(
      "Updating pieces for",
      playerColorToUse,
      "container found:",
      piecesContainer !== null
    );
    console.log("Pieces data:", playerPieces);

    if (!piecesContainer) {
      console.error(`Container for ${playerColorToUse} pieces not found`);
      return;
    }

    piecesContainer.innerHTML = "";

    playerPieces.forEach((piece) => {
      // Skip pieces that have been placed
      if (piece.posx !== undefined && piece.posy !== undefined) {
        return;
      }

      const pieceElement = document.createElement("div");
      pieceElement.className = `piece piece-${piece.color}`;
      pieceElement.dataset.pieceId = piece.id;
      pieceElement.dataset.pieceType = piece.type || "Unknown";

      // Make piece clickable with proper styling
      pieceElement.style.cursor = "pointer";

      // Create a grid for the piece shape - more compact
      const shape = piece.shape || [];
      const maxRows = shape.length;
      const maxCols = Math.max(...shape.map((row) => row.length));

      // Use a 4x4 grid for specified pieces, 3x3 for others
      const specialPieces = ["I4", "N5", "F5", "V5", "W5"];
      const gridSize =
        piece.type === "W5" ? 5 : specialPieces.includes(piece.type) ? 4 : 3;

      // Set the grid size based on shape
      pieceElement.style.gridTemplateRows = `repeat(${gridSize}, 1fr)`;
      pieceElement.style.gridTemplateColumns = `repeat(${gridSize}, 1fr)`;

      // Create the cells - more compact with centered content
      for (let y = 0; y < gridSize; y++) {
        for (let x = 0; x < gridSize; x++) {
          const cell = document.createElement("div");
          cell.className = "piece-cell";

          // Center or scale the piece in the 3x3 grid
          let offsetY = 0;
          let offsetX = 0;

          if (maxRows <= 3 && maxCols <= 3) {
            // Center smaller pieces
            offsetY = Math.floor((gridSize - maxRows) / 2);
            offsetX = Math.floor((gridSize - maxCols) / 2);
          }

          // Scale down larger pieces or center smaller ones
          const scaledY = Math.floor((y * maxRows) / gridSize);
          const scaledX = Math.floor((x * maxCols) / gridSize);

          // For pieces that fit within 3x3
          if (maxRows <= 3 && maxCols <= 3) {
            // Add filled class if the cell is part of the piece shape
            if (
              y >= offsetY &&
              y < offsetY + maxRows &&
              x >= offsetX &&
              x < offsetX + maxCols &&
              shape[y - offsetY] &&
              shape[y - offsetY][x - offsetX] === 1
            ) {
              cell.classList.add("piece-cell-filled");
            }
          } else {
            // For larger pieces, scale down to fit
            if (
              scaledY < maxRows &&
              scaledX < maxCols &&
              shape[scaledY] &&
              shape[scaledY][scaledX] === 1
            ) {
              cell.classList.add("piece-cell-filled");
            }
          }

          pieceElement.appendChild(cell);
        }
      }

      // Add regular click event for piece selection (for rotation/flip)
      pieceElement.addEventListener("click", () => {
        document
          .querySelectorAll(".piece")
          .forEach((p) => p.classList.remove("selected"));
        pieceElement.classList.add("selected");
        selectedPiece = pieceElement;
        selectedPieceId = piece.id;
        console.log(
          `Selected piece ${selectedPieceId} of type ${piece.type || "Unknown"}`
        );
      });

      piecesContainer.appendChild(pieceElement);
    });
  }

  // Function to select a piece
  function selectPiece(pieceElement) {
    // Deselect previously selected piece
    document
      .querySelectorAll(".piece")
      .forEach((p) => p.classList.remove("selected"));

    // Select new piece
    pieceElement.classList.add("selected");
    selectedPiece = pieceElement;
    selectedPieceId = pieceElement.dataset.pieceId;

    console.log(
      "Selected piece:",
      selectedPieceId,
      "of type",
      pieceElement.dataset.pieceType
    );

    // Reset rotation and flip
    rotationValue = 0;
    isFlipped = false;
  }

  // Function to rotate selected piece
  function rotatePiece() {
    if (!selectedPiece) return;

    rotationValue = (rotationValue + 90) % 360;
    updatePiecePreview();
  }

  // Function to flip selected piece
  function flipPiece() {
    if (!selectedPiece) return;

    isFlipped = !isFlipped;
    updatePiecePreview();
  }

  // Function to update piece preview
  function updatePiecePreview() {
    // Implementation depends on piece representation
    // This is a placeholder
    console.log(
      `Preview updated: rotation=${rotationValue}, flipped=${isFlipped}`
    );
  }

  // Function to place a piece
  function placePiece(x, y) {
    if (!selectedPiece) {
      console.error("No piece selected");
      return;
    }

    const pieceId = selectedPieceId;
    console.log(
      `Placing piece ${pieceId} at coordinates (${x}, ${y}) with rotation ${rotationValue} and flipped=${isFlipped}`
    );

    // Send move to server
    fetch(`/api/games/${gameId}/move`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        pieceId: pieceId,
        x: x,
        y: y,
        rotation: rotationValue,
        flipped: isFlipped,
      }),
    })
      .then((response) => {
        console.log("Move API response status:", response.status);
        if (!response.ok) {
          return response.json().then((data) => {
            console.error("API error details:", data);
            throw new Error(data.error || "Failed to place piece");
          });
        }
        return response.json();
      })
      .then((data) => {
        console.log("Move successful:", data);

        // Update board with new state
        updateGameBoard(data.boardState);

        // Clear selection
        selectedPiece.classList.remove("selected");
        selectedPiece = null;
        selectedPieceId = null;

        // Update game status
        updatePlayerInfo(data.currentPlayer, data.currentPlayerColor, false);

        // Fetch updated pieces
        fetchPlayerPieces();
      })
      .catch((error) => {
        console.error("Error placing piece:", error);
        alert(error.message);
      });
  }

  // Function to pass turn
  function passTurn() {
    fetch(`/api/games/${gameId}/skip`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
    })
      .then((response) => {
        if (!response.ok) {
          return response.json().then((data) => {
            throw new Error(data.error || "Failed to skip turn");
          });
        }
        return response.json();
      })
      .then((data) => {
        // Update game status
        updatePlayerInfo(data.currentPlayer, data.currentPlayerColor, false);
        fetchGameState();
      })
      .catch((error) => {
        console.error("Error skipping turn:", error);
        alert(error.message);
      });
  }
});
