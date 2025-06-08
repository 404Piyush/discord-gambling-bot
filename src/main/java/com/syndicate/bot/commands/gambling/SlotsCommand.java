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

public class SlotsCommand implements Command {
    private final Random random = new Random();
    
    
    private final String[] symbols = {"🍒", "🍋", "🍊", "🔔", "⭐", "💎", "🎰"};
    private final int[] symbolWeights = {35, 25, 20, 12, 5, 2, 1}; 
    
    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 1) {
            event.getChannel().sendMessage("❌ Usage: `!slots <bet_amount>`").queue();
            return;
        }

        try {
            long betAmount = Long.parseLong(args[0]);

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

            
            String reel1 = getRandomSymbol();
            String reel2 = getRandomSymbol();
            String reel3 = getRandomSymbol();

            
            double multiplier = calculateMultiplier(reel1, reel2, reel3);
            boolean won = multiplier > 0;

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
            String description = String.format("Slots - %s %s %s", reel1, reel2, reel3);
            db.addTransaction(guildId, userId, transactionType, won ? winnings : -betAmount, "slots", description);

            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🎰 Slot Machine Results");
            embed.setColor(won ? Color.GREEN : Color.RED);

            
            embed.addField("🎰 Reels", String.format("┌─────┬─────┬─────┐\n│  %s  │  %s  │  %s  │\n└─────┴─────┴─────┘", 
                    reel1, reel2, reel3), false);
            
            embed.addField("💰 Bet Amount", betAmount + " credits", true);

            if (won) {
                embed.addField("🎉 Result", "**YOU WON!**", false);
                embed.addField("🎯 Combination", getCombinationName(reel1, reel2, reel3), true);
                embed.addField("💵 Winnings", winnings + " credits (x" + multiplier + ")", true);
                embed.addField("💳 New Balance", newBalance + " credits", true);
            } else {
                embed.addField("💥 Result", "**You Lost**", false);
                embed.addField("💳 New Balance", newBalance + " credits", true);
            }

            
            embed.addField("💡 Payouts", getPayoutInfo(), false);

            embed.setFooter("The Syndicate's Den • Slot Machine", event.getJDA().getSelfUser().getAvatarUrl());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Invalid bet amount! Please enter a valid number.").queue();
        } catch (SQLException e) {
            event.getChannel().sendMessage("❌ Database error occurred. Please try again.").queue();
        }
    }

    private String getRandomSymbol() {
        int rand = random.nextInt(100);
        int cumulative = 0;
        
        for (int i = 0; i < symbolWeights.length; i++) {
            cumulative += symbolWeights[i];
            if (rand < cumulative) {
                return symbols[i];
            }
        }
        
        return symbols[0]; 
    }

    private double calculateMultiplier(String reel1, String reel2, String reel3) {
        
        if (reel1.equals(reel2) && reel2.equals(reel3)) {
            return switch (reel1) {
                case "🍒" -> 5.0;   
                case "🍋" -> 8.0;   
                case "🍊" -> 12.0;  
                case "🔔" -> 20.0;  
                case "⭐" -> 50.0;  
                case "💎" -> 100.0; 
                case "🎰" -> 500.0; 
                default -> 0.0;
            };
        }
        
        
        if (reel1.equals(reel2) || reel2.equals(reel3)) {
            String symbol = reel1.equals(reel2) ? reel1 : reel2;
            return switch (symbol) {
                case "🍒" -> 1.5;   
                case "🍋" -> 2.0;   
                case "🍊" -> 2.5;   
                case "🔔" -> 3.0;   
                case "⭐" -> 5.0;   
                case "💎" -> 10.0;  
                case "🎰" -> 25.0;  
                default -> 0.0;
            };
        }
        
        
        int cherryCount = 0;
        if (reel1.equals("🍒")) cherryCount++;
        if (reel2.equals("🍒")) cherryCount++;
        if (reel3.equals("🍒")) cherryCount++;
        
        if (cherryCount >= 2) {
            return 1.2;
        }
        
        return 0.0; 
    }

    private String getCombinationName(String reel1, String reel2, String reel3) {
        if (reel1.equals(reel2) && reel2.equals(reel3)) {
            return switch (reel1) {
                case "🍒" -> "Triple Cherries";
                case "🍋" -> "Triple Lemons";
                case "🍊" -> "Triple Oranges";
                case "🔔" -> "Triple Bells";
                case "⭐" -> "Triple Stars";
                case "💎" -> "Triple Diamonds";
                case "🎰" -> "🎉 JACKPOT! 🎉";
                default -> "Triple Match";
            };
        } else if (reel1.equals(reel2) || reel2.equals(reel3)) {
            return "Double Match";
        } else {
            return "Cherry Bonus";
        }
    }

    private String getPayoutInfo() {
        return """
            **Triple Matches:** 🎰(500x) 💎(100x) ⭐(50x) 🔔(20x) 🍊(12x) 🍋(8x) 🍒(5x)
            **Double Matches:** 🎰(25x) 💎(10x) ⭐(5x) 🔔(3x) 🍊(2.5x) 🍋(2x) 🍒(1.5x)
            **Cherry Bonus:** Any 2+ 🍒 (1.2x)
            """;
    }

    @Override
    public String getName() {
        return "slots";
    }

    @Override
    public String getDescription() {
        return "Play the slot machine and try to hit the jackpot!";
    }

    @Override
    public String getUsage() {
        return "!slots <bet_amount>";
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