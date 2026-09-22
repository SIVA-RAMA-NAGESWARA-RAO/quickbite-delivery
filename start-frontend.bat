@echo off
cd /d "%~dp0frontend"

if not exist node_modules (
  echo Installing frontend dependencies - this only happens once...
  call npm install
)

if not exist .env (
  copy .env.example .env >nul
)

echo Starting the QuickBite website...
call npm run dev
pause
