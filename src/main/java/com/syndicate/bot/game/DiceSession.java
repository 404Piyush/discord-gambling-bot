package com.syndicate.bot.game;

import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiceSession extends GameSession {
    private final DatabaseManager databaseManager;
    private final SlashCommandInteractionEvent originalEvent;
    private final Random random = new Random();
    private Message gameMessage;
    
    public DiceSession(String userId, String guildId, DatabaseManager databaseManager, SlashCommandInteractionEvent originalEvent) {
        super(userId, guildId, 0); 
        this.databaseManager = databaseManager;
        this.originalEvent = originalEvent;
    }
    
    public void startGame() {
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎲 The Syndicate's Dice Table 🎲")
                .setColor(Color.ORANGE)
                .setDescription("**🎯 Welcome to our premium dice experience!**\n\n" +
                    "Roll two dice and bet on the outcome. Choose your betting strategy:\n\n" +
                    "🔴 **High (8-12)** - Pays 2x your bet\n" +
                    "🔵 **Low (2-7)** - Pays 2x your bet\n" +
                    "⚫ **Lucky 7** - Pays 5x your bet\n" +
                    "🐍 **Snake Eyes (Double 1s = 2)** - Pays 30x your bet\n" +
                    "📦 **Boxcars (Double 6s = 12)** - Pays 30x your bet\n\n" +
                    "💡 **Snake Eyes**: When both dice show 1 (total = 2)\n" +
                    "💡 **Boxcars**: When both dice show 6 (total = 12)")
                .addField("💰 Your Balance", "$" + String.format("%,d", balance), true)
                .addField("🎲 Dice Rules", "Two standard 6-sided dice", true)
                .addField("🎯 House Edge", "Competitive rates", true)
                .setFooter("The Syndicate's Den • Premium Dice Gaming")
                .setTimestamp(java.time.Instant.now());
            
            
            List<ActionRow> rows = new ArrayList<>();
            
            
            rows.add(ActionRow.of(
                Button.danger("dice_bet_high_25", "🔴 High $25"),
                Button.primary("dice_bet_low_25", "🔵 Low $25"),
                Button.secondary("dice_bet_seven_50", "⚫ Lucky 7 $50"),
                Button.success("dice_custom_bet", "💰 Custom Amount")
            ));
            
            
            rows.add(ActionRow.of(
                Button.danger("dice_bet_high_100", "🔴 High $100"),
                Button.primary("dice_bet_low_100", "🔵 Low $100"),
                Button.secondary("dice_bet_seven_200", "⚫ Lucky 7 $200"),
                Button.primary("dice_rules", "📋 Detailed Rules")
            ));
            
            
            rows.add(ActionRow.of(
                Button.success("dice_bet_snake_100", "🐍 Snake Eyes $100"),
                Button.success("dice_bet_boxcars_100", "📦 Boxcars $100"),
                Button.secondary("dice_stats", "📊 Your Stats"),
                Button.danger("dice_close", "❌ Close Table")
            ));

            originalEvent.replyEmbeds(embed.build())
                .addComponents(rows)
                .queue(response -> {
                    response.retrieveOriginal().queue(message -> {
                        this.gameMessage = message;
                    });
                });
                
        } catch (SQLException e) {
            originalEvent.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }
    
    public void handleButtonClick(ButtonInteractionEvent event, DatabaseManager dbManager) {
        if (!isActive || !event.getUser().getId().equals(userId)) {
            if (!event.isAcknowledged()) {
                event.reply("❌ This is not your game!").setEphemeral(true).queue();
            }
            return;
        }
        
        String buttonId = event.getComponentId();
        
        
        if (buttonId.equals("dice_custom_bet")) {
            showCustomBetModal(event);
            return;
        } else if (buttonId.equals("dice_rules")) {
            showDetailedRules(event);
            return;
        } else if (buttonId.equals("dice_stats")) {
            showPlayerStats(event);
            return;
        } else if (buttonId.equals("dice_close")) {
            closeGame(event);
            return;
        }
        
        
        String[] parts = buttonId.split("_");
        if (parts.length < 4) return;
        
        String betType = parts[2]; 
        int betAmount = Integer.parseInt(parts[3]);
        
        processBet(event, betType, betAmount);
    }
    
    private void showCustomBetModal(ButtonInteractionEvent event) {
        TextInput betAmountInput = TextInput.create("bet_amount", "Bet Amount", TextInputStyle.SHORT)
                .setPlaceholder("Enter amount (e.g., 500)")
                .setMinLength(1)
                .setMaxLength(10)
                .setRequired(true)
                .build();
                
        TextInput betTypeInput = TextInput.create("bet_type", "Bet Type", TextInputStyle.SHORT)
                .setPlaceholder("high, low, seven, snake, or boxcars")
                .setMinLength(3)
                .setMaxLength(10)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("dice_custom_modal", "🎲 Custom Dice Bet")
                .addActionRows(ActionRow.of(betAmountInput), ActionRow.of(betTypeInput))
                .build();

        event.replyModal(modal).queue();
    }
    
    private void showDetailedRules(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎲 Dice Game Rules & Payouts")
                .setDescription("**🎯 How to Play:**\n" +
                    "Two standard 6-sided dice are rolled. Bet on the total outcome!\n\n" +
                    "**📊 Betting Options & Payouts:**")
                .addField("🔴 High (8-12)", 
                    "• Total: 8, 9, 10, 11, or 12\n" +
                    "• Payout: 2x your bet\n" +
                    "• Probability: ~58%", true)
                .addField("🔵 Low (2-7)", 
                    "• Total: 2, 3, 4, 5, 6, or 7\n" +
                    "• Payout: 2x your bet\n" +
                    "• Probability: ~58%", true)
                .addField("⚫ Lucky 7", 
                    "• Total: Exactly 7\n" +
                    "• Payout: 5x your bet\n" +
                    "• Probability: ~17%", true)
                .addField("🐍 Snake Eyes", 
                    "• Both dice show 1 (total = 2)\n" +
                    "• Payout: 30x your bet\n" +
                    "• Probability: ~3%", true)
                .addField("📦 Boxcars", 
                    "• Both dice show 6 (total = 12)\n" +
                    "• Payout: 30x your bet\n" +
                    "• Probability: ~3%", true)
                .addField("💡 Pro Tips", 
                    "• Snake Eyes = Two 1s (looks like snake eyes)\n" +
                    "• Boxcars = Two 6s (looks like train boxcars)\n" +
                    "• Higher payouts = lower probability", true)
                .setColor(Color.BLUE)
                .setFooter("The Syndicate's Den • Fair Gaming");
        
        event.reply("").addEmbeds(embed.build()).setEphemeral(true).queue();
    }
    
    private void showPlayerStats(ButtonInteractionEvent event) {
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Your Dice Statistics")
                .setDescription("**🎲 Coming Soon!**\n\n" +
                    "Player statistics tracking will be available in a future update.\n\n" +
                    "This will include:\n" +
                    "• Total games played\n" +
                    "• Win/loss ratio\n" +
                    "• Biggest wins\n" +
                    "• Favorite bet types")
                .setColor(Color.CYAN);
        
        event.reply("").addEmbeds(embed.build()).setEphemeral(true).queue();
    }
    
    private void closeGame(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎲 Dice Table Closed")
                .setDescription("Thanks for playing at The Syndicate's Dice Table!\n\n" +
                    "🎯 Come back anytime for more dice action!")
                .setColor(Color.ORANGE);
        
        event.deferEdit().queue();
        event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
        
        end();
        GameSessionManager.getInstance().endSession(guildId, userId);
    }
    
    private void processBet(ButtonInteractionEvent event, String betType, int betAmount) {
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < betAmount) {
                if (!event.isAcknowledged()) {
                    event.reply("❌ **Insufficient Funds!**\n\n" +
                        "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                        "🎯 **Required**: $" + String.format("%,d", betAmount) + "\n" +
                        "💸 **Shortage**: $" + String.format("%,d", betAmount - balance))
                        .setEphemeral(true).queue();
                }
                return;
            }
            
            
            int die1 = random.nextInt(6) + 1;
            int die2 = random.nextInt(6) + 1;
            int total = die1 + die2;
            
            
            boolean won = checkWin(total, betType);
            int multiplier = getMultiplier(betType);
            long winAmount = won ? betAmount * multiplier : 0;
            long newBalance = balance - betAmount + winAmount;
            
            
            databaseManager.updateUserBalance(guildId, userId, newBalance);
            databaseManager.addTransaction(guildId, userId, "dice_" + betType, -betAmount, null, "Dice bet: " + betType);
            if (won) {
                databaseManager.addTransaction(guildId, userId, "dice_win", winAmount, null, "Dice win: " + betType);
            }
            
            
            showImprovedRollAnimation(event, die1, die2, total, betType, betAmount, won, winAmount - betAmount, newBalance);
            
        } catch (SQLException e) {
            if (!event.isAcknowledged()) {
                event.reply("❌ Database error occurred!").setEphemeral(true).queue();
            }
        }
    }
    
    private void showImprovedRollAnimation(ButtonInteractionEvent event, int die1, int die2, int total, String betType, int betAmount, boolean won, long profit, long newBalance) {
        
        EmbedBuilder rollEmbed = new EmbedBuilder()
            .setTitle("🎲 Rolling The Dice... 🎲")
            .setDescription("🌪️ **The dice are tumbling across the table...** 🌪️\n\n" +
                "```\n" +
                "    🎲     🎲\n" +
                "  ╔═══╗  ╔═══╗\n" +
                "  ║ ? ║  ║ ? ║\n" +
                "  ╚═══╝  ╚═══╝\n" +
                "    Rolling...\n" +
                "```\n" +
                "🎯 *The dice are spinning through the air...*\n" +
                "⚡ *Gravity is pulling them down...*\n" +
                "🎲 *Almost there...*")
            .addField("🎲 Your Bet", getBetDescription(betType), true)
            .addField("💰 Bet Amount", "$" + betAmount, true)
            .addField("🎯 Potential Win", "$" + (betAmount * getMultiplier(betType)), true)
            .setColor(Color.YELLOW)
            .setTimestamp(java.time.Instant.now());

        if (!event.isAcknowledged()) {
            event.deferEdit().queue();
        }
        event.getHook().editOriginalEmbeds(rollEmbed.build()).setComponents().queue();

        
        new Thread(() -> {
            try {
                Thread.sleep(2000); 
                
                String diceVisual = getEnhancedDiceVisual(die1, die2);
                String specialMessage = getSpecialMessage(die1, die2, total, betType);
                
                EmbedBuilder resultEmbed = new EmbedBuilder()
                    .setTitle("🎲 Dice Results - " + (won ? "YOU WIN!" : "House Wins"))
                    .setDescription("🎯 **The dice have landed!**\n\n" + 
                        diceVisual + "\n\n" +
                        "**🎯 Final Result:** " + die1 + " + " + die2 + " = **" + total + "**\n\n" +
                        specialMessage)
                    .addField("🎲 Your Bet", getBetDescription(betType), true)
                    .addField("💰 Bet Amount", "$" + betAmount, true)
                    .addField("🎯 Dice Total", String.valueOf(total), true)
                    .addField("🎰 Result", won ? "🎉 **WIN!**" : "💔 **LOST**", true)
                    .addField("💸 Profit/Loss", (profit >= 0 ? "+$" : "-$") + Math.abs(profit), true)
                    .addField("💳 New Balance", "$" + String.format("%,d", newBalance), true)
                    .addField("🎰 Multiplier", "x" + getMultiplier(betType), true)
                    .addField("🎲 Next Roll", "Ready when you are!", true)
                    .setColor(won ? Color.GREEN : Color.RED)
                    .setTimestamp(java.time.Instant.now());

                if (won && profit >= betAmount * 10) {
                    resultEmbed.setDescription("🎯 **The dice have landed!**\n\n" + 
                        diceVisual + "\n\n" +
                        "**🎯 Final Result:** " + die1 + " + " + die2 + " = **" + total + "**\n\n" +
                        "✨ **INCREDIBLE WIN!** ✨\n" +
                        "🎆 **MASSIVE PAYOUT!** 🎆\n\n" +
                        specialMessage);
                }

                
                List<Button> actionButtons = new ArrayList<>();
                actionButtons.add(Button.success("dice_again", "🎲 Roll Again"));
                actionButtons.add(Button.primary("dice_stats", "📊 View Stats"));
                if (won) {
                    actionButtons.add(Button.secondary("dice_cashout", "💰 Cash Out"));
                }
                
                if (gameMessage != null) {
                    gameMessage.editMessageEmbeds(resultEmbed.build())
                        .setComponents(ActionRow.of(actionButtons))
                        .queue();
                }
                    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        end();
        GameSessionManager.getInstance().endSession(guildId, userId);
    }
    
    private boolean checkWin(int total, String betType) {
        switch (betType) {
            case "high": return total >= 8 && total <= 12;
            case "low": return total >= 2 && total <= 7;
            case "seven": return total == 7;
            case "snake": return total == 2;
            case "boxcars": return total == 12;
            default: return false;
        }
    }
    
    private int getMultiplier(String betType) {
        switch (betType) {
            case "high":
            case "low":
                return 2;
            case "seven":
                return 5;
            case "snake":
            case "boxcars":
                return 30;
            default:
                return 1;
        }
    }
    
    private String getBetDescription(String betType) {
        switch (betType) {
            case "high": return "🔴 HIGH (8-12)";
            case "low": return "🔵 LOW (2-7)";
            case "seven": return "⚫ LUCKY 7";
            case "snake": return "🐍 SNAKE EYES (2)";
            case "boxcars": return "📦 BOXCARS (12)";
            default: return betType.toUpperCase();
        }
    }
    
    private String getEnhancedDiceVisual(int die1, int die2) {
        return "```\n" +
               "    🎲 FINAL RESULT 🎲\n" +
               "  ╔═══╗      ╔═══╗\n" +
               "  ║ " + die1 + " ║  +   ║ " + die2 + " ║\n" +
               "  ╚═══╝      ╚═══╝\n" +
               "     " + getDiceEmoji(die1) + "   +    " + getDiceEmoji(die2) + "\n" +
               "```";
    }
    
    private String getDiceEmoji(int value) {
        switch (value) {
            case 1: return "⚀";
            case 2: return "⚁";
            case 3: return "⚂";
            case 4: return "⚃";
            case 5: return "⚄";
            case 6: return "⚅";
            default: return "🎲";
        }
    }
    
    private String getSpecialMessage(int die1, int die2, int total, String betType) {
        if (die1 == 1 && die2 == 1) {
            return "🐍 **SNAKE EYES!** Both dice show 1! " + (betType.equals("snake") ? "🎉" : "");
        } else if (die1 == 6 && die2 == 6) {
            return "📦 **BOXCARS!** Both dice show 6! " + (betType.equals("boxcars") ? "🎉" : "");
        } else if (total == 7) {
            return "⚫ **Lucky 7!** The most common roll! " + (betType.equals("seven") ? "🎉" : "");
        } else if (total >= 8) {
            return "🔴 **High roll!** " + (betType.equals("high") ? "🎉" : "");
        } else {
            return "🔵 **Low roll!** " + (betType.equals("low") ? "🎉" : "");
        }
    }
    
    @Override
    public void timeout() {
        if (gameMessage != null) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎲 Dice Game - Timed Out")
                .setDescription("⏰ Game session timed out. No bets were placed.")
                .setColor(Color.ORANGE)
                .setTimestamp(java.time.Instant.now());
            
            gameMessage.editMessageEmbeds(embed.build()).setComponents().queue();
        }
        
        GameSessionManager.getInstance().endSession(guildId, userId);
    }
} 