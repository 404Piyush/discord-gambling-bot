package com.syndicate.bot.commands.gambling;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.Command;
import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;
import java.util.Random;

public class DiceCommand implements Command {
    private final Random random = new Random();

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Usage: `!dice <bet_amount> <prediction>`\n" +
                    "Predictions: `high` (8-12), `low` (2-6), `lucky7` (exactly 7), or a specific number (2-12)").queue();
            return;
        }

        try {
            long betAmount = Long.parseLong(args[0]);
            String prediction = args[1].toLowerCase();

            if (betAmount <= 0) {
                event.getChannel().sendMessage("❌ Bet amount must be positive!").queue();
                return;
            }

            DatabaseManager db = SyndicateBot.getDatabaseManager();
            String guildId = event.getGuild().getId();
            String userId = event.getAuthor().getId();

            long userBalance = db.getUserBalance(guildId, userId);
            if (userBalance < betAmount) {
                event.getChannel().sendMessage("❌ Insufficient funds! Your balance: " + userBalance + " credits").queue();
                return;
            }

            
            int die1 = random.nextInt(6) + 1;
            int die2 = random.nextInt(6) + 1;
            int total = die1 + die2;

            
            boolean won = false;
            double multiplier = 0;

            switch (prediction) {
                case "high":
                    won = total >= 8;
                    multiplier = 1.8; 
                    break;
                case "low":
                    won = total <= 6;
                    multiplier = 1.8; 
                    break;
                case "lucky7":
                    won = total == 7;
                    multiplier = 4.0; 
                    break;
                default:
                    try {
                        int specificNumber = Integer.parseInt(prediction);
                        if (specificNumber >= 2 && specificNumber <= 12) {
                            won = total == specificNumber;
                            
                            multiplier = calculateSpecificNumberMultiplier(specificNumber);
                        } else {
                            event.getChannel().sendMessage("❌ Specific number must be between 2 and 12!").queue();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        event.getChannel().sendMessage("❌ Invalid prediction! Use `high`, `low`, `lucky7`, or a number (2-12)").queue();
                        return;
                    }
                    break;
            }

            
            long winnings = 0;
            long newBalance;
            long casinoChange = betAmount;

            if (won) {
                winnings = (long) (betAmount * multiplier);
                newBalance = userBalance + winnings - betAmount;
                casinoChange = -winnings + betAmount; 
            } else {
                newBalance = userBalance - betAmount;
                
            }

            
            db.updateUserBalance(guildId, userId, newBalance);
            long casinoBalance = db.getCasinoBankBalance(guildId);
            db.updateCasinoBankBalance(guildId, casinoBalance + casinoChange);

            
            String transactionType = won ? "WIN" : "LOSS";
            String description = String.format("Dice game - Rolled %d+%d=%d, Predicted %s", die1, die2, total, prediction);
            db.addTransaction(guildId, userId, transactionType, won ? winnings : -betAmount, "dice", description);

            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🎲 Dice Roll Results");
            embed.setColor(won ? Color.GREEN : Color.RED);
            
            embed.addField("🎯 Your Prediction", prediction, true);
            embed.addField("🎲 Dice Roll", String.format("🎲 %d + 🎲 %d = **%d**", die1, die2, total), true);
            embed.addField("💰 Bet Amount", betAmount + " credits", true);

            if (won) {
                embed.addField("🎉 Result", "**YOU WON!**", false);
                embed.addField("💵 Winnings", winnings + " credits", true);
                embed.addField("💳 New Balance", newBalance + " credits", true);
            } else {
                embed.addField("💥 Result", "**You Lost**", false);
                embed.addField("💳 New Balance", newBalance + " credits", true);
            }

            embed.setFooter("The Syndicate's Den • Dice Game", event.getJDA().getSelfUser().getAvatarUrl());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Invalid bet amount! Please enter a valid number.").queue();
        } catch (SQLException e) {
            event.getChannel().sendMessage("❌ Database error occurred. Please try again.").queue();
        }
    }

    private double calculateSpecificNumberMultiplier(int number) {
        
        
        return switch (number) {
            case 2, 12 -> 30.0; 
            case 3, 11 -> 15.0; 
            case 4, 10 -> 10.0; 
            case 5, 9 -> 7.0;   
            case 6, 8 -> 5.0;   
            case 7 -> 4.0;      
            default -> 2.0;
        };
    }

    @Override
    public String getName() {
        return "dice";
    }

    @Override
    public String getDescription() {
        return "Roll two dice and bet on the outcome";
    }

    @Override
    public String getUsage() {
        return "!dice <bet_amount> <prediction>\nPredictions: high (8-12), low (2-6), lucky7 (exactly 7), or specific number (2-12)";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.GAMBLING;
    }

    @Override
    public boolean requiresCommitteeRole() {
        return false;
    }
} 