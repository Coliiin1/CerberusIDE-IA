@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tunel_rapido.ps1" %*
exit /b %errorlevel%
