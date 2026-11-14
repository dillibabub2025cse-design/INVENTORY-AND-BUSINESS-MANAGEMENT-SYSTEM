@echo off
REM =========================================================================
REM Inventory and Business Management System - Test Suite Runner
REM =========================================================================

set CLASSPATH=target/classes;target/test-classes;mysql-connector-j-8.3.0.jar

where java >nul 2>nul
if %ERRORLEVEL% equ 0 (
    java -ea -cp "%CLASSPATH%" test.TestSuite
    goto end
)

if exist "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\java.exe" (
    "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\java.exe" -ea -cp "%CLASSPATH%" test.TestSuite
    goto end
)

echo [ERROR] Java runtime not found.
pause

:end

