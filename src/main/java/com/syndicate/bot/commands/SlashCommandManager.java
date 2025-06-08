package com.syndicate.bot.commands;

import com.syndicate.bot.commands.economy.*;
import com.syndicate.bot.commands.gambling.*;
import com.syndicate.bot.commands.management.*;
import com.syndicate.bot.commands.information.*;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlashCommandManager {
    private static final Logger logger = LoggerFactory.getLogger(SlashCommandManager.class);
    private final Map<String, SlashCommand> commands = new HashMap<>();
    private final DatabaseManager databaseManager;

    public SlashCommandManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        registerCommands();
    }

    private void registerCommands() {
        
        registerCommand(new com.syndicate.bot.commands.information.MenuSlashCommand(databaseManager));
        
        
        registerCommand(new GambleSlashCommand(databaseManager));
        registerCommand(new EconomySlashCommand(databaseManager));
        registerCommand(new InfoSlashCommand());
        
        
        registerCommand(new com.syndicate.bot.commands.information.HelpAllSlashCommand());

        logger.info("Registered {} slash commands", commands.size());
    }

    private void registerCommand(SlashCommand command) {
        commands.put(command.getName().toLowerCase(), command);
        logger.debug("Registered slash command: {}", command.getName());
    }

    public void registerSlashCommands(JDA jda) {
        List<CommandData> commandDataList = new ArrayList<>();
        
        System.out.println("📋 Preparing to register slash commands:");
        for (SlashCommand command : commands.values()) {
            System.out.println("  - /" + command.getName() + " - " + command.getDescription());
            commandDataList.add(command.getCommandData());
        }
        
        jda.updateCommands().addCommands(commandDataList).queue(
            success -> {
                System.out.println("✅ Successfully registered " + commandDataList.size() + " slash commands");
                logger.info("Successfully registered {} slash commands", commandDataList.size());
            },
            error -> {
                System.out.println("❌ Failed to register slash commands: " + error.getMessage());
                logger.error("Failed to register slash commands", error);
                error.printStackTrace();
            }
        );
    }

    public void handleSlashCommand(SlashCommandInteractionEvent event) {
        String commandName = event.getName().toLowerCase();
        SlashCommand command = commands.get(commandName);
        
        if (command != null) {
            try {
                
                if (command.requiresCommitteeRole()) {
                    boolean hasRole = databaseManager.isCommitteeMember(event.getGuild().getId(), event.getUser().getId());
                    if (!hasRole) {
                        event.reply("❌ You must be a committee member to use this command!").setEphemeral(true).queue();
                        return;
                    }
                }
                
                command.execute(event);
                logger.debug("Executed slash command: {} by user: {}", commandName, event.getUser().getAsTag());
            } catch (Exception e) {
                logger.error("Error executing slash command: " + commandName, e);
                if (!event.isAcknowledged()) {
                    event.reply("❌ An error occurred while executing the command.").setEphemeral(true).queue();
                } else {
                    event.getHook().sendMessage("❌ An error occurred while executing the command.").setEphemeral(true).queue();
                }
            }
        }
    }

    public SlashCommand getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public Map<String, SlashCommand> getAllCommands() {
        return new HashMap<>(commands);
    }
} 