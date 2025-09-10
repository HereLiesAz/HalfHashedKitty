@echo off
<<<<<<< HEAD
<<<<<<< HEAD
java -jar target/connection-manager-1.0-SNAPSHOT-jar-with-dependencies.jar
=======
=======
>>>>>>> origin/feature/build-pc-app
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: java is not installed.
    exit /b 1
)
java -jar target/connection-manager-1.0-SNAPSHOT.jar
<<<<<<< HEAD
>>>>>>> origin/feature/build-pc-app
=======
>>>>>>> origin/feature/build-pc-app
