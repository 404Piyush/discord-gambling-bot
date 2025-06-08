package com.syndicate.bot.listeners;

import com.syndicate.bot.commands.SlashCommandManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlashCommandListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SlashCommandListener.class);
    private final SlashCommandManager slashCommandManager;

    public SlashCommandListener(SlashCommandManager slashCommandManager) {
        this.slashCommandManager = slashCommandManager;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("❌ This bot only works in servers!").setEphemeral(true).queue();
            return;
        }

        logger.debug("Received slash command: {} from user: {}", event.getName(), event.getUser().getAsTag());
        slashCommandManager.handleSlashCommand(event);
    }
} 