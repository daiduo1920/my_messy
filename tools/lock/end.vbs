dim WSHshell
MsgBox "end..."
set WSHshell = WScript.CreateObject("WScript.shell")
WSHshell.run "taskkill /im WScript.exe /f", 0, true