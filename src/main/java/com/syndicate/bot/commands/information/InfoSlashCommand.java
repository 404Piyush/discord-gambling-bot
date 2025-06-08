package com.syndicate.bot.commands.information;

import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.*;

public class InfoSlashCommand implements SlashCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        
        if (subcommand == null) {
            
            showInfoMenu(event);
            return;
        }
        
        switch (subcommand) {
            case "help":
                showHelpInfo(event);
                break;
            case "commands":
                showCommandsInfo(event);
                break;
            case "games":
                showGamesInfo(event);
                break;
            case "stats":
                showStatsInfo(event);
                break;
            case "menu":
                showInfoMenu(event);
                break;
            default:
                event.reply("❌ Unknown info command!").setEphemeral(true).queue();
        }
    }

    private void showInfoMenu(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("ℹ️ **The Syndicate's Den - Information Center**")
                .setDescription("**🎯 Welcome to our information hub!**\n\n" +
                    "Get all the information you need about our casino, commands, and games. " +
                    "Select a category below to learn more about our services.\n\n" +
                    "📋 **Choose an information category:**")
                .addField("🎮 **Games Available**", 
                    "• Blackjack with auto-win on 21\n" +
                    "• American Roulette (0, 00, 1-36)\n" +
                    "• Custom Dice with snake eyes/boxcars\n" +
                    "• Classic Slots with jackpots", false)
                .addField("💰 **Economy Features**", 
                    "• Starting balance: $1,000\n" +
                    "• Player transfers enabled\n" +
                    "• Real-time leaderboards\n" +
                    "• Comprehensive transaction history", false)
                .addField("🛡️ **Security & Fairness**", 
                    "• Anti-cheating session management\n" +
                    "• Provably fair game mechanics\n" +
                    "• Committee oversight system\n" +
                    "• Complete transaction logging", false)
                .setColor(Color.CYAN)
                .setFooter("The Syndicate's Den • Information Center")
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build())
            .addComponents(
                ActionRow.of(
                    Button.primary("info_help", "❓ General Help"),
                    Button.success("info_commands", "📜 Command List"),
                    Button.secondary("info_games", "🎮 Game Rules"),
                    Button.danger("info_stats", "📊 Server Stats")
                ),
                ActionRow.of(
                    Button.primary("info_economy", "💰 Economy Guide"),
                    Button.secondary("info_committee", "🏛️ Committee Info"),
                    Button.danger("info_close", "❌ Close Menu")
                )
            ).setEphemeral(true).queue();
    }

    private void showHelpInfo(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❓ **General Help & Getting Started**")
                .setDescription("**Welcome to The Syndicate's Den!** Here's everything you need to know to get started.")
                .addField("🎯 **Quick Start Guide**", 
                    "1️⃣ Use `/economy balance` to check your starting $1,000\n" +
                    "2️⃣ Try `/gamble menu` to see available games\n" +
                    "3️⃣ Start with `/gamble dice` for easy wins\n" +
                    "4️⃣ Check `/economy leaderboard` to see rankings", false)
                .addField("💡 **Pro Tips**", 
                    "• Each game has different odds and strategies\n" +
                    "• You cannot run multiple games simultaneously\n" +
                    "• Games auto-timeout after 5 minutes\n" +
                    "• All transactions are logged for transparency", false)
                .addField("🆘 **Need More Help?**", 
                    "• Use `/info commands` for full command list\n" +
                    "• Use `/info games` for detailed game rules\n" +
                    "• Contact committee members for support", false)
                .setColor(Color.GREEN)
                .setFooter("The Syndicate's Den • Help Center")
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build())
            .addComponents(
                ActionRow.of(
                    Button.secondary("info_back", "🔙 Back to Menu"),
                    Button.primary("info_commands", "📜 Commands"),
                    Button.success("info_games", "🎮 Games"),
                    Button.danger("info_close", "❌ Close")
                )
            ).setEphemeral(true).queue();
    }

    private void showCommandsInfo(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📜 **Complete Command Reference**")
                .setDescription("**All available commands organized by category.**")
                .addField("🎰 **Gambling Commands**", 
                    "`/gamble menu` - Interactive casino menu\n" +
                    "`/gamble dice` - Play dice with custom bets\n" +
                    "`/gamble roulette [bet]` - American roulette\n" +
                    "`/gamble blackjack [bet]` - Classic 21\n" +
                    "`/gamble slots [bet]` - 3-reel slot machine", false)
                .addField("💰 **Economy Commands**", 
                    "`/economy balance` - Check your balance\n" +
                    "`/economy transfer @user [amount]` - Send money\n" +
                    "`/economy leaderboard` - View top players", false)
                .addField("ℹ️ **Information Commands**", 
                    "`/info menu` - This information center\n" +
                    "`/info help` - Getting started guide\n" +
                    "`/info games` - Detailed game rules\n" +
                    "`/info stats` - Server statistics", false)
                .addField("🛠️ **Management Commands (Committee Only)**", 
                    "`/manage committee` - Manage committee members\n" +
                    "`/manage bankstatus` - Casino financial health\n" +
                    "`/manage setodds` - Configure game settings", false)
                .setColor(Color.BLUE)
                .setFooter("The Syndicate's Den • Command Reference")
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build())
            .addComponents(
                ActionRow.of(
                    Button.secondary("info_back", "🔙 Back to Menu"),
                    Button.primary("info_help", "❓ Help"),
                    Button.success("info_games", "🎮 Games"),
                    Button.danger("info_close", "❌ Close")
                )
            ).setEphemeral(true).queue();
    }

    private void showGamesInfo(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎮 **Game Rules & Information**")
                .setDescription("**Complete guide to all casino games and their rules.**")
                .addField("🎲 **Dice Game**", 
                    "**Objective**: Bet on dice roll outcomes\n" +
                    "**Rolls**: Two 6-sided dice (2-12)\n" +
                    "**Special**: Snake Eyes (2) and Boxcars (12) pay extra\n" +
                    "**Betting**: Custom amounts via interactive menu\n" +
                    "**House Edge**: ~2.8% (varies by bet type)", false)
                .addField("🎰 **Roulette**", 
                    "**Type**: American Roulette (0, 00, 1-36)\n" +
                    "**Bets**: Single numbers, colors, odd/even, dozens\n" +
                    "**Payouts**: 35:1 (single), 2:1 (dozens), 1:1 (colors)\n" +
                    "**Features**: Authentic spinning animation\n" +
                    "**House Edge**: 5.26% (standard American)", false)
                .addField("♠️ **Blackjack**", 
                    "**Goal**: Get closer to 21 than dealer without busting\n" +
                    "**Actions**: Hit, Stand, Double Down, Split\n" +
                    "**Special**: Natural 21 = instant win (3:2 payout)\n" +
                    "**Features**: No dealer hints, pure strategy\n" +
                    "**House Edge**: ~0.5% (with optimal play)", false)
                .addField("🎰 **Slots**", 
                    "**Type**: Classic 3-reel slot machine\n" +
                    "**Symbols**: Various combinations and payouts\n" +
                    "**Features**: Progressive jackpot potential\n" +
                    "**Betting**: Fixed bet amounts\n" +
                    "**House Edge**: ~5% (typical slot machine)", false)
                .setColor(Color.MAGENTA)
                .setFooter("The Syndicate's Den • Game Rules")
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build())
            .addComponents(
                ActionRow.of(
                    Button.secondary("info_back", "🔙 Back to Menu"),
                    Button.primary("info_help", "❓ Help"),
                    Button.success("info_commands", "📜 Commands"),
                    Button.danger("info_close", "❌ Close")
                )
            ).setEphemeral(true).queue();
    }

    private void showStatsInfo(SlashCommandInteractionEvent event) {
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 **Server Statistics**")
                .setDescription("**Casino performance and player activity.**")
                .addField("🎰 **Casino Stats**", 
                    "Use `/manage bankstatus` for detailed financial info\n" +
                    "(Committee members only)", false)
                .addField("📈 **Player Activity**", 
                    "• Check `/economy leaderboard` for top players\n" +
                    "• All transactions are logged in real-time\n" +
                    "• Session management prevents cheating", false)
                .addField("🔧 **System Health**", 
                    "• Auto-updating commands (no outdated errors)\n" +
                    "• Professional error handling\n" +
                    "• Comprehensive session tracking", false)
                .setColor(Color.ORANGE)
                .setFooter("The Syndicate's Den • Statistics")
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build())
            .addComponents(
                ActionRow.of(
                    Button.secondary("info_back", "🔙 Back to Menu"),
                    Button.primary("info_help", "❓ Help"),
                    Button.success("info_commands", "📜 Commands"),
                    Button.danger("info_close", "❌ Close")
                )
            ).setEphemeral(true).queue();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("info", "Help and information")
                .addSubcommands(
                        new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("menu", "Info menu"),
                        new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("help", "How to use the bot"),
                        new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("commands", "List all commands"),
                        new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("games", "Game rules"),
                        new net.dv8tion.jda.api.interactions.commands.build.SubcommandData("stats", "Server stats")
                );
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Help and information";
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