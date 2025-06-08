package com.syndicate.bot.game;

import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RouletteSession extends GameSession {
    private final DatabaseManager databaseManager;
    private final SlashCommandInteractionEvent originalEvent;
    private final Random random = new Random();
    private Message gameMessage;
    private boolean isAmericanRoulette = true; 

    public RouletteSession(String userId, String guildId, long bet, DatabaseManager databaseManager, SlashCommandInteractionEvent originalEvent) {
        super(userId, guildId, bet);
        this.databaseManager = databaseManager;
        this.originalEvent = originalEvent;
    }

    public void startGame() {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎰 American Roulette Table")
                .setDescription("**🎯 Welcome to The Syndicate's Roulette Table!**\n\n" +
                    "Choose your betting strategy below. Click any number or betting option to place your wager!\n\n" +
                    "🟢 **Green**: 0, 00 (35x payout)\n" +
                    "🔴 **Red Numbers**: " + Arrays.toString(getRedNumbers()).replace("[", "").replace("]", "") + "\n" +
                    "⚫ **Black Numbers**: All other numbers\n\n" +
                    "💡 *Tip: Single numbers pay 35x, while color/odd/even bets pay 2x*")
                .addField("💰 Your Bet", "$" + bet, true)
                .addField("🎲 Payout Rates", 
                    "• **Single Number**: 35x\n" +
                    "• **Red/Black**: 2x\n" +
                    "• **Odd/Even**: 2x\n" +
                    "• **High/Low**: 2x\n" +
                    "• **Dozens**: 3x\n" +
                    "• **Columns**: 3x", true)
                .addField("🎰 Table Type", "American Roulette (0, 00)", true)
                .setColor(Color.ORANGE)
                .setTimestamp(java.time.Instant.now());

        List<ActionRow> rows = createRouletteInterface();

        originalEvent.replyEmbeds(embed.build())
                .addComponents(rows)
                .queue(response -> {
                    response.retrieveOriginal().queue(message -> {
                        this.gameMessage = message;
                    });
                });
    }

    private List<ActionRow> createRouletteInterface() {
        List<ActionRow> rows = new ArrayList<>();
        
        
        rows.add(ActionRow.of(
            Button.success("roulette_0", "🟢 0"),
            Button.success("roulette_00", "🟢 00"),
            Button.secondary("roulette_info", "📊 View Wheel"),
            Button.primary("roulette_help", "❓ How to Play"),
            Button.danger("roulette_cancel", "❌ Cancel")
        ));
        
        
        for (int rowStart = 1; rowStart <= 34; rowStart += 12) {
            List<Button> numberButtons = new ArrayList<>();
            for (int i = 0; i < 5 && (rowStart + i) <= 36; i++) {
                int number = rowStart + i;
                String buttonStyle = getButtonStyle(number);
                String emoji = getNumberEmoji(number);
                numberButtons.add(createStyledButton(buttonStyle, "roulette_" + number, emoji + " " + number));
            }
            
            while (numberButtons.size() < 5) {
                int number = rowStart + numberButtons.size();
                if (number <= 36) {
                    String buttonStyle = getButtonStyle(number);
                    String emoji = getNumberEmoji(number);
                    numberButtons.add(createStyledButton(buttonStyle, "roulette_" + number, emoji + " " + number));
                }
            }
            rows.add(ActionRow.of(numberButtons));
        }
        
        
        rows.add(ActionRow.of(
            Button.danger("roulette_red", "🔴 Red (2x)"),
            Button.secondary("roulette_black", "⚫ Black (2x)"),
            Button.primary("roulette_odd", "🎲 Odd (2x)"),
            Button.success("roulette_even", "⚡ Even (2x)"),
            Button.primary("roulette_high", "📈 High 19-36 (2x)")
        ));
        
        
        rows.add(ActionRow.of(
            Button.primary("roulette_low", "📉 Low 1-18 (2x)"),
            Button.secondary("roulette_1st12", "1st Dozen (1-12) 3x"),
            Button.secondary("roulette_2nd12", "2nd Dozen (13-24) 3x"),
            Button.secondary("roulette_3rd12", "3rd Dozen (25-36) 3x")
        ));

        return rows;
    }

    private String getButtonStyle(int number) {
        if (number == 0) return "success"; 
        return isRed(number) ? "danger" : "secondary"; 
    }

    private String getNumberEmoji(int number) {
        if (number == 0) return "🟢";
        return isRed(number) ? "🔴" : "⚫";
    }

    private Button createStyledButton(String style, String id, String label) {
        switch (style) {
            case "danger": return Button.danger(id, label);
            case "success": return Button.success(id, label);
            case "primary": return Button.primary(id, label);
            default: return Button.secondary(id, label);
        }
    }

    public void handleButtonInteraction(ButtonInteractionEvent event) {
        if (!isActive || !event.getUser().getId().equals(userId)) {
            if (!event.isAcknowledged()) {
                event.reply("❌ This is not your game!").setEphemeral(true).queue();
            }
            return;
        }

        String buttonId = event.getComponentId();
        
        
        if (buttonId.equals("roulette_info")) {
            showWheelInfo(event);
            return;
        } else if (buttonId.equals("roulette_help")) {
            showHelpInfo(event);
            return;
        } else if (buttonId.equals("roulette_cancel")) {
            cancelGame(event);
            return;
        }

        String betType = buttonId.substring("roulette_".length());
        
        
        int number = isAmericanRoulette ? random.nextInt(38) : random.nextInt(37); 
        if (isAmericanRoulette && number == 37) number = -1; 
        
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            boolean won = checkWin(number, betType);
            int multiplier = getMultiplier(betType);
            
            long winAmount = won ? bet * multiplier : 0;
            long newBalance = balance - bet + winAmount;
            
            databaseManager.updateUserBalance(guildId, userId, newBalance);
            databaseManager.addTransaction(guildId, userId, "roulette", -bet, null, "Roulette bet: " + betType);
            if (won) {
                databaseManager.addTransaction(guildId, userId, "roulette_win", winAmount, null, "Roulette win: " + betType);
            }
            
            
            showEnhancedSpinAnimation(event, number, betType, won, winAmount - bet, newBalance);
            
        } catch (SQLException e) {
            if (!event.isAcknowledged()) {
                event.reply("❌ Database error occurred!").setEphemeral(true).queue();
            }
        }
    }

    private void showWheelInfo(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎰 Roulette Wheel Layout")
                .setDescription("**American Roulette Wheel Numbers:**\n\n" +
                    "🟢 **Green**: 0, 00\n" +
                    "🔴 **Red**: " + Arrays.toString(getRedNumbers()).replace("[", "").replace("]", "") + "\n" +
                    "⚫ **Black**: " + Arrays.toString(getBlackNumbers()).replace("[", "").replace("]", "") + "\n\n" +
                    "🎯 **Betting Options:**\n" +
                    "• Single Number: 35:1 payout\n" +
                    "• Red/Black, Odd/Even, High/Low: 2:1 payout\n" +
                    "• Dozens (1-12, 13-24, 25-36): 3:1 payout")
                .setColor(Color.BLUE);
        
        event.reply("").addEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void showHelpInfo(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎰 How to Play Roulette")
                .setDescription("**🎯 Objective:** Predict where the ball will land!\n\n" +
                    "**🎲 Betting Options:**\n" +
                    "• **Single Numbers**: Click any number (0-36, 00) for 35x payout\n" +
                    "• **Colors**: Red or Black for 2x payout\n" +
                    "• **Odd/Even**: For 2x payout\n" +
                    "• **High/Low**: 1-18 (Low) or 19-36 (High) for 2x payout\n" +
                    "• **Dozens**: 1st (1-12), 2nd (13-24), or 3rd (25-36) for 3x payout\n\n" +
                    "**💡 Tips:**\n" +
                    "• Green numbers (0, 00) beat all other bets except direct number bets\n" +
                    "• Single number bets have the highest payout but lowest odds\n" +
                    "• Color/odd/even bets have better odds but lower payouts")
                .setColor(Color.GREEN);
        
        event.reply("").addEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void cancelGame(ButtonInteractionEvent event) {
        try {
            
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🎰 Roulette Game Cancelled")
                    .setDescription("Your bet has been returned to your balance.")
                    .addField("💰 Returned", "$" + bet, true)
                    .setColor(Color.ORANGE);
            
            event.deferEdit().queue();
            event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
            
        } catch (SQLException e) {
            event.reply("❌ Error cancelling game!").setEphemeral(true).queue();
        }
        
        end();
        GameSessionManager.getInstance().endSession(guildId, userId);
    }

    private void showEnhancedSpinAnimation(ButtonInteractionEvent event, int number, String betType, boolean won, long profit, long newBalance) {
        
        EmbedBuilder spinEmbed = new EmbedBuilder()
                .setTitle("🎰 American Roulette - Spinning!")
                .setDescription("🌪️ **THE WHEEL IS SPINNING...** 🌪️\n\n" +
                    createSpinningWheelArt(1) + "\n\n" +
                    "💫 *The ball is bouncing around the wheel...*\n" +
                    "🎯 *Gravity is pulling it down...*\n" +
                    "⚡ *Almost there...*")
                .addField("🎲 Your Bet", formatBetType(betType) + " - $" + bet, false)
                .addField("🎰 Wheel", "American Roulette (0, 00, 1-36)", true)
                .addField("💫 Status", "🌪️ **SPINNING**", true)
                .setColor(Color.YELLOW)
                .setTimestamp(java.time.Instant.now());

        if (!event.isAcknowledged()) {
            event.deferEdit().queue();
        }
        event.getHook().editOriginalEmbeds(spinEmbed.build()).setComponents().queue();

        
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                
                EmbedBuilder slowingEmbed = new EmbedBuilder()
                        .setTitle("🎰 American Roulette - Slowing Down!")
                        .setDescription("🎯 **THE BALL IS SLOWING DOWN...** 🎯\n\n" +
                            createSpinningWheelArt(2) + "\n\n" +
                            "🎲 *Click... click... click...*\n" +
                            "⏰ *The ball is finding its pocket...*")
                        .addField("🎲 Your Bet", formatBetType(betType) + " - $" + bet, false)
                        .addField("💫 Status", "⏰ **SLOWING DOWN**", true)
                        .setColor(Color.ORANGE);

                if (gameMessage != null) {
                    gameMessage.editMessageEmbeds(slowingEmbed.build()).setComponents().queue();
                }
                
                Thread.sleep(1000);
                
                
                String numberDisplay = number == -1 ? "00" : String.valueOf(number);
                String numberColor = getNumberColor(number);
                String ballEmoji = number == 0 || number == -1 ? "🟢" : (isRed(number) ? "🔴" : "⚫");
                
                EmbedBuilder resultEmbed = new EmbedBuilder()
                        .setTitle("🎰 Roulette Results - " + (won ? "YOU WIN!" : "House Wins"))
                        .setDescription("🎯 **THE BALL HAS LANDED!**\n\n" +
                            createResultWheelArt(number) + "\n\n" +
                            "# " + ballEmoji + " **" + numberDisplay + "** " + ballEmoji + "\n" +
                            "*(" + numberColor + " Number)*\n\n" +
                            (won ? "🎉 **CONGRATULATIONS!** 🎉" : "💔 **Better luck next time!**"))
                        .addField("🎲 Your Bet", formatBetType(betType), true)
                        .addField("💰 Bet Amount", "$" + bet, true)
                        .addField("🎯 Winning Number", ballEmoji + " " + numberDisplay + " (" + numberColor + ")", true)
                        .addField("🎰 Result", won ? "🎉 **WIN!**" : "💔 **LOST**", true)
                        .addField("💸 Profit/Loss", (profit >= 0 ? "+$" : "-$") + Math.abs(profit), true)
                        .addField("💳 New Balance", "$" + String.format("%,d", newBalance), true)
                        .addField("🎰 Multiplier", "x" + getMultiplier(betType), true)
                        .addField("🏠 House Edge", isAmericanRoulette ? "5.26%" : "2.70%", true)
                        .addField("🎲 Next Spin", "Ready when you are!", true)
                        .setColor(won ? Color.GREEN : Color.RED)
                        .setTimestamp(java.time.Instant.now());

                if (won && profit >= bet * 10) {
                    resultEmbed.setDescription("🎯 **THE BALL HAS LANDED!**\n\n" +
                        createResultWheelArt(number) + "\n\n" +
                        "# " + ballEmoji + " **" + numberDisplay + "** " + ballEmoji + "\n" +
                        "*(" + numberColor + " Number)*\n\n" +
                        "✨ **INCREDIBLE WIN!** ✨\n" +
                        "🎆 **BIG PAYOUT!** 🎆");
                }

                
                List<Button> actionButtons = new ArrayList<>();
                actionButtons.add(Button.success("roulette_again", "🎰 Spin Again"));
                actionButtons.add(Button.primary("roulette_stats", "📊 View Stats"));
                if (won) {
                    actionButtons.add(Button.secondary("roulette_cashout", "💰 Cash Out"));
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

    private String createSpinningWheelArt(int phase) {
        if (phase == 1) {
            return "```\n" +
                   "      🎯 ROULETTE WHEEL 🎯\n" +
                   "    ╭─────────────────────╮\n" +
                   "   ╱ 🔴⚫🔴⚫🟢⚫🔴⚫🔴 ╲\n" +
                   "  │ ⚫🔴⚫🔴⚫🔴⚫🔴⚫🔴 │\n" +
                   " │ 🔴⚫🔴⚫🔴⚫🔴⚫🔴⚫🔴 │\n" +
                   "  │ ⚫🔴⚫🔴🟢🔴⚫🔴⚫🔴 │\n" +
                   "   ╲ 🔴⚫🔴⚫🔴⚫🔴⚫🔴 ╱\n" +
                   "    ╰─────────────────────╯\n" +
                   "           ⚪ ← BALL\n" +
                   "```";
        } else {
            return "```\n" +
                   "      🎯 ROULETTE WHEEL 🎯\n" +
                   "    ╭─────────────────────╮\n" +
                   "   ╱ ⚫🔴⚫🔴⚫🔴⚫🔴⚫ ╲\n" +
                   "  │ 🔴⚫🔴⚫🟢⚫🔴⚫🔴⚫ │\n" +
                   " │ ⚫🔴⚫🔴⚫🔴⚫🔴⚫🔴⚫ │\n" +
                   "  │ 🔴⚫🔴⚫🔴⚫🔴⚫🔴⚫ │\n" +
                   "   ╲ ⚫🔴⚫🔴🟢🔴⚫🔴⚫ ╱\n" +
                   "    ╰─────────────────────╯\n" +
                   "         ⚪ ← Slowing...\n" +
                   "```";
        }
    }

    private String createResultWheelArt(int number) {
        String ballEmoji = number == 0 || number == -1 ? "🟢" : (isRed(number) ? "🔴" : "⚫");
        String numberDisplay = number == -1 ? "00" : String.valueOf(number);
        
        return "```\n" +
               "      🎯 ROULETTE WHEEL 🎯\n" +
               "    ╭─────────────────────╮\n" +
               "   ╱ 🔴⚫🔴⚫🟢⚫🔴⚫🔴 ╲\n" +
               "  │ ⚫🔴⚫🔴⚫🔴⚫🔴⚫🔴 │\n" +
               " │ 🔴⚫🔴⚫" + ballEmoji + "⚫🔴⚫🔴⚫🔴 │ ← " + numberDisplay + "\n" +
               "  │ ⚫🔴⚫🔴⚫🔴⚫🔴⚫🔴 │\n" +
               "   ╲ 🔴⚫🔴⚫🔴⚫🔴⚫🔴 ╱\n" +
               "    ╰─────────────────────╯\n" +
               "       🎯 FINAL RESULT!\n" +
               "```";
    }

    private String formatBetType(String betType) {
        switch (betType) {
            case "red": return "🔴 RED";
            case "black": return "⚫ BLACK";
            case "odd": return "🎲 ODD";
            case "even": return "⚡ EVEN";
            case "high": return "📈 HIGH (19-36)";
            case "low": return "📉 LOW (1-18)";
            case "1st12": return "1️⃣ FIRST DOZEN (1-12)";
            case "2nd12": return "2️⃣ SECOND DOZEN (13-24)";
            case "3rd12": return "3️⃣ THIRD DOZEN (25-36)";
            case "0": return "🟢 ZERO";
            case "00": return "🟢 DOUBLE ZERO";
            default: 
                try {
                    int num = Integer.parseInt(betType);
                    String emoji = num == 0 ? "🟢" : (isRed(num) ? "🔴" : "⚫");
                    return emoji + " NUMBER " + num;
                } catch (NumberFormatException e) {
                    return betType.toUpperCase();
                }
        }
    }

    private boolean checkWin(int number, String betType) {
        switch (betType) {
            case "red": return number > 0 && number != -1 && isRed(number);
            case "black": return number > 0 && number != -1 && !isRed(number);
            case "even": return number > 0 && number != -1 && number % 2 == 0;
            case "odd": return number > 0 && number != -1 && number % 2 == 1;
            case "high": return number >= 19 && number <= 36;
            case "low": return number >= 1 && number <= 18;
            case "1st12": return number >= 1 && number <= 12;
            case "2nd12": return number >= 13 && number <= 24;
            case "3rd12": return number >= 25 && number <= 36;
            case "0": return number == 0;
            case "00": return number == -1;
            default:
                try {
                    int targetNumber = Integer.parseInt(betType);
                    return number == targetNumber;
                } catch (NumberFormatException e) {
                    return false;
                }
        }
    }

    private int getMultiplier(String betType) {
        switch (betType) {
            case "red":
            case "black":
            case "even":
            case "odd":
            case "high":
            case "low":
                return 2;
            case "1st12":
            case "2nd12":
            case "3rd12":
                return 3;
            case "0":
            case "00":
                return 35;
            default:
                
                try {
                    Integer.parseInt(betType);
                    return 35;
                } catch (NumberFormatException e) {
                    return 1;
                }
        }
    }

    private boolean isRed(int number) {
        if (number <= 0) return false;
        
        int[] redNumbers = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};
        for (int red : redNumbers) {
            if (number == red) return true;
        }
        return false;
    }

    private int[] getRedNumbers() {
        return new int[]{1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};
    }

    private int[] getBlackNumbers() {
        return new int[]{2, 4, 6, 8, 10, 11, 13, 15, 17, 20, 22, 24, 26, 28, 29, 31, 33, 35};
    }

    private String getNumberColor(int number) {
        if (number == 0 || number == -1) return "Green";
        return isRed(number) ? "Red" : "Black";
    }

    @Override
    public void timeout() {
        if (gameMessage != null) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🎰 Roulette - Timed Out")
                    .setDescription("⏰ Game timed out. Your bet has been returned to your balance.")
                    .addField("💰 Returned", "$" + bet, true)
                    .setColor(Color.ORANGE)
                    .setTimestamp(java.time.Instant.now());
            
            gameMessage.editMessageEmbeds(embed.build()).setComponents().queue();
        }
        
        GameSessionManager.getInstance().endSession(guildId, userId);
    }
} 