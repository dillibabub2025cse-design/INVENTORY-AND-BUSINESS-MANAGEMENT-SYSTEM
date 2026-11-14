@echo off
REM =========================================================================
REM Inventory and Business Management System - Silent Desktop Launcher
REM Launches the native GUI directly with ZERO persistent terminal window
REM =========================================================================

cd /d "%~dp0"
if exist "InventoryERP.exe" (
    start "" "InventoryERP.exe"
    exit
)

if exist "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\javaw.exe" (
    start "" "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\javaw.exe" -cp "target/classes;mysql-connector-j-8.3.0.jar" ui.SwingApp
    exit
)

start "" javaw -cp "target/classes;mysql-connector-j-8.3.0.jar" ui.SwingApp
exit
