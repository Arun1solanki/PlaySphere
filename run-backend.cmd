@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
if exist ".env" (
  for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
    set "line=%%A"
    if not "!line!"=="" if not "!line:~0,1!"=="#" set "%%A=%%B"
  )
)
cd backend
call mvnw.cmd spring-boot:run
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo Backend stopped with exit code %EXIT_CODE%.
  pause
)
exit /b %EXIT_CODE%
