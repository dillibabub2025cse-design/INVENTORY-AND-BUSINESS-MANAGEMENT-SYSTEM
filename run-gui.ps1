# =========================================================================
# Inventory and Business Management System - Silent PowerShell Desktop Launcher
# Launches the GUI directly with ZERO persistent terminal window
# =========================================================================

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectDir

if (Test-Path "$projectDir\InventoryERP.exe") {
    Start-Process -FilePath "$projectDir\InventoryERP.exe" -WindowStyle Hidden
    exit
}

$javaw = "javaw"
if (Test-Path "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\javaw.exe") {
    $javaw = "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\javaw.exe"
}

Start-Process -FilePath $javaw -ArgumentList "-cp `"target/classes;mysql-connector-j-8.3.0.jar`" ui.SwingApp" -WindowStyle Hidden -WorkingDirectory $projectDir
