
document.getElementById("loginForm")
.addEventListener("submit", async function(e) {

    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const password = document.getElementById("password").value;

    try {

        const response = await fetch("/api/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({ name, password })
        });

        const data = await response.json();

        if (response.ok) {

            localStorage.setItem("token", data.token);
            localStorage.setItem("playerName", data.name);
            localStorage.setItem("totalScore", data.totalScore);

            window.location.href = "game.html";

        } else {
            alert(data.error);
        }

    } catch (error) {
        alert("Server connection failed!");
        console.error(error);
    }
});
