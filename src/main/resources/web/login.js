
var loginDetails = "\n" + ["Username must be a valid email address",
    "Password must have 8 characters or more with at least :",
    "1 lower case character",
    "1 upper case character" ,
    "1 numeric character",
    "1 special character !@#$%&"
].join("\n");

function storeUserToken(userId, userToken) {

    if (typeof (Storage) != undefined) {
        sessionStorage.setItem('user-id', userId);
        sessionStorage.setItem('user-token', userToken.token);
    }
}

function signUp(user) {

    if ((user.username == null) || (user.password == null)) {
        return;
    }

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/user", true);
    xhr.setRequestHeader('Content-Type', "application/json;charset=UTF-8");

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {
                var userToken = JSON.parse(xhr.responseText);
                storeUserToken(user.username,userToken);
                // Open the Dashboard page after succesfull authentication
                window.location.href="/ui/dashboard";
            } else {
                alert("Failure to sign up:\n\nUser can not exist already.\n" + loginDetails);
            }
        }
    };
    xhr.send(JSON.stringify(user));
}

function signIn(user) {

    if ((user.username == null) || (user.password == null)) {
        return;
    }

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/login", true);
    xhr.setRequestHeader('Content-Type', "application/json;charset=UTF-8");

    xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
            if (xhr.status == 200) {
                var userToken = JSON.parse(xhr.responseText);
                storeUserToken(user.username,userToken);
                // Open the Dashboard page after succesfull authentication
                window.location.href="/ui/dashboard";
            } else {
                alert("Failure to sign in :\n\nUser can not exist already.\n" + loginDetails);
            }
        }
    };
    xhr.send(JSON.stringify(user));
}

function signUpUser() {

    var user = {
        username: document.getElementById("usernameSignUpId").value,
        password: document.getElementById("passwordSignUpId").value
    }

    signUp(user);
}

function signInUser() {

    var user = {
        username: document.getElementById("usernameSignInId").value,
        password: document.getElementById("passwordSignInId").value
    }

    signIn(user);
}
