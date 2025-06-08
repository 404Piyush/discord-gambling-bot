package com.syndicate.bot.commands.economy;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.Command;
import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;

public class LeaderboardCommand implements Command {

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        try {
            DatabaseManager db = SyndicateBot.getDatabaseManager();
            String guildId = event.getGuild().getId();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🏆 Casino Leaderboard");
            embed.setColor(Color.ORANGE);
            embed.setDescription("Top 10 richest players in the casino");

            
            
            StringBuilder leaderboard = new StringBuilder();
            leaderboard.append("🥇 **Top Player** - Coming Soon!\n");
            leaderboard.append("🥈 **Second Place** - Coming Soon!\n");
            leaderboard.append("🥉 **Third Place** - Coming Soon!\n");
            leaderboard.append("\n*Leaderboard feature will be fully implemented in the next update.*\n");
            leaderboard.append("*Play games to prepare for the rankings!*");
            
            embed.addField("💰 Top Players", leaderboard.toString(), false);
            
            
            try {
                long userBalance = db.getUserBalance(guildId, event.getAuthor().getId());
                embed.addField("Your Balance", 
                        String.format("💳 %,d credits", userBalance), true);
                        
                if (userBalance > 0) {
                    embed.addField("Keep Playing!", 
                            "Climb the ranks and become the casino king! 👑", true);
                }
            } catch (SQLException e) {
                
            }

            embed.setFooter("The Syndicate's Den • Leaderboard", event.getJDA().getSelfUser().getAvatarUrl());
            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ Error retrieving leaderboard. Please try again.").queue();
        }
    }

    @Override
    public String getName() {
        return "leaderboard";
    }

    @Override
    public String getDescription() {
        return "Show the casino leaderboard with the richest players";
    }

    @Override
    public String getUsage() {
        return "!leaderboard";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.ECONOMY;
    }

    @Override
    public boolean requiresCommitteeRole() {
        return false;
    }
} 