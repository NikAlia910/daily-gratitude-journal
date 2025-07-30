@echo off
setlocal enabledelayedexpansion

REM Daily Gratitude Journal - Gatling Test Runner (Windows)
REM This script provides easy execution of Gatling performance tests

REM Default configuration
set DEFAULT_BASE_URL=http://localhost:8080
set DEFAULT_USERS=10
set DEFAULT_RAMP=10
set SIMULATION_CLASS=gatling.simulations.GratitudeEntryGatlingTest

REM Initialize variables
set BASE_URL=%DEFAULT_BASE_URL%
set USERS=%DEFAULT_USERS%
set RAMP=%DEFAULT_RAMP%
set DRY_RUN=false
set VERBOSE=false

REM Function to show usage
:show_usage
echo Daily Gratitude Journal - Gatling Test Runner
echo.
echo Usage: %0 [OPTIONS]
echo.
echo Options:
echo   -h, --help              Show this help message
echo   -u, --url URL           Base URL for the application (default: %DEFAULT_BASE_URL%)
echo   -n, --users NUMBER      Number of concurrent users (default: %DEFAULT_USERS%)
echo   -r, --ramp SECONDS      Ramp-up time in seconds (default: %DEFAULT_RAMP%)
echo   -c, --class CLASS       Simulation class (default: %SIMULATION_CLASS%)
echo   -d, --dry-run           Show command without executing
echo   -v, --verbose           Enable verbose output
echo.
echo Examples:
echo   %0                                    # Run with default settings
echo   %0 -u http://localhost:8080          # Run with custom URL
echo   %0 -n 20 -r 30                       # Run with 20 users, 30s ramp
echo   %0 -d                                 # Show command without running
echo.
goto :eof

REM Function to check if application is running
:check_application
echo [INFO] Checking if application is running at %1...
powershell -Command "try { Invoke-WebRequest -Uri '%1/management/health' -TimeoutSec 5 | Out-Null; Write-Host '[SUCCESS] Application is running and healthy' -ForegroundColor Green } catch { Write-Host '[ERROR] Application is not running or not accessible at %1' -ForegroundColor Red; Write-Host '[WARNING] Please start the application before running tests' -ForegroundColor Yellow; exit 1 }"
if %ERRORLEVEL% neq 0 (
    exit /b 1
)
exit /b 0

REM Function to run the test
:run_test
echo [INFO] Configuration:
echo   Base URL: %1
echo   Users: %2
echo   Ramp Time: %3s
echo   Simulation: %4
echo.

if "%5"=="true" (
    echo [WARNING] DRY RUN - Command that would be executed:
    set MVN_CMD=mvn io.gatling:gatling-maven-plugin:test
    set ARGS=-Dgatling.simulationClass=%4
    
    if not "%1"=="%DEFAULT_BASE_URL%" (
        set ARGS=!ARGS! -DbaseURL=%1
    )
    
    if not "%2"=="%DEFAULT_USERS%" (
        set ARGS=!ARGS! -Dusers=%2
    )
    
    if not "%3"=="%DEFAULT_RAMP%" (
        set ARGS=!ARGS! -Dramp=%3
    )
    
    if "%6"=="true" (
        set MVN_CMD=!MVN_CMD! -X
    )
    
    echo !MVN_CMD! !ARGS!
    goto :eof
)

REM Check if application is running
call :check_application "%1"
if %ERRORLEVEL% neq 0 (
    exit /b 1
)

echo [INFO] Starting Gatling performance test...
echo.

REM Build Maven command
set MVN_CMD=mvn io.gatling:gatling-maven-plugin:test
set ARGS=-Dgatling.simulationClass=%4

if not "%1"=="%DEFAULT_BASE_URL%" (
    set ARGS=!ARGS! -DbaseURL=%1
)

if not "%2"=="%DEFAULT_USERS%" (
    set ARGS=!ARGS! -Dusers=%2
)

if not "%3"=="%DEFAULT_RAMP%" (
    set ARGS=!ARGS! -Dramp=%3
)

if "%6"=="true" (
    set MVN_CMD=!MVN_CMD! -X
)

REM Execute the test
!MVN_CMD! !ARGS!
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Gatling test completed successfully!
    echo [INFO] Check the generated reports in target/gatling/results/
) else (
    echo [ERROR] Gatling test failed!
    exit /b 1
)
goto :eof

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :run_test_main
if "%~1"=="-h" goto :show_usage
if "%~1"=="--help" goto :show_usage
if "%~1"=="-u" (
    set BASE_URL=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--url" (
    set BASE_URL=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-n" (
    set USERS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--users" (
    set USERS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-r" (
    set RAMP=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--ramp" (
    set RAMP=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-c" (
    set SIMULATION_CLASS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--class" (
    set SIMULATION_CLASS=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-d" (
    set DRY_RUN=true
    shift
    goto :parse_args
)
if "%~1"=="--dry-run" (
    set DRY_RUN=true
    shift
    goto :parse_args
)
if "%~1"=="-v" (
    set VERBOSE=true
    shift
    goto :parse_args
)
if "%~1"=="--verbose" (
    set VERBOSE=true
    shift
    goto :parse_args
)

echo [ERROR] Unknown option: %~1
call :show_usage
exit /b 1

:run_test_main
REM Validate numeric inputs
echo %USERS%| findstr /r "^[0-9][0-9]*$" >nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Users must be a positive integer
    exit /b 1
)

if %USERS% lss 1 (
    echo [ERROR] Users must be a positive integer
    exit /b 1
)

echo %RAMP%| findstr /r "^[0-9][0-9]*$" >nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Ramp time must be a positive integer
    exit /b 1
)

if %RAMP% lss 1 (
    echo [ERROR] Ramp time must be a positive integer
    exit /b 1
)

REM Run the test
call :run_test "%BASE_URL%" "%USERS%" "%RAMP%" "%SIMULATION_CLASS%" "%DRY_RUN%" "%VERBOSE%"
exit /b %ERRORLEVEL% 