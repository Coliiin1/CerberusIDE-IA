@echo off
setlocal

rem ============================================================
rem  CerberusIDE - launcher (JavaFX SDK local)
rem  Ejecuta el proyecto desde la clase launcher main.Main
rem ============================================================

set "PATH_TO_FX=C:\Users\danie\SKD\javafx-sdk-24\lib"

rem Compila el proyecto (incremental, rapido tras la primera vez)
call mvnw.cmd -q -DskipTests compile
if errorlevel 1 goto :error

rem Ejecuta el launcher usando el JavaFX SDK local en el module-path
java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml -cp "target\classes" main.Main
exit /b %errorlevel%

:error
echo.
echo ERROR DE COMPILACION. Revisa los mensajes de arriba.
exit /b 1
