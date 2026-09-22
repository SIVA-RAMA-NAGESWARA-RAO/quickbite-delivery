@echo off
setlocal

echo ============================================
echo   QuickBite Delivery - Database setup
echo ============================================
echo.
echo This creates a "quickbite" login and four empty databases
echo (authdb, restaurantdb, orderdb, paymentdb) on your local PostgreSQL.
echo It is safe to run more than once.
echo.
echo You will be asked for your PostgreSQL SUPERUSER (postgres) password
echo - the one you set when you installed PostgreSQL.
echo.
pause

psql -U postgres -h localhost -f "%~dp0db\init.sql"

if %ERRORLEVEL% NEQ 0 (
  echo.
  echo ============================================
  echo   Something went wrong. Common causes:
  echo   1. PostgreSQL isn't running - open "Services" and start it.
  echo   2. "psql" isn't on your PATH - add PostgreSQL's "bin" folder
  echo      to PATH ^(usually C:\Program Files\PostgreSQL\<version>\bin^).
  echo   3. Wrong superuser password entered above.
  echo ============================================
) else (
  echo.
  echo ============================================
  echo   Done! Databases are ready.
  echo ============================================
)

pause
