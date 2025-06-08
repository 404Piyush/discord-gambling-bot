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
import java.util.List;

public class PayCommand implements Command {

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Usage: `!pay @user <amount>`").queue();
            return;
        }

        List<User> mentionedUsers = event.getMessage().getMentions().getUsers();
        if (mentionedUsers.isEmpty()) {
            event.getChannel().sendMessage("❌ Please mention a user to pay!").queue();
            return;
        }

        User recipient = mentionedUsers.get(0);
        if (recipient.equals(event.getAuthor())) {
            event.getChannel().sendMessage("❌ You cannot pay yourself!").queue();
            return;
        }

        if (recipient.isBot()) {
            event.getChannel().sendMessage("❌ You cannot pay bots!").queue();
            return;
        }

        try {
            long amount = Long.parseLong(args[1]);
            if (amount <= 0) {
                event.getChannel().sendMessage("❌ Amount must be positive!").queue();
                return;
            }

            DatabaseManager db = SyndicateBot.getDatabaseManager();
            String guildId = event.getGuild().getId();
            String senderId = event.getAuthor().getId();
            String recipientId = recipient.getId();

            
            db.initializeUserBalance(guildId, senderId, 1000);
            db.initializeUserBalance(guildId, recipientId, 1000);

            long senderBalance = db.getUserBalance(guildId, senderId);
            if (senderBalance < amount) {
                event.getChannel().sendMessage("❌ Insufficient funds! Your balance: " + senderBalance + " credits").queue();
                return;
            }

            
            long newSenderBalance = senderBalance - amount;
            long recipientBalance = db.getUserBalance(guildId, recipientId);
            long newRecipientBalance = recipientBalance + amount;

            db.updateUserBalance(guildId, senderId, newSenderBalance);
            db.updateUserBalance(guildId, recipientId, newRecipientBalance);

            
            db.addTransaction(guildId, senderId, "TRANSFER_OUT", -amount, null, "Paid " + recipient.getAsTag());
            db.addTransaction(guildId, recipientId, "TRANSFER_IN", amount, null, "Received from " + event.getAuthor().getAsTag());

            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("💸 Payment Successful");
            embed.setColor(Color.GREEN);

            embed.addField("💰 Amount Transferred", amount + " credits", true);
            embed.addField("👤 From", event.getAuthor().getAsMention(), true);
            embed.addField("👤 To", recipient.getAsMention(), true);
            
            embed.addField("💳 Your New Balance", newSenderBalance + " credits", true);
            embed.addField("💳 Recipient's New Balance", newRecipientBalance + " credits", true);
            embed.addField("📊 Transaction ID", "TXN-" + System.currentTimeMillis(), true);

            embed.setFooter("The Syndicate's Den • Payment System", event.getJDA().getSelfUser().getAvatarUrl());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Invalid amount! Please enter a valid number.").queue();
        } catch (SQLException e) {
            event.getChannel().sendMessage("❌ Database error occurred. Please try again.").queue();
        }
    }

    @Override
    public String getName() {
        return "pay";
    }

    @Override
    public String getDescription() {
        return "Transfer credits to another user";
    }

    @Override
    public String getUsage() {
        return "!pay @user <amount>";
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