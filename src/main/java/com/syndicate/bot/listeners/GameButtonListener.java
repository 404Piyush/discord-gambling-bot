package com.syndicate.bot.listeners;

import com.syndicate.bot.database.DatabaseManager;
import com.syndicate.bot.game.BlackjackSession;
import com.syndicate.bot.game.CoinFlipSession;
import com.syndicate.bot.game.DiceSession;
import com.syndicate.bot.game.GameSession;
import com.syndicate.bot.game.GameSessionManager;
import com.syndicate.bot.game.RouletteSession;
import com.syndicate.bot.game.RussianRouletteSession;
import com.syndicate.bot.game.SlotsSession;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.*;
import java.sql.SQLException;

public class GameButtonListener extends ListenerAdapter {
    private final DatabaseManager databaseManager;

    public GameButtonListener(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        String userId = event.getUser().getId();
        String guildId = event.getGuild().getId();

        
        if (buttonId.startsWith("main_")) {
            handleMainMenuButtons(event);
            return;
        }

        
        if (buttonId.startsWith("gamble_")) {
            handleGamblingMenuButtons(event);
            return;
        }

        if (buttonId.startsWith("economy_")) {
            handleEconomyMenuButtons(event);
            return;
        }

        if (buttonId.startsWith("info_")) {
            handleInfoMenuButtons(event);
            return;
        }

        if (buttonId.startsWith("manage_")) {
            handleManageMenuButtons(event);
            return;
        }

        
        if (buttonId.startsWith("bj_")) {
            GameSession session = GameSessionManager.getInstance().getSession(guildId, userId);
            if (session instanceof BlackjackSession) {
                ((BlackjackSession) session).handleButtonInteraction(event);
            } else {
                event.reply("❌ No active blackjack game found!").setEphemeral(true).queue();
            }
            return;
        }

        
        if (buttonId.startsWith("roulette_")) {
            GameSession session = GameSessionManager.getInstance().getSession(guildId, userId);
            if (session instanceof RouletteSession) {
                ((RouletteSession) session).handleButtonInteraction(event);
            } else {
                event.reply("❌ No active roulette game found!").setEphemeral(true).queue();
            }
            return;
        }

        
        if (buttonId.equals("slots_again")) {
            try {
                long amount = 100; 
                long balance = databaseManager.getUserBalance(guildId, userId);
                
                if (balance < amount) {
                    event.reply("❌ Insufficient funds! You need at least " + amount + " credits.")
                            .setEphemeral(true).queue();
                    return;
                }

                if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                    event.reply("❌ You already have an active game! Please finish it first.")
                            .setEphemeral(true).queue();
                    return;
                }

                event.reply("🎰 Starting new slots game with " + amount + " credits!")
                        .setEphemeral(true).queue();
                
                
            } catch (SQLException e) {
                event.reply("❌ Database error occurred!").setEphemeral(true).queue();
            }
            return;
        }

        
        if (buttonId.startsWith("dice_")) {
            GameSession session = GameSessionManager.getInstance().getSession(guildId, userId);
            if (session instanceof DiceSession) {
                ((DiceSession) session).handleButtonClick(event, databaseManager);
            } else {
                event.reply("❌ No active dice game found!").setEphemeral(true).queue();
            }
            return;
        }

        
        if (buttonId.startsWith("rr_")) {
            GameSession session = GameSessionManager.getInstance().getSession(guildId, userId);
            if (session instanceof RussianRouletteSession) {
                ((RussianRouletteSession) session).handleButtonInteraction(event);
            } else {
                event.reply("❌ No active Russian Roulette game found!").setEphemeral(true).queue();
            }
            return;
        }

        
        if (buttonId.startsWith("cf_")) {
            GameSession session = GameSessionManager.getInstance().getSession(guildId, userId);
            if (session instanceof CoinFlipSession) {
                ((CoinFlipSession) session).handleButtonInteraction(event);
            } else {
                event.reply("❌ No active Coin Flip game found!").setEphemeral(true).queue();
            }
            return;
        }

        
        if (buttonId.startsWith("roulette_bet_") || buttonId.startsWith("slots_bet_") || buttonId.startsWith("blackjack_bet_") || buttonId.startsWith("dice_bet_") || buttonId.startsWith("rr_bet_") || buttonId.startsWith("cf_bet_")) {
            handleGameBetButtons(event);
            return;
        }

        
        if (buttonId.startsWith("game_")) {
            event.reply("🎮 Interactive games are coming soon! Use the slash commands for now.")
                    .setEphemeral(true).queue();
            return;
        }

        
        event.reply("❌ Unknown button action! Please try again or contact support if this persists.")
                .setEphemeral(true).queue();
    }

    private void handleGamblingMenuButtons(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        String userId = event.getUser().getId();
        String guildId = event.getGuild().getId();

        try {
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            
            switch (buttonId) {
                case "gamble_dice":
                    showDiceBetMenu(event);
                    break;
                case "gamble_roulette":
                    showRouletteBetMenu(event);
                    break;
                case "gamble_slots":
                    showSlotsBetMenu(event);
                    break;
                case "gamble_blackjack":
                    showBlackjackBetMenu(event);
                    break;
                case "gamble_russian_roulette":
                    showRussianRouletteBetMenu(event);
                    break;
                case "gamble_coin_flip":
                    showCoinFlipBetMenu(event);
                    break;
                case "gamble_rules":
                    showGameRules(event);
                    break;
                case "gamble_stats":
                    showPlayerStats(event);
                    break;
                case "gamble_close":
                    event.getMessage().delete().queue();
                    event.reply("🚪 Casino closed. Thanks for visiting!").setEphemeral(true).queue();
                    break;
                case "back_to_gamble_menu":
                    showGamblingMenu(event);
                    break;
                case "back_to_main_menu":
                    showMainMenu(event);
                    break;
                default:
                    event.reply("❌ Unknown action!").setEphemeral(true).queue();
            }
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleEconomyMenuButtons(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        switch (buttonId) {
            case "back_to_economy_menu":
                showEconomyMenu(event);
                break;
            case "back_to_main_menu":
                showMainMenu(event);
                break;
            case "economy_balance":
                event.reply("💵 **Balance Check**\n\nUse `/economy balance` for detailed balance information!").setEphemeral(true).queue();
                break;
            case "economy_transfer":
                event.reply("💸 **Transfer Money**\n\nUse `/economy transfer @user amount` to send money!").setEphemeral(true).queue();
                break;
            case "economy_leaderboard":
                event.reply("🏆 **Leaderboard**\n\nUse `/economy leaderboard` to see the richest players!").setEphemeral(true).queue();
                break;
            default:
                event.reply("❌ Unknown economy action!").setEphemeral(true).queue();
        }
    }

    private void handleGameBetButtons(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        String userId = event.getUser().getId();
        String guildId = event.getGuild().getId();
        
        try {
            
            int betAmount = 0;
            if (buttonId.startsWith("roulette_bet_")) {
                betAmount = Integer.parseInt(buttonId.replace("roulette_bet_", ""));
                startRouletteWithBet(event, betAmount);
            } else if (buttonId.startsWith("slots_bet_")) {
                betAmount = Integer.parseInt(buttonId.replace("slots_bet_", ""));
                startSlotsWithBet(event, betAmount);
            } else if (buttonId.startsWith("blackjack_bet_")) {
                betAmount = Integer.parseInt(buttonId.replace("blackjack_bet_", ""));
                startBlackjackWithBet(event, betAmount);
            } else if (buttonId.startsWith("dice_bet_")) {
                betAmount = Integer.parseInt(buttonId.replace("dice_bet_", ""));
                startDiceWithBet(event, betAmount);
            } else if (buttonId.startsWith("rr_bet_")) {
                betAmount = Integer.parseInt(buttonId.replace("rr_bet_", ""));
                startRussianRouletteWithBet(event, betAmount);
            } else if (buttonId.startsWith("cf_bet_")) {
                betAmount = Integer.parseInt(buttonId.replace("cf_bet_", ""));
                startCoinFlipWithBet(event, betAmount);
            }
        } catch (NumberFormatException e) {
            event.reply("❌ Invalid bet amount!").setEphemeral(true).queue();
        }
    }

    private void showDiceBetMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎲 Dice Game - Place Your Bet")
                .setDescription("Choose your bet amount to start rolling dice!")
                .setColor(Color.BLUE);

        event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.primary("dice_bet_50", "$50"),
                        Button.primary("dice_bet_100", "$100"),
                        Button.primary("dice_bet_200", "$200"),
                        Button.primary("dice_bet_500", "$500")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_gamble_menu", "🔙 Casino Menu"),
                        Button.secondary("back_to_main_menu", "🏠 Main Menu"),
                        Button.secondary("gamble_close", "❌ Close")
                    )
                ).queue();
    }

    private void showRussianRouletteBetMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("💀 Russian Roulette - Place Your Bet")
                .setDescription("Choose your bet amount for the ultimate game of chance!\n\n⚠️ **Warning: High Risk Game!**")
                .setColor(Color.RED);

        event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.primary("rr_bet_100", "$100"),
                        Button.primary("rr_bet_250", "$250"),
                        Button.primary("rr_bet_500", "$500"),
                        Button.primary("rr_bet_1000", "$1000")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_gamble_menu", "🔙 Casino Menu"),
                        Button.secondary("back_to_main_menu", "🏠 Main Menu"),
                        Button.secondary("gamble_close", "❌ Close")
                    )
                ).queue();
    }

    private void showCoinFlipBetMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🪙 Coin Flip - Place Your Bet")
                .setDescription("Choose your bet amount for a simple heads or tails game!")
                .setColor(Color.YELLOW);

        event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.primary("cf_bet_25", "$25"),
                        Button.primary("cf_bet_50", "$50"),
                        Button.primary("cf_bet_100", "$100"),
                        Button.primary("cf_bet_200", "$200")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_gamble_menu", "🔙 Casino Menu"),
                        Button.secondary("back_to_main_menu", "🏠 Main Menu"),
                        Button.secondary("gamble_close", "❌ Close")
                    )
                ).queue();
    }

    private void startRussianRouletteWithBet(ButtonInteractionEvent event, int bet) {
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet))
                    .setEphemeral(true).queue();
                return;
            }
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo).setEphemeral(true).queue();
                return;
            }
            
            
            event.editMessage("💀 Starting Russian Roulette with $" + bet + " bet...")
                    .setComponents().queue();
            
            
            RussianRouletteSession session = new RussianRouletteSession(userId, guildId, bet, databaseManager, null);
            GameSessionManager.getInstance().startSession(session);
            
            event.getHook().editOriginal("💀 **Russian Roulette Started!**\n\n" +
                "The game will begin shortly. Prepare yourself...")
                .queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void startCoinFlipWithBet(ButtonInteractionEvent event, int bet) {
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet))
                    .setEphemeral(true).queue();
                return;
            }
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo).setEphemeral(true).queue();
                return;
            }
            
            
            event.editMessage("🪙 Starting Coin Flip with $" + bet + " bet...")
                    .setComponents().queue();
            
            
            CoinFlipSession session = new CoinFlipSession(userId, guildId, bet, databaseManager, null);
            GameSessionManager.getInstance().startSession(session);
            
            event.getHook().editOriginal("🪙 **Coin Flip Started!**\n\n" +
                "Choose heads or tails to win!")
                .queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void startRouletteWithBet(ButtonInteractionEvent event, int bet) {
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet))
                    .setEphemeral(true).queue();
                return;
            }
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo).setEphemeral(true).queue();
                return;
            }
            
            event.editMessage("🎰 Starting Roulette with $" + bet + " bet...")
                    .setComponents().queue();
            
            
            
            event.getHook().editOriginal("🎰 **Roulette Session Started!**\n\n" +
                "Use `/gamble roulette bet:" + bet + "` to continue with the interactive game!")
                .queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void startSlotsWithBet(ButtonInteractionEvent event, int bet) {
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet))
                    .setEphemeral(true).queue();
                return;
            }
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo).setEphemeral(true).queue();
                return;
            }
            
            event.editMessage("🎰 Starting Slots with $" + bet + " bet...")
                    .setComponents().queue();
            
            event.getHook().editOriginal("🎰 **Slots Session Started!**\n\n" +
                "Use `/gamble slots bet:" + bet + "` to continue with the interactive game!")
                .queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void startBlackjackWithBet(ButtonInteractionEvent event, int bet) {
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet))
                    .setEphemeral(true).queue();
                return;
            }
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo).setEphemeral(true).queue();
                return;
            }
            
            event.editMessage("♠️ Starting Blackjack with $" + bet + " bet...")
                    .setComponents().queue();
            
            event.getHook().editOriginal("♠️ **Blackjack Session Started!**\n\n" +
                "Use `/gamble blackjack bet:" + bet + "` to continue with the interactive game!")
                .queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void startDiceWithBet(ButtonInteractionEvent event, int bet) {
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet))
                    .setEphemeral(true).queue();
                return;
            }
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo).setEphemeral(true).queue();
                return;
            }
            
            event.editMessage("🎲 Starting Dice Game with $" + bet + " bet...")
                    .setComponents().queue();
            
            event.getHook().editOriginal("🎲 **Dice Game Session Started!**\n\n" +
                "Use `/gamble dice` to continue with the interactive game!")
                .queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void showRouletteBetMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎰 Roulette - Place Your Bet")
                .setDescription("Choose your bet amount to start playing roulette!")
                .setColor(Color.RED);

        event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.primary("roulette_bet_50", "$50"),
                        Button.primary("roulette_bet_100", "$100"),
                        Button.primary("roulette_bet_250", "$250"),
                        Button.primary("roulette_bet_500", "$500")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_gamble_menu", "🔙 Casino Menu"),
                        Button.secondary("back_to_main_menu", "🏠 Main Menu"),
                        Button.secondary("gamble_close", "❌ Close")
                    )
                ).queue();
    }

    private void showSlotsBetMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎰 Slots - Place Your Bet")
                .setDescription("Choose your bet amount to start spinning!")
                .setColor(Color.YELLOW);

        event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.primary("slots_bet_25", "$25"),
                        Button.primary("slots_bet_50", "$50"),
                        Button.primary("slots_bet_100", "$100"),
                        Button.primary("slots_bet_200", "$200")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_gamble_menu", "🔙 Casino Menu"),
                        Button.secondary("back_to_main_menu", "🏠 Main Menu"),
                        Button.danger("gamble_close", "❌ Close")
                    )
                ).queue();
    }

    private void showBlackjackBetMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("♠️ Blackjack - Place Your Bet")
                .setDescription("Choose your bet amount to start playing blackjack!")
                .setColor(Color.BLACK);

        event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.primary("blackjack_bet_100", "$100"),
                        Button.primary("blackjack_bet_250", "$250"),
                        Button.primary("blackjack_bet_500", "$500"),
                        Button.primary("blackjack_bet_1000", "$1000")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_gamble_menu", "🔙 Casino Menu"),
                        Button.secondary("back_to_main_menu", "🏠 Main Menu"),
                        Button.danger("gamble_close", "❌ Close")
                    )
                ).queue();
    }

    private void showGameRules(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Game Rules & Information")
                .setDescription("Learn how to play each game in our casino!")
                .addField("🎲 **Dice Game**", 
                    "• Roll two dice and predict the outcome\n" +
                    "• Bet on high (8-12), low (2-6), or exact sum\n" +
                    "• Snake Eyes (2) and Boxcars (12) have special payouts\n" +
                    "• Interactive betting with multiple options", false)
                .addField("🎰 **Roulette**", 
                    "• European roulette with 0-36 numbers\n" +
                    "• Bet on red/black, odd/even, or specific numbers\n" +
                    "• Red numbers: 1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36\n" +
                    "• Interactive betting wheel interface", false)
                .addField("🎰 **Slots**", 
                    "• 3-reel slot machine with various symbols\n" +
                    "• Match symbols for different payouts\n" +
                    "• Cherries: 2x, Lemons: 3x, Bells: 5x, Bars: 10x, Sevens: 20x\n" +
                    "• Auto-spin and manual control options", false)
                .addField("♠️ **Blackjack**", 
                    "• Classic 21 against the dealer\n" +
                    "• Hit, Stand, Double Down, Split available\n" +
                    "• Dealer hits on soft 17\n" +
                    "• Blackjack pays 3:2, regular wins pay 1:1", false)
                .addField("💀 **Russian Roulette**", 
                    "• 6-chamber revolver with 1 bullet\n" +
                    "• Pull trigger to advance chambers\n" +
                    "• Survive all 6 chambers to win 5x bet\n" +
                    "• Hit the bullet = Game Over", false)
                .addField("🪙 **Coin Flip**", 
                    "• Simple heads or tails game\n" +
                    "• 50/50 chance to win\n" +
                    "• Win 2x your bet\n" +
                    "• Quick and easy gameplay", false)
                .setColor(Color.BLUE)
                .setFooter("The Syndicate's Den • House always has an edge!");

        event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.secondary("back_to_gamble_menu", "🔙 Casino Menu"),
                        Button.secondary("back_to_main_menu", "🏠 Main Menu"),
                        Button.danger("gamble_close", "❌ Close")
                    )
                ).queue();
    }

    private void showPlayerStats(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        String guildId = event.getGuild().getId();
        
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📊 Your Casino Statistics")
                    .setDescription("Here's your gambling profile!")
                    .addField("💰 Current Balance", "$" + String.format("%,d", balance), true)
                    .addField("🎮 Active Games", 
                        GameSessionManager.getInstance().hasActiveSession(guildId, userId) ? 
                        "Yes" : "None", true)
                    .addField("🏆 Status", balance > 1000 ? "Winning" : "Losing", true)
                    .setColor(balance > 1000 ? Color.GREEN : Color.RED)
                    .setFooter("Keep playing responsibly!");

            event.editMessageEmbeds(embed.build())
                    .setComponents(
                        ActionRow.of(
                            Button.secondary("back_to_gamble_menu", "🔙 Casino Menu"),
                            Button.secondary("back_to_main_menu", "🏠 Main Menu"),
                            Button.danger("gamble_close", "❌ Close")
                        )
                    ).queue();
                    
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void showGamblingMenu(ButtonInteractionEvent event) {
        try {
            String guildId = event.getGuild().getId();
            String userId = event.getUser().getId();
            
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🎰 The Syndicate's Casino")
                    .setDescription("**🎯 Welcome to our premium gambling establishment!**\n\n" +
                        "Choose your game of choice from the selection below.")
                    .addField("💰 Your Balance", "$" + String.format("%,d", balance), true)
                    .addField("🎰 Casino Status", "Open 24/7", true)
                    .addField("🎯 House Edge", "Competitive", true)
                    .addField("🎲 **Dice Game**", "Roll and predict outcomes", false)
                    .addField("🎰 **Roulette**", "Classic wheel betting", false)
                    .addField("🎰 **Slots**", "3-reel slot machine", false)
                    .addField("♠️ **Blackjack**", "Beat the dealer to 21", false)
                    .addField("💀 **Russian Roulette**", "Ultimate game of chance", false)
                    .addField("🪙 **Coin Flip**", "Simple heads or tails", false)
                    .setColor(Color.ORANGE)
                    .setFooter("The Syndicate's Den • Premium Gaming Experience")
                    .setTimestamp(java.time.Instant.now());

            
            String activeSessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
            if (activeSessionInfo != null) {
                embed.addField("⚠️ Active Game", activeSessionInfo, false);
            }

            event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.primary("gamble_dice", "🎲 Dice Game"),
                        Button.primary("gamble_roulette", "🎰 Roulette"),
                        Button.primary("gamble_slots", "🎰 Slots")
                    ),
                    ActionRow.of(
                        Button.primary("gamble_blackjack", "♠️ Blackjack"),
                        Button.primary("gamble_russian_roulette", "💀 Russian Roulette"),
                        Button.primary("gamble_coin_flip", "🪙 Coin Flip")
                    ),
                    ActionRow.of(
                        Button.secondary("gamble_rules", "📋 Game Rules"),
                        Button.secondary("gamble_stats", "📊 Your Stats")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_main_menu", "🔙 Main Menu"),
                        Button.secondary("gamble_close", "❌ Close")
                    )
                ).queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleMainMenuButtons(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        switch (buttonId) {
            case "main_to_gamble":
                showGamblingMenu(event);
                break;
            case "main_to_economy":
                showEconomyMenu(event);
                break;
            case "main_to_info":
                showInfoMenu(event);
                break;
            case "main_to_manage":
                showManageMenu(event);
                break;
            case "main_refresh":
                showMainMenu(event);
                break;
            case "main_close":
                event.getMessage().delete().queue();
                event.reply("👋 Menu closed. Use `/menu` to open it again!").setEphemeral(true).queue();
                break;
            default:
                event.reply("❌ Unknown main menu action!").setEphemeral(true).queue();
        }
    }

    private void handleInfoMenuButtons(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        switch (buttonId) {
            case "info_help":
                event.reply("📚 **Help Guide**\n\nUse `/info help` for detailed getting started guide!").setEphemeral(true).queue();
                break;
            case "info_commands":
                event.reply("📋 **Commands**\n\nUse `/info commands` for complete command reference!").setEphemeral(true).queue();
                break;
            case "info_games":
                event.reply("🎮 **Game Rules**\n\nUse `/info games` to learn how to play each game!").setEphemeral(true).queue();
                break;
            case "info_stats":
                event.reply("📊 **Server Stats**\n\nUse `/info stats` to view server statistics!").setEphemeral(true).queue();
                break;
            case "back_to_info_menu":
                showInfoMenu(event);
                break;
            case "back_to_main_menu":
                showMainMenu(event);
                break;
            default:
                event.reply("❌ Unknown info action!").setEphemeral(true).queue();
        }
    }

    private void handleManageMenuButtons(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        switch (buttonId) {
            case "manage_committee":
                event.reply("👥 **Committee Management**\n\nUse `/manage committee` for admin user management!").setEphemeral(true).queue();
                break;
            case "manage_bank":
                event.reply("🏦 **Bank Status**\n\nUse `/manage bank` to check casino finances!").setEphemeral(true).queue();
                break;
            case "manage_settings":
                event.reply("⚙️ **Settings**\n\nUse `/manage settings` to configure bot settings!").setEphemeral(true).queue();
                break;
            case "back_to_manage_menu":
                showManageMenu(event);
                break;
            case "back_to_main_menu":
                showMainMenu(event);
                break;
            default:
                event.reply("❌ Unknown manage action!").setEphemeral(true).queue();
        }
    }

    private void showMainMenu(ButtonInteractionEvent event) {
        try {
            String guildId = event.getGuild().getId();
            String userId = event.getUser().getId();
            
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🏛️ The Syndicate's Den - Main Menu")
                    .setDescription("**Welcome to The Syndicate's Den!**\n\n" +
                        "Your premier destination for virtual gambling and entertainment. " +
                        "Choose from the options below to access different areas of our establishment.")
                    .addField("💰 Your Balance", "$" + String.format("%,d", balance), true)
                    .addField("🎰 Casino Status", "Open 24/7", true)
                    .addField("👥 Online Users", event.getGuild().getMemberCount() + " members", true)
                    .addField("🎮 **Casino Games**", "Play dice, roulette, slots, and blackjack", false)
                    .addField("💵 **Economy System**", "Manage your money and view leaderboards", false)
                    .addField("ℹ️ **Information Hub**", "Get help, rules, and bot information", false)
                    .addField("⚙️ **Management**", "Admin tools for committee members", false)
                    .setColor(new Color(138, 43, 226)) 
                    .setFooter("The Syndicate's Den • Your Gateway to Virtual Gaming")
                    .setTimestamp(java.time.Instant.now());

            event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.success("main_to_gamble", "🎰 Casino Games"),
                        Button.primary("main_to_economy", "💰 Economy"),
                        Button.secondary("main_to_info", "ℹ️ Information"),
                        Button.danger("main_to_manage", "⚙️ Management")
                    ),
                    ActionRow.of(
                        Button.secondary("main_refresh", "🔄 Refresh"),
                        Button.danger("main_close", "❌ Close Menu")
                    )
                ).queue();
                
        } catch (Exception e) {
            event.reply("❌ An error occurred while loading the main menu!").setEphemeral(true).queue();
        }
    }

    private void showEconomyMenu(ButtonInteractionEvent event) {
        try {
            String guildId = event.getGuild().getId();
            String userId = event.getUser().getId();
            
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("💰 Economy Dashboard")
                    .setDescription("**Manage your financial empire!**\n\n" +
                        "Keep track of your wealth and engage in the economy.")
                    .addField("💵 Your Balance", "$" + String.format("%,d", balance), true)
                    .addField("📈 Status", balance > 1000 ? "Profitable" : "Starting Out", true)
                    .addField("🎯 Daily Goal", "$10,000", true)
                    .addField("💳 **Check Balance**", "View your current funds", false)
                    .addField("💸 **Transfer Money**", "Send money to other players", false)
                    .addField("🏆 **Leaderboard**", "See the richest players", false)
                    .setColor(new Color(255, 215, 0)) 
                    .setFooter("The Syndicate's Den • Economy Hub")
                    .setTimestamp(java.time.Instant.now());

            event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.success("economy_balance", "💵 Check Balance"),
                        Button.primary("economy_transfer", "💸 Transfer Money"),
                        Button.secondary("economy_leaderboard", "🏆 Leaderboard")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_main_menu", "🔙 Main Menu"),
                        Button.danger("main_close", "❌ Close")
                    )
                ).queue();
                
        } catch (Exception e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void showInfoMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("ℹ️ Information Center")
                .setDescription("**Learn everything about The Syndicate's Den!**\n\n" +
                    "Get help, learn the rules, and discover all features.")
                .addField("📚 **Help Guide**", "How to use the bot", false)
                .addField("📋 **All Commands**", "Complete command list", false)
                .addField("🎮 **Game Rules**", "Learn how to play each game", false)
                .addField("📊 **Server Stats**", "View server statistics", false)
                .setColor(new Color(30, 144, 255)) 
                .setFooter("The Syndicate's Den • Information Hub")
                .setTimestamp(java.time.Instant.now());

        event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.primary("info_help", "📚 Help Guide"),
                        Button.secondary("info_commands", "📋 Commands"),
                        Button.success("info_games", "🎮 Game Rules"),
                        Button.primary("info_stats", "📊 Stats")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_main_menu", "🔙 Main Menu"),
                        Button.danger("main_close", "❌ Close")
                    )
                ).queue();
    }

    private void showManageMenu(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⚙️ Management Center")
                .setDescription("**Committee Administration Panel**\n\n" +
                    "Manage casino operations and system settings.")
                .addField("👥 **Committee Management**", "Add/remove admin users", false)
                .addField("🏦 **Bank Status**", "Monitor casino finances", false)
                .addField("⚙️ **System Settings**", "Configure bot settings", false)
                .setColor(new Color(220, 20, 60)) 
                .setFooter("The Syndicate's Den • Admin Panel")
                .setTimestamp(java.time.Instant.now());

        event.editMessageEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.danger("manage_committee", "👥 Committee"),
                        Button.primary("manage_bank", "🏦 Bank Status"),
                        Button.secondary("manage_settings", "⚙️ Settings")
                    ),
                    ActionRow.of(
                        Button.secondary("back_to_main_menu", "🔙 Main Menu"),
                        Button.danger("main_close", "❌ Close")
                    )
                ).queue();
    }
} 