Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
strDir = fso.GetParentFolderName(WScript.ScriptFullName)
WshShell.CurrentDirectory = strDir

javawPath = "C:\Program Files\JetBrains\PyCharm 2025.3.2\jbr\bin\javaw.exe"
If Not fso.FileExists(javawPath) Then
    javawPath = "javaw.exe"
End If

cmdLine = """" & javawPath & """ -cp """ & strDir & "\target\classes;" & strDir & "\mysql-connector-j-8.3.0.jar"" ui.SwingApp"
WshShell.Run cmdLine, 0, False
