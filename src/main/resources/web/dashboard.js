
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
    buttonElement.classList.add('soccer-button');
    buttonElement.innerHTML = label;
    buttonElement.addEventListener('click', callback, false);

    return buttonElement;
}

function editPlayerFirstName(player) {

    var playerFirstName = prompt("Enter the first name of the player", player.playerFirstName);
    if (playerFirstName == null) {
        return;
    }
    player.playerFirstName = playerFirstName;
    refreshPlayer(player);

    updatePlayer(player);
}

function editPlayerLastName(player) {

    var playerLastName = prompt("Enter the last name of the player", player.playerLastName);
    if (playerLastName == null) {
        return;
    }
    player.playerLastName = playerLastName;
    refreshPlayer(player);

    updatePlayer(player);
}

function editPlayerCountry(player) {

    var playerCountry = prompt("Enter the country of the player", player.playerCountry);
    if (playerCountry == null) {
        return;
    }
    player.playerCountry = playerCountry;
    refreshPlayer(player);

    updatePlayer(player);
}

function editPlayerTransferValue(player) {

    var playerTransferValue = prompt("Enter the transfer value of the player. 0 for no transfer", player.playerTransferValue);
    if ((playerTransferValue == null) || (Number.isInteger(Number(playerTransferValue)) == false) || (Number(playerTransferValue) < 0)) {
        return;
    }
    player.playerTransferValue = playerTransferValue;
    refreshPlayer(player);

    updatePlayer(player);
}

function updatePlayer(player) {

    var xhr = new XMLHttpRequest();
    xhr.open("PUT", "/player/" + player.playerId.uuid, true);
    xhr.setRequestHeader('Content-Type', "application/json;charset=UTF-8");
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader("User-Token", loadUserToken());

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {

                var updatePlayerOutcome = JSON.parse(xhr.responseText);
                if (updatePlayerOutcome.error != null) {
                    alert("Update player failed :\n\n" + updatePlayerOutcome.error);
                    return;
                }

                // Reload the page
                location.reload();
            } else {
                alert("Failure to update player. Error = " + xhr.status);
            }
        }
    };

    var putPlayer = {
        playerId: player.playerId,
        playerFirstName: player.playerFirstName,
        playerLastName: player.playerLastName,
        playerCountry: player.playerCountry,
        playerTransferValue: player.playerTransferValue
    }

    xhr.send(JSON.stringify(putPlayer));
}

function refreshPlayer(player) {

    var row = player.row;
    while (row.cells.length > 0) {
        row.deleteCell(0);
    }

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(player.playerId.uuid));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(player.playerType));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(player.playerFirstName));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(player.playerLastName));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(player.playerAge));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(player.playerCountry));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(player.playerAssetValue));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(player.playerTransferValue));

    // Create an ActionsElement for this player
    var actionsElement = document.createElement('DIV');
    actionsElement.classList.add('soccer-actions');

    actionsElement.appendChild(defineButtonElement("EDIT FIRST NAME", function() {
        editPlayerFirstName(player);
    }));

    actionsElement.appendChild(defineButtonElement("EDIT LAST NAME", function() {
        editPlayerLastName(player);
    }));

    actionsElement.appendChild(defineButtonElement("EDIT COUNTRY", function() {
        editPlayerCountry(player);
    }));

    actionsElement.appendChild(defineButtonElement("EDIT TRANSFER VALUE", function() {
        editPlayerTransferValue(player);
    }));

    cell = row.insertCell(-1);
    cell.appendChild(actionsElement);
}

function editTeamName(team) {

    var teamName = prompt("Enter the name of the team", team.teamName);
    if (teamName == null) {
        return;
    }
    team.teamName = teamName;
    refreshTeam(team);

    updateTeam(team);
}

function editTeamCountry(team) {

    var teamCountry = prompt("Enter the country of the team", team.teamCountry);
    if (teamCountry == null) {
        return;
    }
    team.teamCountry = teamCountry;
    refreshTeam(team);

    updateTeam(team);
}

function updateTeam(team) {

    var xhr = new XMLHttpRequest();
    xhr.open("PUT", "/team/" + team.teamId.uuid, true);
    xhr.setRequestHeader('Content-Type', "application/json;charset=UTF-8");
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader("User-Token", loadUserToken());

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {

                var updateTeamOutcome = JSON.parse(xhr.responseText);
                if (updateTeamOutcome.error != null) {
                    alert("Update team failed :\n\n" + updateTeamOutcome.error);
                    return;
                }

                // Reload the page
                location.reload();
            } else {
                alert("Failure to update team. Error = " + xhr.status);
            }
        }
    };

    var putTeam = {
        teamId: team.teamId,
        teamName: team.teamName,
        teamCountry: team.teamCountry
    }

    xhr.send(JSON.stringify(putTeam));
}

function refreshTeam(team) {

    var row = team.row;
    while (row.cells.length > 0) {
        row.deleteCell(0);
    }

    var teamValue = 0;
    for (var player of team.playerArray) {
        teamValue += player.playerAssetValue;
    }
    var teamPlayers = team.playerArray.length;

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(team.teamId.uuid));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(team.teamName));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(team.teamCountry));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(team.teamBalance));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(teamValue));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(teamPlayers));

    // Create an ActionsElement for this team
    var actionsElement = document.createElement('DIV');
    actionsElement.classList.add('soccer-actions');

    actionsElement.appendChild(defineButtonElement("EDIT NAME", function() {
        editTeamName(team);
    }));

    actionsElement.appendChild(defineButtonElement("EDIT COUNTRY", function() {
        editTeamCountry(team);
    }));

    cell = row.insertCell(-1);
    cell.appendChild(actionsElement);
}

function displayTeam(team) {

    var teamTableBody = document.getElementById("teamTableBodyId")
    team.row = teamTableBody.insertRow(-1);

    refreshTeam(team);

    var playerTableBody = document.getElementById("playerTableBodyId")

    // Display all players
    for (var player of team.playerArray) {
        player.row = playerTableBody.insertRow(-1);
        refreshPlayer(player);
    }
}

function loadTeam() {

    var xhr = new XMLHttpRequest();
    xhr.open("GET", "/team", true);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader("User-Token", loadUserToken());

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {
                var team = JSON.parse(xhr.responseText);
                displayTeam(team);
            } else {
                alert("Failure to get team");
            }
        }
    };
    xhr.send();
}

function transferPlayer(marketPlayer, marketTeam) {

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/transfer", true);
    xhr.setRequestHeader('Content-Type', "application/json;charset=UTF-8");
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader("User-Token", loadUserToken());

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {

                var transferPlayerOutcome = JSON.parse(xhr.responseText);
                if (transferPlayerOutcome.error != null) {
                    alert("Transfer player failed :\n\n" + transferPlayerOutcome.error);
                    return;
                }

                // Reload the page
                location.reload();
            } else {
                alert("Failure to transfer player. Error = " + xhr.status);
            }
        }
    };

    var postPlayer = {
        playerId: marketPlayer.playerId,
        teamId: marketTeam.teamId
    }

    xhr.send(JSON.stringify(postPlayer));
}

function displayMarketPlayer(marketPlayer, marketTeam, marketTableBody) {

    // Create a new row
    var row = marketTableBody.insertRow(-1);

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketPlayer.playerId.uuid));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketPlayer.playerType));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketPlayer.playerFirstName));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketPlayer.playerLastName));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketPlayer.playerAge));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketPlayer.playerCountry));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketPlayer.playerTransferValue));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketTeam.teamName));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketTeam.teamCountry));

    cell = row.insertCell(-1);
    cell.appendChild(defineTextElement(marketTeam.teamId.uuid));

    // Create an ActionsElement for this player
    var actionsElement = document.createElement('DIV');
    actionsElement.classList.add('soccer-actions');

    actionsElement.appendChild(defineButtonElement("TRANSFER", function() {
        transferPlayer(marketPlayer, marketTeam);
    }));

    cell = row.insertCell(-1);
    cell.appendChild(actionsElement);
}

function displayMarketTeam(marketTeam, marketTableBody) {

    if (marketTeam.playerArray != null) {
        for (var marketPlayer of marketTeam.playerArray) {
            displayMarketPlayer(marketPlayer, marketTeam, marketTableBody);
        }
    }
}

function displayMarket(market) {

    var marketTableBody = document.getElementById("marketTableBodyId")

    if (market.teamArray != null) {
        for (var marketTeam of market.teamArray) {
            displayMarketTeam(marketTeam, marketTableBody);
        }
    }
}

function loadMarket() {

    var xhr = new XMLHttpRequest();
    xhr.open("GET", "/market", true);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.setRequestHeader("User-Token", loadUserToken());

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {
                var market = JSON.parse(xhr.responseText);
                displayMarket(market);
            } else {
                alert("Failure to get market");
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

    loadTeam();

    loadMarket();
}

displayPage();
