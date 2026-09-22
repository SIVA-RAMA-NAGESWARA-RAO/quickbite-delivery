@echo off
echo Closing all QuickBite service windows...
taskkill /F /FI "WINDOWTITLE eq Eureka Server*"      >nul 2>nul
taskkill /F /FI "WINDOWTITLE eq Auth Service*"       >nul 2>nul
taskkill /F /FI "WINDOWTITLE eq Restaurant Service*" >nul 2>nul
taskkill /F /FI "WINDOWTITLE eq Payment Service*"    >nul 2>nul
taskkill /F /FI "WINDOWTITLE eq Order Service*"      >nul 2>nul
taskkill /F /FI "WINDOWTITLE eq API Gateway*"        >nul 2>nul
echo Done. (If a window didn't close, close it manually with Ctrl+C.)
pause
