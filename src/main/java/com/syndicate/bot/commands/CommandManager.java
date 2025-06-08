package com.syndicate.bot.commands;

import com.syndicate.bot.commands.economy.*;
import com.syndicate.bot.commands.gambling.*;
import com.syndicate.bot.commands.management.*;
import com.syndicate.bot.commands.information.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CommandManager {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);
    private final Map<String, Command> commands = new HashMap<>();
    private final String prefix = "!";

    public CommandManager() {
        registerCommands();
    }

    private void registerCommands() {
        
        registerCommand(new DiceCommand());
        registerCommand(new RouletteCommand());
        registerCommand(new BlackjackCommand());
        registerCommand(new SlotsCommand());
        
        
        registerCommand(new BalanceCommand());
        registerCommand(new PayCommand());
        registerCommand(new LeaderboardCommand());
        
        
        registerCommand(new SetOddsCommand());
        registerCommand(new BankStatusCommand());
        registerCommand(new CommitteeCommand());
        
        
        registerCommand(new HelpCommand());
        registerCommand(new StatsCommand());

        logger.info("Registered {} commands", commands.size());
    }

    private void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);
        logger.debug("Registered command: {}", command.getName());
    }

    public void handleCommand(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        
        if (!message.startsWith(prefix)) {
            return;
        }

        String[] parts = message.substring(prefix.length()).split("\\s+");
        String commandName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        Command command = commands.get(commandName);
        if (command != null) {
            try {
                command.execute(event, args);
                logger.debug("Executed command: {} by user: {}", commandName, event.getAuthor().getAsTag());
            } catch (Exception e) {
                logger.error("Error executing command: " + commandName, e);
                event.getChannel().sendMessage("❌ An error occurred while executing the command.").queue();
            }
        }
    }

    public Command getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public Set<String> getCommandNames() {
        return commands.keySet();
    }

    public Map<String, Command> getAllCommands() {
        return new HashMap<>(commands);
    }

    public String getPrefix() {
        return prefix;
    }
} 