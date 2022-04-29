
// Press e before clicking a point in the grid to set the entrance
var entranceKey = false;

window.onkeyup = function(e) {
    if (e.keyCode != 69) {
        return;
    }
    entranceKey = false;;
}

window.onkeydown = function(e) {
    if (e.keyCode != 69) {
        return;
    }
    entranceKey = true;
}

function loadUserId() {

    if (typeof (Storage) != undefined) {
        return sessionStorage.getItem('user-id');
    }
    return null;
}

function loadUserToken() {

    if (typeof (Storage) != undefined) {
        return sessionStorage.getItem('user-token');
    }
    return null;
}

function defineTextElement(text) {

    var textElement = document.createElement('P');
    textElement.innerText = text;

    return textElement;
}

function defineButtonElement(label, callback) {

    var buttonElement = document.createElement('BUTTON');

    buttonElement.type = "BUTTON";
    buttonElement.classList.add('maze-button');
    buttonElement.innerHTML = label;
    buttonElement.addEventListener('click', callback, false);

    return buttonElement;
}

function assignWallSet(maze) {

    if (maze.wallSet != null) {
        return;
    }

    var wallSet = new Set();

    if (maze.walls != null) {
        for (var wall of maze.walls) {
            wallSet.add(wall);
        }
    }

    maze.wallSet = wallSet;
}

function computePathSet(path) {

    var pathSet = new Set();

    for (var point of path) {
        pathSet.add(wall);
    }
    return pathSet;
}

function nameGridPoint(col, row) {

    return String.fromCharCode(64 + col) + row;
}

function typeGridPoint(name, maze, pathSet) {

    if (maze.mazeId == null) {
        // Maze not submitted yet
        return "Input";
    }

    if (maze.entrance == name) {
        return "Entrance";
    }

    if ((pathSet != null) && (pathSet.has(name))) {
        return "Path";
    }

    if (maze.wallSet.has(name)) {
        return "Wall";
    }

    return "Empty";
}

function toggleEntrancePointElement(pointElement, maze) {

    entranceKey = false;

    // Remove this entrance if it was there
    if (maze.entranceElement == pointElement) {
        removeEntrance(pointElement, maze);
        return;
    }

    // Add this entrance to the maze
    addEntrance(pointElement, maze);
}

function toggleWallPointElement(pointElement, maze) {

    // Remove this wall if it was there
    if (pointElement.classList.contains('maze-point-wall')) {
        pointElement.classList.remove('maze-point-wall');
        removeWall(pointElement.name, maze);
        return;
    }

    // add this wall to the maze
    pointElement.classList.add('maze-point-wall');
    addWall(pointElement.name, maze);
}

function togglePointElement(pointElement, maze) {

    if (entranceKey) {
        toggleEntrancePointElement(pointElement, maze);
    } else {
        toggleWallPointElement(pointElement, maze);
    }
}

function definePointElement(name, type, col, maze) {

    var pointElement = document.createElement('DIV');
    pointElement.classList.add('maze-point');

    pointElement.name = name;

    pointElement.style.gridColumn = col + " / span 1";

    switch (type) {
        case "Input":
            pointElement.classList.add('maze-point-input');
            pointElement.addEventListener('click', function() {
                togglePointElement(pointElement, maze);
            });
            break;
        case "Empty":
            break;
        case "Path":
            pointElement.classList.add('maze-point-path');
            break;
        case "Wall":
            pointElement.classList.add('maze-point-wall');
            break;
        case "Entrance":
            pointElement.classList.add('maze-point-entrance');
            break;
    }

    return pointElement;
}

function refreshGridElement(maze) {

    var gridElement = maze.gridElement;

    if (gridElement == null) {
        return;
    }

    while (gridElement.childElementCount > 0) {
        gridElement.removeChild(gridElement.lastChild);
    }

    if (maze.gridSize == null) {
        return;
    }

    var gridArray = maze.gridSize.split("x");
    if (gridArray.length != 2) {
        return;
    }

    var gridCol = parseInt(gridArray[0]);
    var gridRow = parseInt(gridArray[1]);

    gridElement.style.gridTemplateColumns = "30px ".repeat(gridCol);
    gridElement.style.gridTemplateRows = "30px ".repeat(gridRow);

    // Compute the set of walls for this Maze
    assignWallSet(maze);

    var pathSet;

    if (maze.minPathSet != null) {
        // Display the Min Path
        pathSet = maze.minPathSet;
    } else if (maze.maxPathSet != null) {
        // Display the Max path
        pathSet = maze.maxPathSet;
    } else {
        // No Path to display
        pathSet = null;
    }

    for (let row = 1; row <= gridRow; row++) {
        for (let col = 1; col <= gridCol; col++) {

            var name = nameGridPoint(col, row);
            var type = typeGridPoint(name, maze, pathSet);

            var pointElement = definePointElement(name, type, col, maze);
            gridElement.appendChild(pointElement);
        }
    }
}

function refreshActionsElement(maze) {

    var actionsElement = maze.actionsElement;

    if (actionsElement == null) {
        return;
    }

    while (actionsElement.childElementCount > 0) {
        actionsElement.removeChild(actionsElement.lastChild);
    }

    if (maze.gridSize == null) {
        // Maze not yet created
        var createButton = defineButtonElement("CREATE", function() {
            createMaze(maze);
        });
        actionsElement.appendChild(createButton);
        return;
    }

    if (maze.mazeId == null) {
        // Maze not submitted yet for this user
        var submitButton = defineButtonElement("SUBMIT", function() {
            submitMaze(maze);
        });
        actionsElement.appendChild(submitButton);
        return;
    }

    if ((maze.minPathSet != null) || (maze.maxPathSet != null)) {
        // A path is already displayed
        var clearPathButton = defineButtonElement("CLEAR PATH", function() {
            clearPath(maze);
        });
        actionsElement.appendChild(clearPathButton);
        return;
    }

    var solveMinPathButton = defineButtonElement("SOLVE MIN PATH", function() {
        solveMinPath(maze);
    });
    actionsElement.appendChild(solveMinPathButton);

    var solveMaxPathButton = defineButtonElement("SOLVE MAX PATH", function() {
        solveMaxPath(maze);
    });
    actionsElement.appendChild(solveMaxPathButton);
}

function refreshMaze(maze) {

    refreshGridElement(maze);
    refreshActionsElement(maze);
}

function createMaze(maze) {

    var gridCol;
    var gridRow;

    var gridSize = prompt("Enter the grid size for the maze, such as 5x5", "10x10");
    if (gridSize == null) {
        return;
    }

    try {
        var gridArray = gridSize.split("x");
        if (gridArray.length != 2) {
            return;
        }

        gridCol = parseInt(gridArray[0]);
        gridRow = parseInt(gridArray[1]);

        if ((gridCol < 1) || (gridRow < 1)) {
            alert("Grid Size does not have the proper format such as 10x10");
            return;
        }

    } catch (error) {
        alert("Grid Size does not have the proper format such as 10x10");
        return;
    }

    maze.gridSize = gridCol + "x" + gridRow;

    // Refresh the Maze to now didplay the grid of this new maze
    refreshMaze(maze);
}

function submitMaze(maze) {

    if ((maze.gridSize == null) || (maze.wallSet == null)) {
        alert("Maze grid size or wallSet are not defined");
        return;
    }

    if (maze.entrance == null) {
        alert("Maze entrance is not defined.\n\nHold the e key while selecting a location");
        return;
    }

    var entrance = maze.entrance;
    var gridSize = maze.gridSize;
    var walls = Array.from(maze.wallSet);

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/maze", true);
    xhr.setRequestHeader('Content-Type', "application/json;charset=UTF-8");
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader("User-Token", loadUserToken());

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {

                var mazeCreation = JSON.parse(xhr.responseText);
                if (mazeCreation.error != null) {
                    alert("Creating maze failed :\n\n" + mazeCreation.error);
                    return;
                }

                // Reload the page with this new maze
                location.reload();
            } else {
                alert("Failure to create maze. Error = " + xhr.status);
            }
        }
    };

    var postMaze = {
        entrance: entrance,
        gridSize: gridSize,
        walls: walls
    }

    xhr.send(JSON.stringify(postMaze));
}

function addWall(wall, maze) {

    maze.wallSet.add(wall);
}

function removeWall(wall, maze) {

    maze.wallSet.delete(wall);
}

function addEntrance(entranceElement, maze) {

    if (maze.entranceElement != null) {
        maze.entranceElement.classList.remove('maze-point-entrance');
    }
    maze.entranceElement = entranceElement;
    maze.entranceElement.classList.add('maze-point-entrance');
    maze.entrance = entranceElement.name;
}

function removeEntrance(_entranceElement, maze) {

    if (maze.entranceElement != null) {
        maze.entranceElement.classList.remove('maze-point-entrance');
        maze.entranceElement = null;
    }
    maze.entrance = null;
}

function clearPath(maze) {

    maze.minPathSet = null;
    maze.maxPathSet = null;

    refreshMaze(maze);
}

function buildPathSet(mazeSolution) {

    if ((mazeSolution == null) || (mazeSolution.path == null)) {
        return null;
    }

    var pathSet = new Set();

    for (var path of mazeSolution.path) {
        pathSet.add(path);
    }

    return pathSet;
}

function solveMinPath(maze) {

    maze.minPathSet = null;
    maze.maxPathSet = null;

    var path = "/maze/" + maze.mazeId + "/solution?steps=min";

    var xhr = new XMLHttpRequest();
    xhr.open("GET", path, true);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader("User-Token", loadUserToken());

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {
                var mazeSolution = JSON.parse(xhr.responseText);

                if (mazeSolution.error != null) {
                    alert("Solving maze failed :\n\n" + mazeSolution.error);
                    return;
                }

                // Build the Path Set from the solution
                var pathSet = buildPathSet(mazeSolution);
                if (pathSet == null) {
                    alert("Path solution is not defined");
                    return;
                }
                maze.minPathSet = pathSet;

                // Refresh the display of this maze
                refreshMaze(maze);
            } else {
                alert("Failure to solve min path for maze. Error = " + xhr.status);
            }
        }
    };

    xhr.send();
}

function solveMaxPath(maze) {

    maze.minPathSet = null;
    maze.maxPathSet = null;

    var path = "/maze/" + maze.mazeId + "/solution?steps=max";

    var xhr = new XMLHttpRequest();
    xhr.open("GET", path, true);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader("User-Token", loadUserToken());

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {
                var mazeSolution = JSON.parse(xhr.responseText);

                if (mazeSolution.error != null) {
                    alert("Solving maze failed :\n\n" + mazeSolution.error);
                    return;
                }

                // Build the Path Set from the solution
                var pathSet = buildPathSet(mazeSolution);
                if (pathSet == null) {
                    alert("Path solution is not defined");
                    return;
                }
                maze.maxPathSet = pathSet;

                // Refresh the display of this maze
                refreshMaze(maze);
            } else {
                alert("Failure to solve max path for maze. Error = " + xhr.status);
            }
        }
    };

    xhr.send();
}

function displayMaze(maze, mazeTableBody) {

    // Create a GridElement for this maze
    var gridElement = document.createElement('DIV');
    gridElement.classList.add('maze-grid');
    maze.gridElement = gridElement;

    // Create an ActionsElement for this maze
    var actionsElement = document.createElement('DIV');
    actionsElement.classList.add('maze-actions');
    maze.actionsElement = actionsElement;

    // Create a new row
    var row = mazeTableBody.insertRow(-1);

    cell = row.insertCell(-1);
    if (maze.mazeId != null) {
        cell.appendChild(defineTextElement("#" + maze.mazeId));
    }

    cell = row.insertCell(-1);
    cell.appendChild(gridElement);

    cell = row.insertCell(-1);
    cell.appendChild(actionsElement);

    // Refresh the Maze to display the current state
    refreshMaze(maze);
}

function displayMazeArray(mazeArray) {

    var mazeTableBody = document.getElementById("mazeTableBodyId")

    // First row enables the creation of a new maze
    var newMaze = {};
    displayMaze(newMaze, mazeTableBody);

    // Display all mazes already created for this user
    for (var maze of mazeArray) {
        displayMaze(maze, mazeTableBody);
    }
}

function loadMazes() {

    var xhr = new XMLHttpRequest();
    xhr.open("GET", "/maze", true);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader("User-Token", loadUserToken());

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {
                var mazeArray = JSON.parse(xhr.responseText);
                displayMazeArray(mazeArray);
            } else {
                alert("Failure to get mazes");
            }
        }
    };
    xhr.send();
}

function displayUserName() {

    var usernameElement = document.getElementById("usernameId");
    usernameElement.innerText = "USER : " + loadUserId();
}

function displayPage() {

    displayUserName();

    loadMazes();
}

displayPage();
