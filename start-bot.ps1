# The Syndicate's Den Discord Bot - PowerShell Startup Script
Write-Host "Starting The Syndicate's Den Discord Bot..." -ForegroundColor Green

# Check if .env file exists, if not create a template
if (-not (Test-Path ".env")) {
    Write-Host "Creating .env file template..." -ForegroundColor Yellow
    "BOT_TOKEN=YOUR_BOT_TOKEN_HERE" | Out-File -FilePath ".env" -Encoding UTF8
    Write-Host "Please edit .env file with your bot token!" -ForegroundColor Red
    Write-Host "Open .env file and replace YOUR_BOT_TOKEN_HERE with your actual bot token." -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check if jar file exists
if (-not (Test-Path "target\syndicate-den-bot.jar")) {
    Write-Host "Jar file not found! Building project..." -ForegroundColor Yellow
    mvn clean package -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed! Please check the errors above." -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
}

Write-Host "Configuration will be loaded from .env file..." -ForegroundColor Cyan
Write-Host ""

# Start the bot (it will automatically read from .env file)
Write-Host "Starting bot..." -ForegroundColor Green
java -jar target\syndicate-den-bot.jar

Read-Host "Press Enter to exit" 