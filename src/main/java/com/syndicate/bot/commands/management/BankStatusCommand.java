package com.syndicate.bot.commands.management;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.Command;
import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;

public class BankStatusCommand implements Command {

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        try {
            DatabaseManager db = SyndicateBot.getDatabaseManager();
            String guildId = event.getGuild().getId();
            String userId = event.getAuthor().getId();

            
            if (!db.isCommitteeMember(guildId, userId)) {
                event.getChannel().sendMessage("❌ This command is restricted to committee members only!").queue();
                return;
            }

            long casinoBalance = db.getCasinoBankBalance(guildId);
            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🏦 Casino Bank Status");
            embed.setColor(Color.BLUE);
            embed.setDescription("Financial overview of The Syndicate's Den");

            embed.addField("💰 Total Casino Funds", String.format("%,d credits", casinoBalance), true);
            
            
            String healthStatus;
            String healthEmoji;
            Color healthColor;
            
            if (casinoBalance >= 5000000) {
                healthStatus = "Excellent";
                healthEmoji = "💎";
                healthColor = Color.GREEN;
            } else if (casinoBalance >= 1000000) {
                healthStatus = "Good";
                healthEmoji = "💚";
                healthColor = Color.GREEN;
            } else if (casinoBalance >= 500000) {
                healthStatus = "Stable";
                healthEmoji = "💛";
                healthColor = Color.ORANGE;
            } else if (casinoBalance >= 100000) {
                healthStatus = "Caution";
                healthEmoji = "⚠️";
                healthColor = Color.ORANGE;
            } else {
                healthStatus = "Critical";
                healthEmoji = "🚨";
                healthColor = Color.RED;
            }
            
            embed.setColor(healthColor);
            embed.addField(healthEmoji + " Financial Health", healthStatus, true);
            
            
            String recommendations;
            if (casinoBalance < 100000) {
                recommendations = "• Consider adjusting game odds\n• Reduce maximum bet limits\n• Monitor player activities closely";
            } else if (casinoBalance < 500000) {
                recommendations = "• Monitor high-stakes gambling\n• Consider promotional events\n• Review game profitability";
            } else {
                recommendations = "• Casino is performing well\n• Consider expanding game options\n• Plan investment opportunities";
            }
            
            embed.addField("📊 Recommendations", recommendations, false);
            
            
            embed.addField("🎰 Casino Operations", 
                    "• House Edge: 5% (default)\n• Status: Operational\n• Games: Dice, Roulette, Slots, Blackjack", 
                    false);
                    
            embed.addField("⚙️ Committee Controls", 
                    "Use `!setodds` to adjust game odds\n" +
                    "Use `!committee` to manage committee members", 
                    false);

            embed.setFooter("The Syndicate's Den • Committee Access Only", event.getJDA().getSelfUser().getAvatarUrl());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (SQLException e) {
            event.getChannel().sendMessage("❌ Database error occurred. Please try again.").queue();
        }
    }

    @Override
    public String getName() {
        return "bankstatus";
    }

    @Override
    public String getDescription() {
        return "View the casino's financial status and health (Committee only)";
    }

    @Override
    public String getUsage() {
        return "!bankstatus";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MANAGEMENT;
    }

    @Override
    public boolean requiresCommitteeRole() {
        return true;
    }
} 