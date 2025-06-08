package com.syndicate.bot.commands.economy;

import com.syndicate.bot.commands.SlashCommand;
import com.syndicate.bot.database.DatabaseManager;
import com.syndicate.bot.game.GameSessionManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class EconomySlashCommand implements SlashCommand {
    private final DatabaseManager databaseManager;

    public EconomySlashCommand(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        
        if (subcommand == null) {
            
            showEconomyMenu(event);
            return;
        }
        
        switch (subcommand) {
            case "balance":
                handleBalance(event);
                break;
            case "transfer":
                handleTransfer(event);
                break;
            case "leaderboard":
                handleLeaderboard(event);
                break;
            default:
                event.reply("❌ Unknown economy command!").setEphemeral(true).queue();
        }
    }

    private void showEconomyMenu(SlashCommandInteractionEvent event) {
        try {
            String guildId = event.getGuild().getId();
            String userId = event.getUser().getId();
            
            
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            
            int userRank = -1;
            try {
                userRank = databaseManager.getUserRank(guildId, userId);
            } catch (SQLException e) {
                
            }
            
            
            int totalPlayers = 0;
            long totalWealth = 0;
            try {
                totalPlayers = databaseManager.getTotalPlayers(guildId);
                totalWealth = databaseManager.getTotalServerWealth(guildId);
            } catch (SQLException e) {
                
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("💰 **The Syndicate's Economy Center**")
                    .setDescription("**🎯 Welcome to your financial dashboard!**\n\n" +
                        "Manage your money, check your wealth, and see how you stack up against other players. " +
                        "Your financial empire starts here!\n\n" +
                        "💳 **Select an option below:**")
                    .addField("💰 **Your Balance**", "$" + String.format("%,d", balance), true)
                    .addField("🏆 **Your Rank**", 
                        userRank > 0 ? "#" + userRank : "Unranked", true)
                    .addField("👥 **Server Players**", String.valueOf(totalPlayers), true)
                    .addField("💎 **Financial Services**", 
                        "• Check your detailed balance\n" +
                        "• Transfer money to other players\n" +
                        "• View server wealth rankings\n" +
                        "• Track your financial progress", false)
                    .addField("📊 **Server Economy**", 
                        String.format("**Total Wealth**: $%s\n**Active Players**: %d\n**Average Balance**: $%s", 
                            String.format("%,d", totalWealth),
                            totalPlayers,
                            totalPlayers > 0 ? String.format("%,d", totalWealth / totalPlayers) : "0"), false)
                    .setColor(new Color(255, 215, 0)) 
                    .setFooter("The Syndicate's Den • Economy Center")
                    .setTimestamp(java.time.Instant.now());

            
            String activeSessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
            if (activeSessionInfo != null) {
                embed.addField("⚠️ **Active Game**", activeSessionInfo, false);
            }

            event.replyEmbeds(embed.build())
                .addComponents(
                    ActionRow.of(
                        Button.success("economy_balance", "💰 Detailed Balance"),
                        Button.primary("economy_transfer", "💸 Transfer Money"),
                        Button.secondary("economy_leaderboard", "🏆 Leaderboard"),
                        Button.danger("economy_transactions", "📋 Transaction History")
                    ),
                    ActionRow.of(
                        Button.primary("economy_stats", "📊 Your Stats"),
                        Button.secondary("economy_help", "❓ Economy Help"),
                        Button.danger("economy_close", "❌ Close Menu")
                    )
                ).setEphemeral(true).queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleBalance(SlashCommandInteractionEvent event) {
        try {
            String guildId = event.getGuild().getId();
            String userId = event.getUser().getId();
            
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            
            int userRank = databaseManager.getUserRank(guildId, userId);
            int totalPlayers = databaseManager.getTotalPlayers(guildId);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("💰 **Your Financial Profile**")
                    .setDescription("**Detailed overview of your casino finances**")
                    .addField("💵 **Current Balance**", "$" + String.format("%,d", balance), true)
                    .addField("🏆 **Server Rank**", 
                        userRank > 0 ? "#" + userRank + " of " + totalPlayers : "Unranked", true)
                    .addField("📊 **Account Status**", 
                        balance >= 10000 ? "🔥 High Roller" : 
                        balance >= 5000 ? "💎 VIP Player" : 
                        balance >= 1000 ? "🎰 Regular Player" : "🆘 Low Funds", true)
                    .addField("💡 **Quick Actions**", 
                        "• Use `/gamble menu` to start playing\n" +
                        "• Use `/economy transfer` to send money\n" +
                        "• Use `/economy leaderboard` to see rankings", false)
                    .setColor(Color.GREEN)
                    .setFooter("The Syndicate's Den • Balance Check")
                    .setTimestamp(java.time.Instant.now());

            
            String activeSessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
            if (activeSessionInfo != null) {
                embed.addField("⚠️ **Active Game**", activeSessionInfo, false);
            }

            event.replyEmbeds(embed.build())
                .addComponents(
                    ActionRow.of(
                        Button.secondary("economy_back", "🔙 Back to Menu"),
                        Button.success("economy_leaderboard", "🏆 Leaderboard"),
                        Button.primary("gamble_menu", "🎰 Start Gaming"),
                        Button.danger("economy_close", "❌ Close")
                    )
                ).setEphemeral(true).queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleTransfer(SlashCommandInteractionEvent event) {
        OptionMapping userOption = event.getOption("user");
        OptionMapping amountOption = event.getOption("amount");
        
        if (userOption == null || amountOption == null) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("💸 **Money Transfer Guide**")
                    .setDescription("**How to send money to other players**")
                    .addField("📝 **Command Format**", 
                        "`/economy transfer @user [amount]`", false)
                    .addField("💡 **Examples**", 
                        "`/economy transfer @friend 500`\n" +
                        "`/economy transfer @player 1000`\n" +
                        "`/economy transfer @someone 250`", false)
                    .addField("⚠️ **Requirements**", 
                        "• You must have sufficient balance\n" +
                        "• Minimum transfer: $1\n" +
                        "• Cannot transfer to yourself\n" +
                        "• Cannot transfer to bots", false)
                    .addField("🔒 **Security**", 
                        "• All transfers are logged\n" +
                        "• Transfers are instant and irreversible\n" +
                        "• Committee can view transaction history", false)
                    .setColor(Color.ORANGE)
                    .setFooter("The Syndicate's Den • Transfer Guide")
                    .setTimestamp(java.time.Instant.now());

            event.replyEmbeds(embed.build())
                .addComponents(
                    ActionRow.of(
                        Button.secondary("economy_back", "🔙 Back to Menu"),
                        Button.primary("economy_balance", "💰 Check Balance"),
                        Button.danger("economy_close", "❌ Close")
                    )
                ).setEphemeral(true).queue();
            return;
        }
        
        User targetUser = userOption.getAsUser();
        long amount = amountOption.getAsLong();
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        
        if (targetUser.isBot()) {
            event.reply("❌ **Transfer Failed**\n\nYou cannot transfer money to bots!")
                 .setEphemeral(true).queue();
            return;
        }
        
        if (targetUser.getId().equals(userId)) {
            event.reply("❌ **Transfer Failed**\n\nYou cannot transfer money to yourself!")
                 .setEphemeral(true).queue();
            return;
        }
        
        if (amount <= 0) {
            event.reply("❌ **Transfer Failed**\n\nTransfer amount must be greater than $0!")
                 .setEphemeral(true).queue();
            return;
        }
        
        try {
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            databaseManager.initializeUserBalance(guildId, targetUser.getId(), 1000);
            
            long senderBalance = databaseManager.getUserBalance(guildId, userId);
            
            if (senderBalance < amount) {
                event.reply("❌ **Transfer Failed - Insufficient Funds**\n\n" +
                          "💰 **Your Balance**: $" + String.format("%,d", senderBalance) + "\n" +
                          "💸 **Required**: $" + String.format("%,d", amount) + "\n" +
                          "🚫 **Shortage**: $" + String.format("%,d", amount - senderBalance))
                     .setEphemeral(true).queue();
                return;
            }
            
            long targetBalance = databaseManager.getUserBalance(guildId, targetUser.getId());
            
            
            databaseManager.updateUserBalance(guildId, userId, senderBalance - amount);
            databaseManager.updateUserBalance(guildId, targetUser.getId(), targetBalance + amount);
            
            
            databaseManager.addTransaction(guildId, userId, "transfer_out", -amount, null, 
                "Transfer to " + targetUser.getName());
            databaseManager.addTransaction(guildId, targetUser.getId(), "transfer_in", amount, null, 
                "Transfer from " + event.getUser().getName());
            
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("✅ **Transfer Successful!**")
                    .setDescription("**Money has been sent successfully**")
                    .addField("💸 **Amount Transferred**", "$" + String.format("%,d", amount), true)
                    .addField("👤 **Recipient**", targetUser.getAsMention(), true)
                    .addField("💰 **Your New Balance**", "$" + String.format("%,d", senderBalance - amount), true)
                    .addField("📋 **Transaction Details**", 
                        "• Transfer completed instantly\n" +
                        "• Transaction logged for security\n" +
                        "• Both parties notified", false)
                    .setColor(Color.GREEN)
                    .setFooter("The Syndicate's Den • Transfer Complete")
                    .setTimestamp(java.time.Instant.now());

            event.replyEmbeds(embed.build())
                .addComponents(
                    ActionRow.of(
                        Button.secondary("economy_back", "🔙 Back to Menu"),
                        Button.primary("economy_balance", "💰 Check Balance"),
                        Button.success("economy_transfer", "💸 Transfer More"),
                        Button.danger("economy_close", "❌ Close")
                    )
                ).setEphemeral(true).queue();
            
        } catch (SQLException e) {
            event.reply("❌ **Transfer Failed**\n\nDatabase error occurred. Please try again.")
                 .setEphemeral(true).queue();
        }
    }

    private void handleLeaderboard(SlashCommandInteractionEvent event) {
        try {
            
            String guildId = event.getGuild().getId();
            
            
            List<LeaderboardEntry> topPlayers = databaseManager.getTopPlayers(guildId, 10);
            
            if (topPlayers.isEmpty()) {
                event.reply("📊 **Leaderboard**\n\n" +
                          "No players found in this server yet!\n" +
                          "Start playing games to appear on the leaderboard.")
                     .setEphemeral(true)
                     .queue();
                return;
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🏆 **The Syndicate's Den - Leaderboard**")
                    .setColor(new Color(255, 215, 0))
                    .setDescription("Top richest players in this server")
                    .setThumbnail("https://cdn.discordapp.com/emojis/741057612154322985.png");
            
            StringBuilder leaderboardText = new StringBuilder();
            String[] medals = {"🥇", "🥈", "🥉"};
            
            for (int i = 0; i < topPlayers.size(); i++) {
                LeaderboardEntry entry = topPlayers.get(i);
                String position = i < 3 ? medals[i] : String.format("%d.", i + 1);
                String medal = i < 3 ? "" : "🏅";
                
                
                String userName;
                try {
                    var user = event.getJDA().getUserById(entry.getUserId());
                    userName = user != null ? user.getName() : "Unknown User";
                } catch (Exception e) {
                    userName = "Unknown User";
                }
                
                leaderboardText.append(String.format("%s **%s** %s\n", 
                                     position, userName, medal))
                               .append(String.format("💰 $%s\n\n", 
                                     String.format("%,d", entry.getBalance())));
            }
            
            embed.addField("💎 **Rankings**", leaderboardText.toString(), false);
            
            
            String userId = event.getUser().getId();
            boolean userInTopTen = topPlayers.stream()
                    .anyMatch(entry -> entry.getUserId().equals(userId));
            
            if (!userInTopTen) {
                try {
                    long userBalance = databaseManager.getUserBalance(guildId, userId);
                    int userPosition = databaseManager.getUserRank(guildId, userId);
                    
                    if (userPosition > 0) {
                        embed.addField("📍 **Your Position**", 
                                     String.format("**#%d** - $%s", 
                                                 userPosition, 
                                                 String.format("%,d", userBalance)), 
                                     true);
                    }
                } catch (SQLException e) {
                    
                }
            }
            
            
            try {
                int totalPlayers = databaseManager.getTotalPlayers(guildId);
                long totalWealth = databaseManager.getTotalServerWealth(guildId);
                
                embed.addField("📊 **Server Stats**", 
                             String.format("**Players:** %d\n**Total Wealth:** $%s", 
                                         totalPlayers, 
                                         String.format("%,d", totalWealth)), 
                             true);
            } catch (SQLException e) {
                
            }
            
            embed.setFooter("The Syndicate's Den • Updated every game", 
                          event.getJDA().getSelfUser().getAvatarUrl())
                 .setTimestamp(java.time.Instant.now());
            
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            
        } catch (Exception e) {
            event.reply("❌ Failed to load leaderboard. Please try again later.")
                 .setEphemeral(true)
                 .queue();
        }
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("economy", "Money commands")
                .addSubcommands(
                        new SubcommandData("balance", "Check your money"),
                        new SubcommandData("transfer", "Send money to someone")
                                .addOption(OptionType.USER, "user", "Who to send money to", true)
                                .addOption(OptionType.INTEGER, "amount", "How much to send", true),
                        new SubcommandData("leaderboard", "See who has the most money")
                );
    }

    @Override
    public String getName() {
        return "economy";
    }

    @Override
    public String getDescription() {
        return "Money commands";
    }

    @Override
    public com.syndicate.bot.commands.CommandCategory getCategory() {
        return com.syndicate.bot.commands.CommandCategory.ECONOMY;
    }

    @Override
    public boolean requiresCommitteeRole() {
        return false;
    }
} 