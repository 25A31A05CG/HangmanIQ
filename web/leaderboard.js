
async function loadLeaderboard() {

    const tbody = document.getElementById("leaderboardBody");

    try {

        const response = await fetch("/api/leaderboard");

        if (!response.ok) {
            throw new Error("Leaderboard loading failed");
        }

        const players = await response.json();

        tbody.innerHTML = "";

        if (players.length === 0) {

            tbody.innerHTML =
                "<tr><td colspan='3'>No players yet!</td></tr>";

            return;
        }

        players.forEach((player, index) => {

            const row = document.createElement("tr");

            const rank = document.createElement("td");
            const name = document.createElement("td");
            const score = document.createElement("td");

            rank.textContent = index + 1;
            name.textContent = player.name;
            score.textContent = player.score;

            row.append(rank, name, score);

            tbody.appendChild(row);
        });

    } catch (error) {

        console.error(error);

        tbody.innerHTML =
            "<tr><td colspan='3'>Unable to load leaderboard.</td></tr>";
    }
}

async function logout() {

    const token = localStorage.getItem("token");

    try {
        await fetch("/api/logout", {
            method: "POST",
            headers: {
                "Authorization": "Bearer " + token
            }
        });
    } catch (error) {
        console.error(error);
    }

    localStorage.clear();
    window.location.href = "login.html";
}

loadLeaderboard();
