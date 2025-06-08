@echo off
echo 🎰 Starting The Syndicate's Den Discord Bot...

REM Check if .env file exists, if not create a template
if not exist .env (
    echo Creating .env file template...
    echo BOT_TOKEN=YOUR_BOT_TOKEN_HERE > .env
    echo.
    echo ⚠️  Please edit .env file with your bot token!
    echo    Open .env file and replace YOUR_BOT_TOKEN_HERE with your actual bot token.
    echo.
    pause
    exit /b 1
)

REM Check if jar file exists, build if not
if not exist target\syndicate-den-bot.jar (
    echo 🔨 Jar file not found! Building project...
    mvn clean package -q
    if %errorlevel% neq 0 (
        echo ❌ Build failed! Please check the errors above.
        pause
        exit /b 1
    )
)

echo 🚀 Starting bot (configuration loaded from .env file)...
echo.

REM Run the bot
java -jar target\syndicate-den-bot.jar

pause 