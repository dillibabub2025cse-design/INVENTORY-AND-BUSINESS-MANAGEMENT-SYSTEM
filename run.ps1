# =========================================================================
# Inventory and Business Management System - PowerShell Launcher
# =========================================================================

$javaExe = "java"
if (!(Get-Command java -ErrorAction SilentlyContinue)) {
    if (Test-Path "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\java.exe") {
        $javaExe = "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\java.exe"
    } else {
        Write-Error "Java executable not found. Please ensure JDK 11+ is installed."
        exit 1
    }
}

& $javaExe -cp "target/classes;mysql-connector-j-8.3.0.jar" main.Main

