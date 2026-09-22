@echo off
echo ============================================
echo   Starting QuickBite Delivery (6 windows)
echo ============================================
echo.
echo Each service opens in its own window so you can see its logs.
echo Leave them all running. Close this launcher window any time -
echo it isn't needed once the others have started.
echo.

echo [1/6] Eureka Server (service registry) - waiting for this to come up first...
start "Eureka Server"      cmd /k "cd /d "%~dp0eureka-server"      && mvn spring-boot:run"
timeout /t 30 /nobreak >nul

echo [2/6] Auth Service...
start "Auth Service"       cmd /k "cd /d "%~dp0auth-service"       && mvn spring-boot:run"
timeout /t 6 /nobreak >nul

echo [3/6] Restaurant Service...
start "Restaurant Service" cmd /k "cd /d "%~dp0restaurant-service" && mvn spring-boot:run"
timeout /t 6 /nobreak >nul

echo [4/6] Payment Service...
start "Payment Service"    cmd /k "cd /d "%~dp0payment-service"    && mvn spring-boot:run"
timeout /t 6 /nobreak >nul

echo [5/6] Order Service...
start "Order Service"      cmd /k "cd /d "%~dp0order-service"      && mvn spring-boot:run"
timeout /t 25 /nobreak >nul

echo [6/6] API Gateway (starts last - it routes to everything above)...
start "API Gateway"        cmd /k "cd /d "%~dp0api-gateway"        && mvn spring-boot:run"

echo.
echo ============================================
echo   All six windows are launching.
echo   Give them 1-2 minutes, then check:
echo   http://localhost:8761   (Eureka - all 5 apps should register)
echo   http://localhost:8080   (API Gateway - what the app uses)
echo ============================================
echo.
echo Once Eureka shows everything registered, run start-frontend.bat
echo in a new window to launch the website.
pause
