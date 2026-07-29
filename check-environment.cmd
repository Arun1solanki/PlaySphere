@echo off
setlocal

echo === Java ===
java -version
if errorlevel 1 (
  echo Java was not found. Install Java 21 and set JAVA_HOME.
  exit /b 1
)

echo.
echo === Maven Wrapper ===
call "%~dp0backend\mvnw.cmd" -version
if errorlevel 1 (
  echo Maven Wrapper check failed.
  exit /b 1
)

echo.
echo Environment check passed.
endlocal
