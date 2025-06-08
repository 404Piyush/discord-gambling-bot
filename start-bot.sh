#!/bin/bash

echo "Starting The Syndicate's Den Discord Bot..."

# Check if .env file exists, if not create a template
if [ ! -f .env ]; then
    echo "Creating .env file template..."
    echo "BOT_TOKEN=YOUR_BOT_TOKEN_HERE" > .env
    echo "Please edit .env file with your bot token!"
    echo "Open .env file and replace YOUR_BOT_TOKEN_HERE with your actual bot token."
    exit 1
fi

# Check if jar file exists, build if not
if [ ! -f target/syndicate-den-bot.jar ]; then
    echo "Jar file not found! Building project..."
    mvn clean package -q
    if [ $? -ne 0 ]; then
        echo "Build failed! Please check the errors above."
        exit 1
    fi
fi

echo "Configuration will be loaded from .env file..."
echo

# Run the bot (it will automatically read from .env file)
java -jar target/syndicate-den-bot.jar 