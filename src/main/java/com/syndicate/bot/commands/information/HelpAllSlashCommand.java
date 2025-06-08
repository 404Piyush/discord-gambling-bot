package com.syndicate.bot.commands.information;

import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.*;

public class HelpAllSlashCommand implements SlashCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎲 **The Syndicate's Den - Complete Command Guide**")
                .setDescription("**Welcome to The Syndicate's Den!** Your premier Discord casino experience.\n" +
                              "Below are all available commands organized by category.")
                .setColor(Color.BLUE)
                .setThumbnail("https://i.imgur.com/4M34hi2.png");

        
        embed.addField("🏠 **Main Navigation**", 
            "`/menu` - Main interactive menu (start here!)\n" +
            "`/help-all` - This complete command reference", false);

        
        embed.addField("🎰 **Casino Games**", 
            "`/gamble menu` - Interactive casino lobby\n" +
            "`/gamble dice` - Roll dice with custom betting\n" +
            "`/gamble roulette [bet]` - American roulette wheel\n" +
            "`/gamble slots [bet]` - 3-reel slot machine\n" +
            "`/gamble blackjack [bet]` - Classic 21 card game\n" +
            "`/gamble russian-roulette [bet]` - High-stakes solo game\n" +
            "`/gamble coin-flip [bet]` - Simple heads or tails", false);

        
        embed.addField("💰 **Economy System**", 
            "`/economy` - Economy dashboard\n" +
            "`/economy balance` - Check your current balance\n" +
            "`/economy transfer @user [amount]` - Send money to another player\n" +
            "`/economy leaderboard` - View top players by balance", false);

        
        embed.addField("ℹ️ **Information & Help**", 
            "`/info menu` - Information center\n" +
            "`/info help` - Getting started guide\n" +
            "`/info commands` - Command reference\n" +
            "`/info games` - Detailed game rules and strategies\n" +
            "`/info stats` - Server statistics and activity", false);

        
        embed.addField("🚀 **Quick Start Guide**", 
            "1️⃣ Type `/menu` to open the main interface\n" +
            "2️⃣ Click 🎰 **Casino** to access games\n" +
            "3️⃣ Start with `/economy balance` to check your starting credits\n" +
            "4️⃣ Try `/gamble dice` for an easy first game\n" +
            "5️⃣ Explore other games and build your fortune!", false);

        
        embed.addField("🎮 **Available Games (6 Total)**", 
            "**🎲 Dice** - Roll and bet on outcomes\n" +
            "**🎡 Roulette** - Classic casino wheel\n" +
            "**🎰 Slots** - 3-reel fruit machine\n" +
            "**🃏 Blackjack** - Beat the dealer to 21\n" +
            "**🔫 Russian Roulette** - High-risk, high-reward\n" +
            "**🪙 Coin Flip** - Simple heads or tails betting", false);

        embed.setFooter("The Syndicate's Den • Type /menu to get started • All games use interactive buttons")
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).setEphemeral(false).queue();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("help-all", "Complete command reference and quick start guide");
    }

    @Override
    public String getName() {
        return "help-all";
    }

    @Override
    public String getDescription() {
        return "Complete command reference and quick start guide";
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