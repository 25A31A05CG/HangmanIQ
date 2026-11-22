# 🎮 HangmanIQ – Smart Hangman Game

**HangmanIQ** is an interactive, web-based Hangman game developed using Java, HTML, CSS, JavaScript, and SQLite. It combines the classic word-guessing game with a user-friendly interface, database connectivity, and a leaderboard to make the gaming experience more engaging.

The project demonstrates Java backend development, frontend integration, database management, and server-side application development.

---

## ✨ Features

- **User Registration:** New users can create their accounts.
- **User Login:** Registered users can securely access the game.
- **Interactive Gameplay:** Guess hidden words letter by letter.
- **Dynamic Interface:** Enjoy an interactive gaming experience with HTML, CSS, and JavaScript.
- **Leaderboard:** View player rankings and compare game performance.
- **Database Integration:** SQLite stores application data.
- **Java Backend:** Handles server-side operations and application logic.

---

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Java | Backend development and game logic |
| HTML5 | Webpage structure |
| CSS3 | Styling and user interface |
| JavaScript | Interactive gameplay |
| SQLite | Database management |
| JDBC | Java database connectivity |
| Java HTTP Server | Handling HTTP requests |
| VS Code | Development environment |

---

## 📂 Project Structure

```text
HangmanIQ/
│
├── lib/
│   └── sqlite-jdbc.jar
│
├── src/
│   ├── Database.java
│   ├── GameManager.java
│   ├── HangmanServer.java
│   └── UserManager.java
│
├── web/
│   ├── game.html
│   ├── game.js
│   ├── leaderboard.html
│   ├── leaderboard.js
│   ├── login.html
│   ├── login.js
│   ├── register.html
│   ├── register.js
│   ├── style.css
│   └── words.js
│
├── ViewDatabase.java
├── .gitignore
└── README.md
```

---

## ⚙️ Installation and Setup

Follow these steps to run HangmanIQ on your local machine.

### Prerequisites

- Java Development Kit (JDK)
- Visual Studio Code or any Java-supported IDE
- SQLite JDBC Driver (included in the `lib` folder)

### Step 1: Clone the Repository

```bash
git clone https://github.com/25A31A05CG/HangmanIQ.git
```

### Step 2: Navigate to the Project Folder

```bash
cd HangmanIQ
```

### Step 3: Compile the Java Files

Open the terminal in the project directory and execute:

```bash
javac --add-modules jdk.httpserver -cp "lib\sqlite-jdbc.jar" -d out src\*.java
```

### Step 4: Run the Application

```bash
java --add-modules jdk.httpserver -cp "out;lib\sqlite-jdbc.jar" HangmanServer
```

### Step 5: Open the Application

Start the server and open the local application address displayed by the server in your browser.

---

## 🎯 Project Objectives

- To develop an interactive word-guessing game.
- To understand Java-based backend development.
- To implement database connectivity using SQLite and JDBC.
- To integrate frontend technologies with a Java backend.
- To understand user authentication and database operations.
- To gain practical experience in full-stack application development.

---

## 🚀 Future Enhancements

- Add multiple difficulty levels.
- Introduce a timer-based challenge.
- Add hints to help players guess words.
- Improve the leaderboard with additional player statistics.
- Introduce achievements and rewards.

---

## 👩‍💻 Developer

**Project Name:** HangmanIQ  
**Project Type:** Java-Based Web Application  
**Repository:** [HangmanIQ on GitHub](https://github.com/25A31A05CG/HangmanIQ)

---

⭐ If you find this project interesting, consider giving the repository a star!

**Made with passion, Java, and creativity. ❤️**
