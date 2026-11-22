compile:
 javac --add-modules jdk.httpserver -cp "lib\sqlite-jdbc.jar" -d out src\*.java

run:
 java --add-modules jdk.httpserver -cp "out;lib\sqlite-jdbc.jar" HangmanServer 