@echo off
echo ============================================
echo   QuickBite Delivery - checking your setup
echo ============================================
echo.

where java >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  echo [OK]   Java found:
  java -version 2>&1 | findstr /R "."
) else (
  echo [FAIL] Java not found on PATH. Install JDK 17 from https://adoptium.net
)
echo.

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  echo [OK]   Maven found:
  call mvn -version 2>&1 | findstr /C:"Apache Maven"
) else (
  echo [FAIL] Maven not found on PATH. Install from https://maven.apache.org/download.cgi
)
echo.

where node >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  echo [OK]   Node.js found:
  node -v
) else (
  echo [FAIL] Node.js not found on PATH. Install the LTS version from https://nodejs.org
)
echo.

where psql >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  echo [OK]   PostgreSQL client tools found.
  where pg_isready >nul 2>nul
  if %ERRORLEVEL% EQU 0 (
    pg_isready -h localhost -p 5432
  )
) else (
  echo [FAIL] "psql" not found on PATH. Add PostgreSQL's "bin" folder to PATH
  echo        ^(usually C:\Program Files\PostgreSQL\<version>\bin^), or install
  echo        PostgreSQL from https://www.postgresql.org/download/
)
echo.

echo ============================================
echo   If everything above says [OK], run
echo   setup-database.bat next, then start-all.bat
echo ============================================
pause
