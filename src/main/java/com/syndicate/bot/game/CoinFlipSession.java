package com.syndicate.bot.game;

import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.*;
import java.sql.SQLException;
import java.util.Random;

public class CoinFlipSession extends GameSession {
    private final DatabaseManager databaseManager;
    private final SlashCommandInteractionEvent originalEvent;
    private final Random random = new Random();
    private final int bet;
    private boolean gameActive;

    public CoinFlipSession(String userId, String guildId, int bet, DatabaseManager databaseManager, SlashCommandInteractionEvent originalEvent) {
        super(userId, guildId, bet);
        this.bet = bet;
        this.databaseManager = databaseManager;
        this.originalEvent = originalEvent;
        this.gameActive = true;
    }

    public void startGame() {
        try {
            
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                originalEvent.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet))
                    .setEphemeral(true).queue();
                endGame();
                return;
            }
            
            
            databaseManager.updateUserBalance(guildId, userId, balance - bet);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🪙 Coin Flip")
                    .setDescription("**Place your bet and flip the coin!**\n\n" +
                        "🪙 **Choose your side:**\n" +
                        "• Heads - Win 2x your bet\n" +
                        "• Tails - Win 2x your bet\n" +
                        "• 50/50 chance to win!\n\n" +
                        "💰 **Your Bet:** $" + String.format("%,d", bet) + "\n" +
                        "🏆 **Potential Win:** $" + String.format("%,d", bet * 2))
                    .setColor(Color.YELLOW)
                    .setFooter("Coin Flip • Choose your side")
                    .setTimestamp(java.time.Instant.now());

            originalEvent.replyEmbeds(embed.build())
                    .addComponents(
                        ActionRow.of(
                            Button.primary("cf_heads", "🪙 Heads"),
                            Button.primary("cf_tails", "🪙 Tails")
                        ),
                        ActionRow.of(
                            Button.secondary("cf_cancel", "❌ Cancel")
                        )
                    ).queue();
                    
        } catch (SQLException e) {
            originalEvent.reply("❌ Database error occurred!").setEphemeral(true).queue();
            endGame();
        }
    }

    public void handleButtonInteraction(ButtonInteractionEvent event) {
        if (!gameActive) {
            event.reply("❌ This game has already ended!").setEphemeral(true).queue();
            return;
        }

        String buttonId = event.getComponentId();
        
        switch (buttonId) {
            case "cf_heads":
                flipCoin(event, true);
                break;
            case "cf_tails":
                flipCoin(event, false);
                break;
            case "cf_cancel":
                cancelGame(event);
                break;
            default:
                event.reply("❌ Unknown action!").setEphemeral(true).queue();
        }
    }

    private void flipCoin(ButtonInteractionEvent event, boolean playerChoseHeads) {
        gameActive = false;
        
        
        boolean coinIsHeads = random.nextBoolean();
        boolean playerWon = (coinIsHeads == playerChoseHeads);
        
        String coinResult = coinIsHeads ? "Heads" : "Tails";
        String playerChoice = playerChoseHeads ? "Heads" : "Tails";
        
        try {
            long currentBalance = databaseManager.getUserBalance(guildId, userId);
            
            if (playerWon) {
                
                long winAmount = bet * 2;
                databaseManager.updateUserBalance(guildId, userId, currentBalance + winAmount);
                
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("🏆 Coin Flip - YOU WIN!")
                        .setDescription("**🎉 Congratulations! 🎉**\n\n" +
                            "🪙 **The coin landed on:** " + coinResult + "\n" +
                            "🎯 **You chose:** " + playerChoice + "\n" +
                            "✅ **Perfect match!**\n\n" +
                            "💰 **Winnings:** $" + String.format("%,d", winAmount) + "\n" +
                            "📈 **Profit:** $" + String.format("%,d", winAmount - bet) + "\n" +
                            "💳 **New Balance:** $" + String.format("%,d", currentBalance + winAmount))
                        .setColor(Color.GREEN)
                        .setFooter("Coin Flip • Victory!")
                        .setTimestamp(java.time.Instant.now());

                event.editMessageEmbeds(embed.build()).setComponents().queue();
            } else {
                
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("💔 Coin Flip - You Lost")
                        .setDescription("**😔 Better luck next time! 😔**\n\n" +
                            "🪙 **The coin landed on:** " + coinResult + "\n" +
                            "🎯 **You chose:** " + playerChoice + "\n" +
                            "❌ **Wrong side!**\n\n" +
                            "💸 **Lost:** $" + String.format("%,d", bet) + "\n" +
                            "💳 **New Balance:** $" + String.format("%,d", currentBalance))
                        .setColor(Color.RED)
                        .setFooter("Coin Flip • Try Again!")
                        .setTimestamp(java.time.Instant.now());

                event.editMessageEmbeds(embed.build()).setComponents().queue();
            }
            
        } catch (SQLException e) {
            event.reply("❌ Error processing game result!").setEphemeral(true).queue();
        }
        
        endGame();
    }

    private void cancelGame(ButtonInteractionEvent event) {
        gameActive = false;
        
        try {
            
            long currentBalance = databaseManager.getUserBalance(guildId, userId);
            databaseManager.updateUserBalance(guildId, userId, currentBalance + bet);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🪙 Coin Flip - Cancelled")
                    .setDescription("**Game cancelled!**\n\n" +
                        "💰 Your bet of $" + String.format("%,d", bet) + " has been refunded.\n" +
                        "💳 **Balance:** $" + String.format("%,d", currentBalance + bet))
                    .setColor(Color.GRAY)
                    .setFooter("Coin Flip • Cancelled")
                    .setTimestamp(java.time.Instant.now());

            event.editMessageEmbeds(embed.build()).setComponents().queue();
            
        } catch (SQLException e) {
            event.reply("❌ Error processing cancellation!").setEphemeral(true).queue();
        }
        
        endGame();
    }

    @Override
    public void timeout() {
        gameActive = false;
        
        try {
            long currentBalance = databaseManager.getUserBalance(guildId, userId);
            databaseManager.updateUserBalance(guildId, userId, currentBalance + bet);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🪙 Coin Flip - Timeout")
                    .setDescription("**⏰ Game Timed Out!**\n\n" +
                        "You took too long to choose.\n" +
                        "💰 Your bet of $" + String.format("%,d", bet) + " has been refunded.")
                    .setColor(Color.GRAY)
                    .setFooter("Coin Flip • Timeout");
                    
            originalEvent.getHook().editOriginalEmbeds(embed.build())
                    .setComponents().queue();
        } catch (Exception e) {
            
        }
        end();
    }
    
    public void endGame() {
        gameActive = false;
        GameSessionManager.getInstance().endSession(guildId, userId);
    }

    public String getGameInfo() {
        return "Coin Flip - Bet: $" + bet;
    }
} 