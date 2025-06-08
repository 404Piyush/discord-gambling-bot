package com.syndicate.bot.commands.information;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.Command;
import com.syndicate.bot.commands.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;

public class StatsCommand implements Command {
    private static final Logger logger = LoggerFactory.getLogger(StatsCommand.class);

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        try {
            MessageEmbed embed = createStatsEmbed(event);
            event.getChannel().sendMessageEmbeds(embed).queue();
        } catch (Exception e) {
            logger.error("Error in stats command", e);
            event.getChannel().sendMessage("❌ Error retrieving statistics!").queue();
        }
    }

    private MessageEmbed createStatsEmbed(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🎰 The Syndicate's Den - Statistics")
                .setColor(new Color(255, 215, 0))
                .setTimestamp(Instant.now());

        
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        String uptimeStr = formatUptime(uptime);
        
        embed.addField("🤖 Bot Statistics", 
                "**Uptime:** " + uptimeStr + "\n" +
                "**Servers:** " + SyndicateBot.getJDA().getGuilds().size() + "\n" +
                "**Users:** " + SyndicateBot.getJDA().getUsers().size(), 
                true);

        
        if (event.getGuild() != null) {
            embed.addField("🏰 Server Statistics",
                    "**Members:** " + event.getGuild().getMemberCount() + "\n" +
                    "**Channels:** " + event.getGuild().getChannels().size() + "\n" +
                    "**Roles:** " + event.getGuild().getRoles().size(),
                    true);
        }

        
        try {
            DatabaseStats dbStats = getDatabaseStats();
            embed.addField("💾 Database Statistics",
                    "**Total Users:** " + dbStats.totalUsers + "\n" +
                    "**Total Transactions:** " + dbStats.totalTransactions + "\n" +
                    "**Committee Members:** " + dbStats.committeeMembers,
                    true);

            embed.addField("🏦 Casino Statistics",
                    "**Bank Balance:** $" + String.format("%,d", dbStats.bankBalance) + "\n" +
                    "**Total Winnings:** $" + String.format("%,d", dbStats.totalWinnings) + "\n" +
                    "**Games Played:** " + String.format("%,d", dbStats.gamesPlayed),
                    true);
        } catch (Exception e) {
            logger.error("Error getting database stats", e);
            embed.addField("💾 Database Statistics", "Error retrieving data", true);
        }

        embed.setFooter("The Syndicate's Den", event.getJDA().getSelfUser().getAvatarUrl());
        return embed.build();
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private DatabaseStats getDatabaseStats() throws Exception {
        DatabaseStats stats = new DatabaseStats();
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:syndicate_den.db")) {
            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM user_balances")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.totalUsers = rs.getInt(1);
                }
            }

            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM transactions")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.totalTransactions = rs.getInt(1);
                }
            }

            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM committee_members")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.committeeMembers = rs.getInt(1);
                }
            }

            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT total_funds FROM casino_bank LIMIT 1")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.bankBalance = rs.getLong(1);
                }
            }

            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE amount > 0")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.totalWinnings = rs.getLong(1);
                }
            }

            
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM transactions WHERE description LIKE '%dice%' OR " +
                    "description LIKE '%roulette%' OR description LIKE '%slots%' OR " +
                    "description LIKE '%blackjack%'")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.gamesPlayed = rs.getInt(1);
                }
            }
        }
        
        return stats;
    }

    private static class DatabaseStats {
        int totalUsers = 0;
        int totalTransactions = 0;
        int committeeMembers = 0;
        long bankBalance = 0;
        long totalWinnings = 0;
        int gamesPlayed = 0;
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Display bot and casino statistics";
    }

    @Override
    public String getUsage() {
        return "!stats";
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