@echo off
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: java is not installed.
    exit /b 1
)
java -jar target/connection-manager-1.0-SNAPSHOT-jar-with-dependencies.jar
