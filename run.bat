@echo off
REM =========================================================================
REM Inventory and Business Management System - Run Script
REM =========================================================================

set CLASSPATH=target/classes;mysql-connector-j-8.3.0.jar

REM Locate Java runtime
where java >nul 2>nul
if %ERRORLEVEL% equ 0 (
    java -cp "%CLASSPATH%" main.Main
    goto end
)

if exist "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\java.exe" (
    "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\java.exe" -cp "%CLASSPATH%" main.Main
    goto end
)

echo [ERROR] Java runtime not found. Please install JDK 11+ or add Java to PATH.
pause

:end

