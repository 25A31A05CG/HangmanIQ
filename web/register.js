
document.getElementById("registerForm")
.addEventListener("submit", async function(e) {

    e.preventDefault();

    const name = document.getElementById("name").value.trim();
    const password = document.getElementById("password").value;

    try {

        const response = await fetch("/api/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({ name, password })
        });

        const data = await response.json();

        if (response.ok) {
            alert("Account created successfully!");
            window.location.href = "login.html";
        } else {
            alert(data.error);
        }

    } catch (error) {
        alert("Server connection failed!");
        console.error(error);
    }
});
