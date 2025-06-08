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

public class BalanceCommand implements Command {

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        User targetUser = event.getAuthor();
        
        
        if (args.length > 0 && !event.getMessage().getMentions().getUsers().isEmpty()) {
            targetUser = event.getMessage().getMentions().getUsers().get(0);
        }

        try {
            DatabaseManager db = SyndicateBot.getDatabaseManager();
            String guildId = event.getGuild().getId();
            String userId = targetUser.getId();

            
            db.initializeUserBalance(guildId, userId, 1000);
            
            long balance = db.getUserBalance(guildId, userId);
            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("💰 Balance Information");
            embed.setColor(Color.CYAN);
            embed.setThumbnail(targetUser.getAvatarUrl());

            embed.addField("👤 Player", targetUser.getAsMention(), true);
            embed.addField("💳 Current Balance", balance + " credits", true);
            embed.addField("🎰 Casino Status", balance > 0 ? "Ready to gamble!" : "Need more credits!", true);

            
            String balanceCategory;
            String categoryEmoji;
            if (balance >= 100000) {
                balanceCategory = "High Roller";
                categoryEmoji = "💎";
            } else if (balance >= 10000) {
                balanceCategory = "VIP Player";
                categoryEmoji = "👑";
            } else if (balance >= 5000) {
                balanceCategory = "Regular Player";
                categoryEmoji = "🎯";
            } else if (balance >= 1000) {
                balanceCategory = "Casual Player";
                categoryEmoji = "🎲";
            } else {
                balanceCategory = "Low Stakes";
                categoryEmoji = "🍀";
            }

            embed.addField(categoryEmoji + " Player Level", balanceCategory, false);

            if (targetUser.equals(event.getAuthor())) {
                embed.setDescription("*Use `!pay @user <amount>` to transfer credits to other players.*");
            }

            embed.setFooter("The Syndicate's Den • Balance Check", event.getJDA().getSelfUser().getAvatarUrl());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (SQLException e) {
            event.getChannel().sendMessage("❌ Database error occurred. Please try again.").queue();
        }
    }

    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public String getDescription() {
        return "Check your or another user's current balance";
    }

    @Override
    public String getUsage() {
        return "!balance [@user]";
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