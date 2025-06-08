package com.syndicate.bot;

import com.syndicate.bot.commands.CommandManager;
import com.syndicate.bot.commands.SlashCommandManager;
import com.syndicate.bot.database.DatabaseManager;
import com.syndicate.bot.listeners.EventListener;
import com.syndicate.bot.listeners.GameButtonListener;
import com.syndicate.bot.listeners.SlashCommandListener;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class SyndicateBot {
    private static final Logger logger = LoggerFactory.getLogger(SyndicateBot.class);
    private static JDA jda;
    private static CommandManager commandManager;
    private static SlashCommandManager slashCommandManager;
    private static DatabaseManager databaseManager;

    public static void main(String[] args) {
        System.out.println("=== MAIN METHOD STARTED ===");
        try {
            System.out.println("About to start logger...");
            logger.info("Starting The Syndicate's Den Discord Bot...");
            System.out.println("Logger started, loading config...");
            
            
            String token = getConfigValue("BOT_TOKEN", "bot.token");
            System.out.println("Config loaded, token length: " + (token != null ? token.length() : "null"));
            
            if (token == null || token.trim().isEmpty()) {
                System.out.println("ERROR: Token is null or empty!");
                logger.error("Bot token not found!");
                logger.error("Please configure your bot token using one of these methods:");
                logger.error("1. Create a .env file with: BOT_TOKEN=your_token_here");
                logger.error("2. Set BOT_TOKEN environment variable");
                logger.error("3. Add bot.token in config.properties");
                System.exit(1);
            }

            System.out.println("Token validated, initializing database...");
            
            databaseManager = new DatabaseManager();
            databaseManager.initialize();
            System.out.println("Database initialized, creating command manager...");

            
            commandManager = new CommandManager();
            slashCommandManager = new SlashCommandManager(databaseManager);
            System.out.println("Command managers created, building JDA...");

            
            jda = JDABuilder.createDefault(token)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_EMOJIS_AND_STICKERS
                    )
                    .setActivity(Activity.playing("Managing The Syndicate's Den"))
                    .addEventListeners(
                            new EventListener(commandManager),
                            new SlashCommandListener(slashCommandManager),
                            new GameButtonListener(databaseManager)
                    )
                    .build();

            System.out.println("JDA built, waiting for ready...");
            jda.awaitReady();
            
            
            System.out.println("Registering slash commands...");
            slashCommandManager.registerSlashCommands(jda);
            
            System.out.println("Bot is ready!");
            logger.info("The Syndicate's Den is now online and ready for business!");

        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            logger.error("Failed to start bot", e);
            System.exit(1);
        }
    }

    private static String getConfigValue(String envVar, String configKey) {
        
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String value = dotenv.get(envVar);
            System.out.println("DEBUG: .env value for " + envVar + ": " + (value != null ? "[FOUND]" : "[NOT FOUND]"));
            if (value != null && !value.trim().isEmpty()) {
                System.out.println("DEBUG: Using .env value");
                return value.trim();
            }
        } catch (Exception e) {
            System.out.println("DEBUG: .env error: " + e.getMessage());
            logger.debug("Could not load .env file: " + e.getMessage());
        }

        
        String value = System.getenv(envVar);
        System.out.println("DEBUG: System env value for " + envVar + ": " + (value != null ? "[FOUND]" : "[NOT FOUND]"));
        if (value != null && !value.trim().isEmpty()) {
            System.out.println("DEBUG: Using system env value");
            return value.trim();
        }
        
        
        try {
            Properties config = loadConfig();
            String propValue = config.getProperty(configKey);
            System.out.println("DEBUG: config.properties value for " + configKey + ": " + (propValue != null ? "[FOUND]" : "[NOT FOUND]"));
            if (propValue != null) {
                System.out.println("DEBUG: Using config.properties value");
            }
            return propValue;
        } catch (IOException e) {
            System.out.println("DEBUG: config.properties error: " + e.getMessage());
            logger.warn("Could not load config.properties: " + e.getMessage());
            return null;
        }
    }

    private static Properties loadConfig() throws IOException {
        Properties config = new Properties();
        if (Files.exists(Paths.get("config.properties"))) {
            config.load(Files.newInputStream(Paths.get("config.properties")));
        } else {
            
            config.setProperty("bot.token", "YOUR_BOT_TOKEN_HERE");
            config.setProperty("bot.prefix", "!");
            config.setProperty("casino.initial_bank", "1000000");
            config.setProperty("casino.min_bet", "1");
            config.setProperty("casino.max_bet", "10000");
            config.store(Files.newOutputStream(Paths.get("config.properties")), 
                        "The Syndicate's Den Configuration");
            logger.info("Created config.properties file. Please add your bot token!");
        }
        return config;
    }

    public static JDA getJDA() {
        return jda;
    }

    public static CommandManager getCommandManager() {
        return commandManager;
    }

    public static SlashCommandManager getSlashCommandManager() {
        return slashCommandManager;
    }

    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
} 