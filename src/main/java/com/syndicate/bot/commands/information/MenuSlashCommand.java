package com.syndicate.bot.commands.information;

import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.commands.SlashCommand;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.*;

public class MenuSlashCommand implements SlashCommand {
    private final DatabaseManager databaseManager;

    public MenuSlashCommand(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        showMainMenu(event);
    }

    private void showMainMenu(SlashCommandInteractionEvent event) {
        try {
            String guildId = event.getGuild().getId();
            String userId = event.getUser().getId();
            
            
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🏛️ The Syndicate's Den - Main Menu")
                    .setDescription("**Welcome to The Syndicate's Den!**\n\n" +
                        "Your premier destination for virtual gambling and entertainment. " +
                        "Choose from the options below to access different areas of our establishment.")
                    .addField("💰 Your Balance", "$" + String.format("%,d", balance), true)
                    .addField("🎰 Casino Status", "Open 24/7", true)
                    .addField("🎮 Games Available", "6 Games", true)
                    .addField("🎮 **Casino Games**", "Dice, Roulette, Slots, Blackjack, Russian Roulette, Coin Flip", false)
                    .addField("💵 **Economy System**", "Manage your money and view leaderboards", false)
                    .addField("ℹ️ **Information Hub**", "Get help, rules, and bot information", false)
                    .setColor(new Color(138, 43, 226)) 
                    .setFooter("The Syndicate's Den • Your Gateway to Virtual Gaming")
                    .setTimestamp(java.time.Instant.now());

            event.replyEmbeds(embed.build())
                .addComponents(
                    ActionRow.of(
                        Button.primary("main_to_gamble", "🎰 Casino Games"),
                        Button.primary("main_to_economy", "💰 Economy"),
                        Button.secondary("main_to_info", "ℹ️ Information")
                    ),
                    ActionRow.of(
                        Button.secondary("main_refresh", "🔄 Refresh"),
                        Button.secondary("main_close", "❌ Close Menu")
                    )
                ).queue();
                
        } catch (Exception e) {
            event.reply("❌ An error occurred while loading the main menu!").setEphemeral(true).queue();
        }
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("menu", "Main menu hub");
    }

    @Override
    public String getName() {
        return "menu";
    }

    @Override
    public String getDescription() {
        return "Main menu hub";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.INFORMATION;
    }

    @Override
    public boolean requiresCommitteeRole() {
        return false;
    }
} 