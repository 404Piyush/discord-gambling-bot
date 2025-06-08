package com.syndicate.bot.listeners;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.CommandManager;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(EventListener.class);
    private final CommandManager commandManager;

    public EventListener(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        
        if (event.getAuthor().isBot()) {
            return;
        }

        
        try {
            DatabaseManager db = SyndicateBot.getDatabaseManager();
            String guildId = event.getGuild().getId();
            String userId = event.getAuthor().getId();
            
            
            db.initializeUserBalance(guildId, userId, 1000);
        } catch (Exception e) {
            logger.error("Error initializing user balance", e);
        }

        
        commandManager.handleCommand(event);
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        try {
            DatabaseManager db = SyndicateBot.getDatabaseManager();
            String guildId = event.getGuild().getId();
            
            
            db.initializeCasinoBank(guildId, 1000000);
            
            logger.info("Joined new guild: {} ({})", event.getGuild().getName(), guildId);
            
            
            if (event.getGuild().getSystemChannel() != null) {
                event.getGuild().getSystemChannel().sendMessage(
                    "🎰 **Welcome to The Syndicate's Den!** 🎰\n\n" +
                    "I'm your casino management bot. Use `!help` to see all available commands.\n\n" +
                    "**Quick Setup:**\n" +
                    "• Use `!committee add @user` to add committee members\n" +
                    "• Use `!bankstatus` to check the casino's funds\n" +
                    "• Use `!balance` to check your personal balance\n\n" +
                    "Let the games begin! 🎲"
                ).queue();
            }
        } catch (Exception e) {
            logger.error("Error initializing guild", e);
        }
    }
} 