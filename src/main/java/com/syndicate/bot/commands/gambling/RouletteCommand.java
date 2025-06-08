package com.syndicate.bot.commands.gambling;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.Command;
import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RouletteCommand implements Command {
    private final Random random = new Random();
    
    
    private final List<Integer> redNumbers = Arrays.asList(
        1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36
    );

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            event.getChannel().sendMessage("❌ Usage: `!roulette <bet_amount> <bet_type>`\n" +
                    "Bet types: `red`, `black`, `odd`, `even`, `high` (19-36), `low` (1-18), or a specific number (0-36)").queue();
            return;
        }

        try {
            long betAmount = Long.parseLong(args[0]);
            String betType = args[1].toLowerCase();

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

            
            int result = random.nextInt(37);
            
            
            boolean won = false;
            double multiplier = 0;
            String betDescription = betType;

            switch (betType) {
                case "red":
                    won = redNumbers.contains(result) && result != 0;
                    multiplier = 2.0;
                    break;
                case "black":
                    won = !redNumbers.contains(result) && result != 0;
                    multiplier = 2.0;
                    break;
                case "odd":
                    won = result % 2 == 1 && result != 0;
                    multiplier = 2.0;
                    break;
                case "even":
                    won = result % 2 == 0 && result != 0;
                    multiplier = 2.0;
                    break;
                case "high":
                    won = result >= 19 && result <= 36;
                    multiplier = 2.0;
                    break;
                case "low":
                    won = result >= 1 && result <= 18;
                    multiplier = 2.0;
                    break;
                default:
                    try {
                        int specificNumber = Integer.parseInt(betType);
                        if (specificNumber >= 0 && specificNumber <= 36) {
                            won = result == specificNumber;
                            multiplier = 36.0; 
                            betDescription = "number " + specificNumber;
                        } else {
                            event.getChannel().sendMessage("❌ Number must be between 0 and 36!").queue();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        event.getChannel().sendMessage("❌ Invalid bet type! Use `red`, `black`, `odd`, `even`, `high`, `low`, or a number (0-36)").queue();
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
            String description = String.format("Roulette - Ball landed on %d, Bet on %s", result, betDescription);
            db.addTransaction(guildId, userId, transactionType, won ? winnings : -betAmount, "roulette", description);

            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🎰 Roulette Results");
            embed.setColor(won ? Color.GREEN : Color.RED);
            
            
            String resultColor;
            String resultEmoji;
            if (result == 0) {
                resultColor = "🟢 GREEN";
                resultEmoji = "🟢";
            } else if (redNumbers.contains(result)) {
                resultColor = "🔴 RED";
                resultEmoji = "🔴";
            } else {
                resultColor = "⚫ BLACK";
                resultEmoji = "⚫";
            }

            embed.addField("🎯 Your Bet", betDescription, true);
            embed.addField("🎰 Ball Landed On", String.format("%s **%d** (%s)", resultEmoji, result, resultColor), true);
            embed.addField("💰 Bet Amount", betAmount + " credits", true);

            if (won) {
                embed.addField("🎉 Result", "**YOU WON!**", false);
                embed.addField("💵 Winnings", winnings + " credits", true);
                embed.addField("💳 New Balance", newBalance + " credits", true);
            } else {
                embed.addField("💥 Result", "**You Lost**", false);
                embed.addField("💳 New Balance", newBalance + " credits", true);
            }

            
            if (result == 0) {
                embed.addField("🍀 House Edge", "Green 0 - The house always has an edge!", false);
            }

            embed.setFooter("The Syndicate's Den • Roulette", event.getJDA().getSelfUser().getAvatarUrl());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Invalid bet amount! Please enter a valid number.").queue();
        } catch (SQLException e) {
            event.getChannel().sendMessage("❌ Database error occurred. Please try again.").queue();
        }
    }

    @Override
    public String getName() {
        return "roulette";
    }

    @Override
    public String getDescription() {
        return "Play European roulette with various betting options";
    }

    @Override
    public String getUsage() {
        return "!roulette <bet_amount> <bet_type>\nBet types: red, black, odd, even, high (19-36), low (1-18), or specific number (0-36)";
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