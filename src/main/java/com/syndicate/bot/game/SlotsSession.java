package com.syndicate.bot.game;

import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.*;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SlotsSession extends GameSession {
    private final DatabaseManager databaseManager;
    private final SlashCommandInteractionEvent originalEvent;
    private final Random random = new Random();
    private Message gameMessage;
    
    
    private final String[] symbols = {"🍒", "🍊", "🍋", "🍇", "⭐", "💎", "🎰", "🔔"};
    private final int[] weights = {25, 20, 18, 15, 10, 7, 3, 2}; 
    
    public SlotsSession(String userId, String guildId, long bet, DatabaseManager databaseManager, SlashCommandInteractionEvent originalEvent) {
        super(userId, guildId, bet);
        this.databaseManager = databaseManager;
        this.originalEvent = originalEvent;
    }

    public void startGame() {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎰 Slot Machine")
                .setDescription("🎲 **SPINNING...** 🎲")
                .addField("Your Bet", bet + " credits", true)
                .setColor(Color.YELLOW)
                .setTimestamp(java.time.Instant.now());

        originalEvent.replyEmbeds(embed.build())
                .addComponents(ActionRow.of(Button.secondary("slots_spinning", "🎰 Spinning...").asDisabled()))
                .queue(response -> {
                    response.retrieveOriginal().queue(message -> {
                        this.gameMessage = message;
                        startSpinAnimation();
                    });
                });
    }

    private void startSpinAnimation() {
        CompletableFuture.runAsync(() -> {
            try {
                
                String[] spinFrames = {
                    "🎰 🎰 🎰",
                    "🎲 🎲 🎲", 
                    "🔄 🔄 🔄",
                    "✨ ✨ ✨",
                    "🎰 🎰 🎰"
                };

                
                for (String frame : spinFrames) {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("🎰 Slot Machine")
                            .setDescription("**" + frame + "**\n\n🎲 **SPINNING...** 🎲")
                            .addField("Your Bet", bet + " credits", true)
                            .setColor(Color.YELLOW)
                            .setTimestamp(java.time.Instant.now());

                    gameMessage.editMessageEmbeds(embed.build()).queue();
                    Thread.sleep(600);
                }

                
                stopReelsSequentially();
                
                
                Thread.sleep(1000);
                showFinalResult();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void stopReelsSequentially() {
        try {
            String reel1 = getRandomSymbol();
            String reel2 = getRandomSymbol();
            String reel3 = getRandomSymbol();

            
            EmbedBuilder embed1 = new EmbedBuilder()
                    .setTitle("🎰 Slot Machine")
                    .setDescription("**" + reel1 + " 🎰 🎰**\n\n🎯 First reel stopped!")
                    .addField("Your Bet", bet + " credits", true)
                    .setColor(Color.ORANGE)
                    .setTimestamp(java.time.Instant.now());

            gameMessage.editMessageEmbeds(embed1.build()).queue();
            Thread.sleep(1000);

            
            EmbedBuilder embed2 = new EmbedBuilder()
                    .setTitle("🎰 Slot Machine")
                    .setDescription("**" + reel1 + " " + reel2 + " 🎰**\n\n🎯 Second reel stopped!")
                    .addField("Your Bet", bet + " credits", true)
                    .setColor(Color.ORANGE)
                    .setTimestamp(java.time.Instant.now());

            gameMessage.editMessageEmbeds(embed2.build()).queue();
            Thread.sleep(1000);

            
            this.finalResult = new String[]{reel1, reel2, reel3};
            
            EmbedBuilder embed3 = new EmbedBuilder()
                    .setTitle("🎰 Slot Machine")
                    .setDescription("**" + reel1 + " " + reel2 + " " + reel3 + "**\n\n🎯 All reels stopped!")
                    .addField("Your Bet", bet + " credits", true)
                    .setColor(Color.BLUE)
                    .setTimestamp(java.time.Instant.now());

            gameMessage.editMessageEmbeds(embed3.build()).queue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String[] finalResult;

    private String getRandomSymbol() {
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }

        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (int i = 0; i < symbols.length; i++) {
            currentWeight += weights[i];
            if (randomValue < currentWeight) {
                return symbols[i];
            }
        }

        return symbols[0]; 
    }

    private void showFinalResult() {
        try {
            String slot1 = finalResult[0];
            String slot2 = finalResult[1];
            String slot3 = finalResult[2];

            
            int multiplier = 0;
            String winType = "";
            
            if (slot1.equals(slot2) && slot2.equals(slot3)) {
                
                switch (slot1) {
                    case "🍒": multiplier = 5; winType = "🍒 Cherry Triple!"; break;
                    case "🍊": multiplier = 8; winType = "🍊 Orange Triple!"; break;
                    case "🍋": multiplier = 10; winType = "🍋 Lemon Triple!"; break;
                    case "🍇": multiplier = 15; winType = "🍇 Grape Triple!"; break;
                    case "⭐": multiplier = 25; winType = "⭐ Star Triple!"; break;
                    case "💎": multiplier = 50; winType = "💎 Diamond Triple!"; break;
                    case "🔔": multiplier = 75; winType = "🔔 Bell Triple!"; break;
                    case "🎰": multiplier = 100; winType = "🎰 **MEGA JACKPOT!**"; break;
                }
            } else if (slot1.equals(slot2) || slot2.equals(slot3) || slot1.equals(slot3)) {
                
                multiplier = 2;
                winType = "Two of a kind!";
            } else if ((slot1.equals("🍒") || slot2.equals("🍒") || slot3.equals("🍒")) && 
                       (slot1.equals("🍋") || slot2.equals("🍋") || slot3.equals("🍋"))) {
                
                multiplier = 3;
                winType = "🍒🍋 Cherry-Lemon Combo!";
            }

            boolean won = multiplier > 0;
            long winAmount = won ? bet * multiplier : 0;
            long balance = databaseManager.getUserBalance(guildId, userId);
            long newBalance = balance - bet + winAmount;

            databaseManager.updateUserBalance(guildId, userId, newBalance);
            databaseManager.addTransaction(guildId, userId, "slots", -bet, null, "Slots bet");
            if (won) {
                databaseManager.addTransaction(guildId, userId, "slots_win", winAmount, null, "Slots win: " + winType);
            }

            
            long profit = winAmount - bet;
            String resultTitle = won ? (multiplier >= 50 ? "🎰 **MEGA JACKPOT!** 🎰" : "🎉 **WINNER!** 🎉") : "💸 **NO MATCH** 💸";
            String profitColor = profit > 0 ? "🟢" : "🔴";
            String profitSign = profit >= 0 ? "+" : "";
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🎰 **Slot Machine Results**")
                    .setDescription("╔══════════════╗\n" +
                                  "║  **" + slot1 + " " + slot2 + " " + slot3 + "**  ║\n" +
                                  "╚══════════════╝")
                    .addField("─────────────────", resultTitle, false);

            if (won) {
                embed.addField("🎊 **Win Type**", "🏆 " + winType, true);
                embed.addField("⚡ **Multiplier**", "`x" + multiplier + "`", true);
                embed.addField("💰 **Payout**", "`" + winAmount + " credits`", true);
                
                if (multiplier >= 75) {
                    embed.addField("🌟 **LEGENDARY WIN!**", "🔥 **ULTRA RARE!** 🔥\n💎 You hit the big one! 💎", false);
                } else if (multiplier >= 25) {
                    embed.addField("✨ **BIG WIN!**", "🎆 **FANTASTIC!** 🎆\nGreat luck!", false);
                }
            } else {
                embed.addField("🎯 **Result**", "No matching symbols", true);
                embed.addField("⚡ **Multiplier**", "`x0`", true);
                embed.addField("💸 **Loss**", "`-" + bet + " credits`", true);
            }
            
            embed.addField("💸 **Profit/Loss**", profitColor + " `" + profitSign + profit + " credits`", true);
            embed.addField("💳 **New Balance**", "`" + newBalance + " credits`", true);
            embed.addField("🎲 **Bet Amount**", "`" + bet + " credits`", true);
            
            Color embedColor = won ? (multiplier >= 50 ? Color.MAGENTA : Color.GREEN) : Color.RED;
            embed.setColor(embedColor)
                 .setFooter("The Syndicate's Den • Slots Complete")
                 .setTimestamp(java.time.Instant.now());

            Button playAgainButton = Button.success("slots_again", "🎰 Spin Again");
            gameMessage.editMessageEmbeds(embed.build())
                    .setComponents(ActionRow.of(playAgainButton))
                    .queue();

        } catch (SQLException e) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ Error")
                    .setDescription("Database error occurred!")
                    .setColor(Color.RED);
            
            gameMessage.editMessageEmbeds(errorEmbed.build()).setComponents().queue();
        }

        end();
        GameSessionManager.getInstance().endSession(guildId, userId);
    }

    @Override
    public void timeout() {
        if (gameMessage != null) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🎰 Slot Machine - Timed Out")
                    .setDescription("Game timed out. Your bet has been returned.")
                    .setColor(Color.ORANGE);
            
            gameMessage.editMessageEmbeds(embed.build()).setComponents().queue();
        }
        
        GameSessionManager.getInstance().endSession(guildId, userId);
    }
} 