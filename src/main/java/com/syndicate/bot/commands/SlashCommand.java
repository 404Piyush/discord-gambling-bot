package com.syndicate.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface SlashCommand {
    void execute(SlashCommandInteractionEvent event);
    CommandData getCommandData();
    String getName();
    String getDescription();
    CommandCategory getCategory();
    boolean requiresCommitteeRole();
    default boolean isGuildOnly() {
        return true;
    }
} 