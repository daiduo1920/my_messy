Dim WshShell,Path,i
MsgBox "start..."
set WSHshell = WScript.CreateObject("WScript.shell")
Do While True
    Wscript.Sleep 4000
	'WshShell.SendKeys "{F5}"
	WshShell.SendKeys "{SCROLLLOCK}"
Loop