
// =============================================
// HANGMAN WORD CHALLENGE
// COMPLETE UPDATED GAME.JS
// =============================================


// =============================================
// 1. AUTHENTICATION
// =============================================

const token = localStorage.getItem("token");

if (!token) {
    window.location.href = "login.html";
}

const playerName =
    localStorage.getItem("playerName") || "Player";

document.getElementById("playerName").textContent = playerName;


// =============================================
// 2. HTML ELEMENTS
// =============================================

const wordDisplay = document.getElementById("wordDisplay");
const attemptsDisplay = document.getElementById("attempts");

const roundScoreDisplay = document.getElementById("roundScore");
const totalScoreDisplay = document.getElementById("totalScore");

const drawing = document.getElementById("drawing");
const message = document.getElementById("message");
const hintMessage = document.getElementById("hintMessage");

const keyboard = document.getElementById("keyboard");

const firstHintButton =
    document.getElementById("firstHintButton");

const secondHintButton =
    document.getElementById("secondHintButton");

const categorySelect =
    document.getElementById("categorySelect");

const gameControls =
    document.getElementById("gameControls");

const nextButton =
    document.getElementById("nextButton");

const quitButton =
    document.getElementById("quitButton");


// =============================================
// 3. HANGMAN DRAWING
// =============================================

const hangmanStages = [
`  +---+
  |   |
      |
      |
      |
      |
=========`,
`  +---+
  |   |
  O   |
      |
      |
      |
=========`,
`  +---+
  |   |
  O   |
  |   |
      |
      |
=========`,
`  +---+
  |   |
  O   |
 /|   |
      |
      |
=========`,
`  +---+
  |   |
  O   |
 /|\\  |
      |
      |
=========`,
`  +---+
  |   |
  O   |
 /|\\  |
 /    |
      |
=========`,
`  +---+
  |   |
  O   |
 /|\\  |
 / \\  |
      |
=========`
];


// =============================================
// 4. GAME VARIABLES
// =============================================

let currentWord = "";
let currentTopic = "";
let currentHint = "";

let guessedLetters = [];

let attempts = 6;

let roundScore = 0;

let totalScore =
    Number(localStorage.getItem("totalScore")) || 0;

let firstHintUsed = false;
let secondHintUsed = false;

let roundCompleted = false;
let roundSaved = false;
let roundReset = true;

let currentGameId = "";

let questionAvailable = false;

let usedQuestions = {};

let roundResult = "";

let saveInProgress = false;
let finishInProgress = false;


// =============================================
// 5. CREATE UNIQUE GAME ID
// =============================================

function createGameId() {

    if (
        window.crypto &&
        typeof window.crypto.randomUUID === "function"
    ) {
        return window.crypto.randomUUID();
    }

    return Date.now() + "-" +
        Math.random().toString(36).slice(2);
}


// =============================================
// 6. UPDATE SCORE DISPLAY
// =============================================

function updateScores() {

    roundScoreDisplay.textContent = roundScore;
    totalScoreDisplay.textContent = totalScore;
}


// =============================================
// 7. LOAD SCORE FROM DATABASE
// =============================================

async function syncCurrentScore() {

    try {

        const response = await fetch("/api/me", {
            method: "GET",
            headers: {
                "Authorization": "Bearer " + token
            }
        });

        if (response.status === 401) {

            localStorage.removeItem("token");
            window.location.href = "login.html";

            return false;
        }

        if (!response.ok) {
            throw new Error("Could not load player score.");
        }

        const data = await response.json();

        totalScore = Number(data.totalScore) || 0;

        localStorage.setItem(
            "totalScore",
            String(totalScore)
        );

        updateScores();

        return true;

    } catch (error) {

        console.error("Score loading error:", error);

        message.textContent =
            "Could not load your score. Please refresh.";

        return false;
    }
}


// =============================================
// 8. LOAD CATEGORIES
// =============================================

function loadCategories() {

    categorySelect.innerHTML = "";

    if (typeof questionBank === "undefined") {

        message.textContent =
            "Question bank not loaded. Check words.js.";

        return;
    }

    const categories = Object.keys(questionBank);

    categories.forEach(category => {

        const option = document.createElement("option");

        option.value = category;
        option.textContent = category;

        categorySelect.appendChild(option);
    });

    const preferredCategory = "AI & Technology";

    if (categories.includes(preferredCategory)) {
        categorySelect.value = preferredCategory;
    }
}


// =============================================
// 9. CREATE A-Z KEYBOARD
// =============================================

function createKeyboard() {

    keyboard.innerHTML = "";

    const letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    for (const letter of letters) {

        const button = document.createElement("button");

        button.type = "button";
        button.textContent = letter;
        button.className = "key";

        button.addEventListener("click", function() {
            handleGuess(letter);
        });

        keyboard.appendChild(button);
    }
}


// =============================================
// 10. UPDATE KEYBOARD
// =============================================

function updateKeyboard() {

    const buttons =
        keyboard.querySelectorAll("button");

    buttons.forEach(button => {

        const letter = button.textContent;

        button.disabled =
            guessedLetters.includes(letter) ||
            roundCompleted;
    });
}


// =============================================
// 11. GET RANDOM QUESTION
// NO REPEATING UNTIL QUESTIONS EXHAUSTED
// =============================================

function getNextQuestion(category) {

    if (
        typeof questionBank === "undefined" ||
        !questionBank[category]
    ) {
        return null;
    }

    const questions = questionBank[category];

    if (!Array.isArray(questions) || questions.length === 0) {
        return null;
    }

    if (!usedQuestions[category]) {
        usedQuestions[category] = [];
    }

    if (usedQuestions[category].length >= questions.length) {
        usedQuestions[category] = [];
    }

    const available = questions
        .map((question, index) => ({
            question,
            index
        }))
        .filter(item =>
            !usedQuestions[category].includes(item.index)
        );

    if (available.length === 0) {
        return null;
    }

    const selected = available[
        Math.floor(Math.random() * available.length)
    ];

    usedQuestions[category].push(selected.index);

    return selected.question;
}


// =============================================
// 12. ENABLE GAME
// =============================================

function enableGame() {

    firstHintButton.disabled = firstHintUsed;
    secondHintButton.disabled = secondHintUsed;

    updateKeyboard();
}


// =============================================
// 13. DISABLE GAME
// =============================================

function disableGame() {

    firstHintButton.disabled = true;
    secondHintButton.disabled = true;

    keyboard.querySelectorAll("button").forEach(button => {
        button.disabled = true;
    });
}


// =============================================
// 14. CATEGORY UNAVAILABLE
// =============================================

function showCategoryUnavailable(category) {

    currentTopic = category;

    questionAvailable = false;

    currentWord = "";
    currentHint = "";

    guessedLetters = [];

    attempts = 6;
    roundScore = 0;

    roundCompleted = false;
    roundSaved = false;
    roundReset = true;

    currentGameId = "";
    roundResult = "";

    attemptsDisplay.textContent = "6";

    wordDisplay.textContent = "Coming Soon!";

    drawing.textContent = hangmanStages[0];

    message.textContent =
        "Questions for " + category + " are coming soon!";

    hintMessage.textContent =
        "Please select another category.";

    createKeyboard();
    disableGame();

    gameControls.style.display = "none";

    updateScores();
}


// =============================================
// 15. START NEW QUESTION
// =============================================

function startNewQuestion() {

    currentTopic = categorySelect.value;

    const question = getNextQuestion(currentTopic);

    if (!question || !question.word || !question.hint) {

        showCategoryUnavailable(currentTopic);

        return;
    }

    questionAvailable = true;

    currentWord = question.word.toUpperCase().trim();

    currentHint = question.hint;

    guessedLetters = [];

    attempts = 6;

    roundScore = 0;

    firstHintUsed = false;
    secondHintUsed = false;

    roundCompleted = false;
    roundSaved = false;
    roundReset = true;

    roundResult = "";

    saveInProgress = false;
    finishInProgress = false;

    currentGameId = createGameId();

    attemptsDisplay.textContent = attempts;

    message.textContent =
        "Guess the hidden word!";

    hintMessage.textContent =
        "Use your hints if you need help!";

    // Hide previous question buttons
    gameControls.style.display = "none";

    nextButton.disabled = false;
    quitButton.disabled = false;

    nextButton.textContent = "Next Question";
    quitButton.textContent = "Quit Game";

    createKeyboard();

    enableGame();

    displayWord();
    updateDrawing();
    updateScores();

    console.log("New question:", currentWord);
}


// =============================================
// 16. DISPLAY WORD
// =============================================

function displayWord() {

    wordDisplay.textContent = currentWord
        .split("")
        .map(letter =>
            guessedLetters.includes(letter) ||
            !/[A-Z]/.test(letter)
                ? letter
                : "_"
        )
        .join(" ");
}


// =============================================
// 17. UPDATE HANGMAN DRAWING
// =============================================

function updateDrawing() {

    drawing.textContent =
        hangmanStages[6 - attempts];
}


// =============================================
// 18. FIRST HINT
// FREE - NO SCORE DEDUCTION
// =============================================

function useFirstHint() {

    if (
        !questionAvailable ||
        roundCompleted ||
        firstHintUsed
    ) {
        return;
    }

    firstHintUsed = true;

    hintMessage.textContent =
        "💡 Hint: " + currentHint;

    firstHintButton.disabled = true;

    // First hint is free.
}


// =============================================
// 19. SECOND HINT
// SCORE WILL BE 10 POINTS
// =============================================

function useSecondHint() {

    if (
        !questionAvailable ||
        roundCompleted ||
        secondHintUsed
    ) {
        return;
    }

    const hiddenLetters = [
        ...new Set(
            currentWord.split("").filter(letter =>
                /[A-Z]/.test(letter) &&
                !guessedLetters.includes(letter)
            )
        )
    ];

    if (hiddenLetters.length === 0) {

        hintMessage.textContent =
            "No hidden letters remaining!";

        return;
    }

    secondHintUsed = true;

    const revealedLetter = hiddenLetters[
        Math.floor(Math.random() * hiddenLetters.length)
    ];

    guessedLetters.push(revealedLetter);

    hintMessage.textContent =
        "🔓 Revealed letter: " + revealedLetter +
        " | Your maximum score is now 10 points.";

    secondHintButton.disabled = true;

    displayWord();
    updateKeyboard();

    const won = currentWord.split("").every(letter =>
        guessedLetters.includes(letter) ||
        !/[A-Z]/.test(letter)
    );

    if (won) {
        finishRound(true);
    }
}


// =============================================
// 20. HANDLE LETTER GUESS
// =============================================

function handleGuess(letter) {

    if (!questionAvailable || roundCompleted) {
        return;
    }

    if (guessedLetters.includes(letter)) {
        return;
    }

    guessedLetters.push(letter);

    if (currentWord.includes(letter)) {

        message.textContent =
            "Correct guess! 🎉";

    } else {

        attempts--;

        attemptsDisplay.textContent = attempts;

        message.textContent =
            "Wrong guess! Try again.";
    }

    displayWord();

    updateDrawing();

    updateKeyboard();

    const won = currentWord.split("").every(letter =>
        guessedLetters.includes(letter) ||
        !/[A-Z]/.test(letter)
    );

    if (won) {

        finishRound(true);

        return;
    }

    if (attempts === 0) {
        finishRound(false);
    }
}


// =============================================
// 21. SAVE SCORE TO DATABASE
// =============================================

async function saveCurrentRound(result) {

    if (roundSaved) {
        return true;
    }

    if (saveInProgress) {
        return false;
    }

    if (!questionAvailable || !currentGameId) {
        return false;
    }

    saveInProgress = true;

    const data = new URLSearchParams({

        gameId: currentGameId,
        topic: currentTopic,
        word: currentWord,
        score: String(roundScore),
        result: result

    });

    try {

        const response = await fetch("/api/save-score", {

            method: "POST",

            headers: {

                "Content-Type":
                    "application/x-www-form-urlencoded",

                "Authorization": "Bearer " + token

            },

            body: data

        });

        const responseText = await response.text();

        let responseData;

        try {
            responseData = JSON.parse(responseText);
        } catch (error) {
            throw new Error(
                "Server returned an invalid response: " +
                responseText.substring(0, 100)
            );
        }

        if (response.status === 401) {

            localStorage.removeItem("token");

            window.location.href = "login.html";

            return false;
        }

        if (!response.ok) {

            throw new Error(
                responseData.error || "Could not save score."
            );
        }

        roundSaved = true;

        totalScore =
            Number(responseData.totalScore) || 0;

        localStorage.setItem(
            "totalScore",
            String(totalScore)
        );

        updateScores();

        return true;

    } catch (error) {

        console.error("Score saving error:", error);

        message.textContent +=
            " Score could not be saved: " + error.message;

        return false;

    } finally {

        saveInProgress = false;
    }
}


// =============================================
// 22. RESET CURRENT GAME SCORE
// LEADERBOARD BEST SCORE IS PRESERVED
// =============================================

async function resetCurrentGameScore() {

    try {

        const response = await fetch("/api/reset-score", {

            method: "POST",

            headers: {
                "Authorization": "Bearer " + token
            }

        });

        const responseText = await response.text();

        let responseData;

        try {
            responseData = JSON.parse(responseText);
        } catch (error) {
            throw new Error(
                "Invalid reset response: " +
                responseText.substring(0, 100)
            );
        }

        if (response.status === 401) {

            localStorage.removeItem("token");

            window.location.href = "login.html";

            return false;
        }

        if (!response.ok) {

            throw new Error(
                responseData.error || "Could not reset score."
            );
        }

        totalScore = 0;

        localStorage.setItem("totalScore", "0");

        updateScores();

        return true;

    } catch (error) {

        console.error("Reset error:", error);

        alert(
            "Could not reset current score: " +
            error.message
        );

        return false;
    }
}


// =============================================
// 23. FINISH ROUND
// =============================================

async function finishRound(won) {

    if (roundCompleted || finishInProgress) {
        return;
    }

    finishInProgress = true;

    roundCompleted = true;

    roundResult = won ? "WON" : "LOST";

    if (won) {

        // No hint = 20
        // First hint = 20
        // Second hint = 10

        roundScore = secondHintUsed ? 10 : 20;

        message.textContent =
            "🎉 Congratulations! You guessed the word! " +
            "You earned " + roundScore + " points!";

        roundReset = true;

    } else {

        roundScore = 0;

        message.textContent =
            "Game Over! The correct word was " + currentWord;

        roundReset = false;
    }

    disableGame();

    updateScores();

    // Save completed question
    const saved = await saveCurrentRound(roundResult);

    if (!saved) {

        message.textContent +=
            " Score saving failed. Click Retry Save.";

        nextButton.textContent = "Retry Save";

    } else {

        // Reset current score after losing
        if (!won) {

            const reset = await resetCurrentGameScore();

            if (reset) {

                roundReset = true;

                message.textContent +=
                    " Your current game score is now 0.";

            } else {

                nextButton.textContent = "Retry Reset";
            }

        } else {

            nextButton.textContent = "Next Question";
        }
    }

    // SHOW NEXT AND QUIT BUTTONS
    gameControls.style.display = "flex";

    nextButton.style.display = "inline-block";
    quitButton.style.display = "inline-block";

    nextButton.disabled = false;
    quitButton.disabled = false;

    finishInProgress = false;

    console.log("Round finished.");
}


// =============================================
// 24. NEXT QUESTION / START NEW GAME
// =============================================

nextButton.onclick = async function() {

    console.log("Next Question clicked!");

    if (finishInProgress || saveInProgress) {
        alert("Please wait for the score update.");
        return;
    }

    if (!roundCompleted) {
        return;
    }

    nextButton.disabled = true;

    try {

        // Retry score saving if necessary
        if (!roundSaved) {

            nextButton.textContent = "Saving...";

            const saved =
                await saveCurrentRound(roundResult);

            if (!saved) {

                nextButton.textContent = "Retry Save";

                nextButton.disabled = false;

                return;
            }
        }

        // Retry score reset if player lost
        if (!roundReset) {

            nextButton.textContent = "Resetting...";

            const reset =
                await resetCurrentGameScore();

            if (!reset) {

                nextButton.textContent = "Retry Reset";

                nextButton.disabled = false;

                return;
            }

            roundReset = true;
        }

        // START THE NEXT QUESTION
        startNewQuestion();

        console.log("Next question started!");

    } catch (error) {

        console.error("Next question error:", error);

        alert("Could not start the next question.");

        nextButton.disabled = false;
    }
};


// =============================================
// 25. QUIT GAME
// RESET CURRENT SCORE
// KEEP BEST SCORE
// =============================================

quitButton.onclick = async function() {

    console.log("Quit Game clicked!");

    if (finishInProgress || saveInProgress) {

        alert("Please wait for the score update.");

        return;
    }

    const confirmQuit = confirm(

        "Are you sure you want to quit?\n\n" +
        "Your current game score will reset to 0.\n" +
        "Your leaderboard best score will remain unchanged."

    );

    if (!confirmQuit) {
        return;
    }

    quitButton.disabled = true;
    nextButton.disabled = true;

    try {

        // Save completed round if not already saved
        if (roundCompleted && !roundSaved) {

            const saved =
                await saveCurrentRound(roundResult);

            if (!saved) {

                alert("Could not save your score.");

                quitButton.disabled = false;
                nextButton.disabled = false;

                return;
            }
        }

        // Reset current score
        const reset = await resetCurrentGameScore();

        if (reset) {

            window.location.href = "leaderboard.html";

        } else {

            quitButton.disabled = false;
            nextButton.disabled = false;
        }

    } catch (error) {

        console.error("Quit error:", error);

        alert("Could not quit the game.");

        quitButton.disabled = false;
        nextButton.disabled = false;
    }
};


// =============================================
// 26. CATEGORY CHANGE
// =============================================

categorySelect.addEventListener("change", function() {

    if (
        questionAvailable &&
        !roundCompleted &&
        currentGameId
    ) {

        const confirmChange = confirm(

            "Changing category will discard your current question. Continue?"

        );

        if (!confirmChange) {

            categorySelect.value = currentTopic;

            return;
        }
    }

    startNewQuestion();
});


// =============================================
// 27. HINT BUTTON EVENTS
// =============================================

firstHintButton.onclick = useFirstHint;

secondHintButton.onclick = useSecondHint;


// =============================================
// 28. INITIALIZE GAME
// =============================================

async function initializeGame() {

    loadCategories();

    createKeyboard();

    updateScores();

    const scoreLoaded = await syncCurrentScore();

    if (!scoreLoaded) {
        return;
    }

    startNewQuestion();
}

initializeGame();

