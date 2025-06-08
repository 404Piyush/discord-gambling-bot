package com.syndicate.bot.commands.gambling;

import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.commands.SlashCommand;
import com.syndicate.bot.database.DatabaseManager;
import com.syndicate.bot.game.BlackjackSession;
import com.syndicate.bot.game.DiceSession;
import com.syndicate.bot.game.GameSessionManager;
import com.syndicate.bot.game.RouletteSession;
import com.syndicate.bot.game.SlotsSession;
import com.syndicate.bot.game.RussianRouletteSession;
import com.syndicate.bot.game.CoinFlipSession;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.*;
import java.sql.SQLException;
import java.util.Random;

public class GambleSlashCommand implements SlashCommand {
    private final DatabaseManager databaseManager;
    private final Random random = new Random();

    public GambleSlashCommand(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        
        if (subcommand == null) {
            
            showGamblingMenu(event);
            return;
        }
        
        switch (subcommand) {
            case "dice":
                handleDice(event);
                break;
            case "roulette":
                handleRoulette(event);
                break;
            case "slots":
                handleSlots(event);
                break;
            case "blackjack":
                handleBlackjack(event);
                break;
            case "russian-roulette":
                handleRussianRoulette(event);
                break;
            case "coin-flip":
                handleCoinFlip(event);
                break;
            case "menu":
                showGamblingMenu(event);
                break;
            default:
                event.reply("❌ Unknown gambling command!").setEphemeral(true).queue();
        }
    }

    private void showGamblingMenu(SlashCommandInteractionEvent event) {
        try {
            String guildId = event.getGuild().getId();
            String userId = event.getUser().getId();
            
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            long balance = databaseManager.getUserBalance(guildId, userId);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🎰 The Syndicate's Casino")
                    .setDescription("**🎯 Welcome to our premium gambling establishment!**\n\n" +
                        "Choose your game of choice from the selection below. Each game offers unique " +
                        "ways to win big or lose it all!\n\n" +
                        "🎮 **Select a game to get started:**")
                    .addField("💰 Your Balance", "$" + String.format("%,d", balance), true)
                    .addField("🎰 Casino Status", "Open 24/7", true)
                    .addField("🎯 House Edge", "Competitive rates", true)
                    .addField("🎲 **Dice Game**", 
                        "Roll two dice and bet on the outcome!\n" +
                        "• Multiple betting options\n" +
                        "• Custom bet amounts\n" +
                        "• Boxcars & Snake Eyes specials", false)
                    .addField("🎰 **Roulette**", 
                        "American roulette with all numbers!\n" +
                        "• 0, 00, and 1-36 available\n" +
                        "• Color, odd/even, dozens betting\n" +
                        "• Authentic casino experience", false)
                    .addField("🎰 **Slots**", 
                        "Classic 3-reel slot machine!\n" +
                        "• Multiple symbol combinations\n" +
                        "• Progressive payouts\n" +
                        "• Big jackpot potential", false)
                    .addField("♠️ **Blackjack**", 
                        "Classic 21 with dealer!\n" +
                        "• Hit, stand, double down, split\n" +
                        "• Auto-win on blackjack\n" +
                        "• Professional card dealing", false)
                    .addField("🔫 **Russian Roulette**", 
                        "Single-player deadly game!\n" +
                        "• 6-chamber revolver with 1 bullet\n" +
                        "• Interactive pull trigger or quit\n" +
                        "• 5x payout if you survive!", false)
                    .addField("🪙 **Coin Flip**", 
                        "Simple heads or tails!\n" +
                        "• 50/50 odds\n" +
                        "• Double your money\n" +
                        "• Quick and easy gameplay", false)
                    .setColor(Color.ORANGE)
                    .setFooter("The Syndicate's Den • Premium Gaming Experience")
                    .setTimestamp(java.time.Instant.now());

            
            String activeSessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
            if (activeSessionInfo != null) {
                embed.addField("⚠️ Active Game", activeSessionInfo, false);
            }

            event.replyEmbeds(embed.build())
                .addComponents(
                    ActionRow.of(
                        Button.primary("gamble_dice", "🎲 Dice Game"),
                        Button.danger("gamble_roulette", "🎰 Roulette"),
                        Button.success("gamble_slots", "🎰 Slots"),
                        Button.secondary("gamble_blackjack", "♠️ Blackjack")
                    ),
                    ActionRow.of(
                        Button.primary("gamble_rules", "📋 Game Rules"),
                        Button.secondary("gamble_stats", "📊 Your Stats"),
                        Button.danger("gamble_close", "❌ Close Menu")
                    )
                ).queue();
                
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleDice(SlashCommandInteractionEvent event) {
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo + 
                    "\n\n🔄 **Use `/gamble dice` again to resume your game.**")
                    .setEphemeral(true).queue();
                return;
            }
            
            
            DiceSession diceSession = new DiceSession(userId, guildId, databaseManager, event);
            if (GameSessionManager.getInstance().startSession(diceSession)) {
                diceSession.startGame();
            } else {
                event.reply("❌ Failed to start dice game! Please try again.").setEphemeral(true).queue();
            }
            
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleRoulette(SlashCommandInteractionEvent event) {
        OptionMapping betOption = event.getOption("bet");
        
        if (betOption == null) {
            event.reply("❌ Please provide a bet amount! Example: `/gamble roulette bet:100`").setEphemeral(true).queue();
            return;
        }
        
        int bet = betOption.getAsInt();
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet) + "\n" +
                    "💸 **Shortage**: $" + String.format("%,d", bet - balance))
                    .setEphemeral(true).queue();
                return;
            }
            
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo)
                    .setEphemeral(true).queue();
                return;
            }
            
            
            RouletteSession rouletteSession = new RouletteSession(userId, guildId, bet, databaseManager, event);
            if (GameSessionManager.getInstance().startSession(rouletteSession)) {
                rouletteSession.startGame();
            } else {
                event.reply("❌ Failed to start roulette game! Please try again.").setEphemeral(true).queue();
            }
            
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleSlots(SlashCommandInteractionEvent event) {
        OptionMapping betOption = event.getOption("bet");
        
        if (betOption == null) {
            event.reply("❌ Please provide a bet amount! Example: `/gamble slots bet:50`").setEphemeral(true).queue();
            return;
        }
        
        int bet = betOption.getAsInt();
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet) + "\n" +
                    "💸 **Shortage**: $" + String.format("%,d", bet - balance))
                    .setEphemeral(true).queue();
                return;
            }
            
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo)
                    .setEphemeral(true).queue();
                return;
            }
            
            
            SlotsSession slotsSession = new SlotsSession(userId, guildId, bet, databaseManager, event);
            if (GameSessionManager.getInstance().startSession(slotsSession)) {
                slotsSession.startGame();
            } else {
                event.reply("❌ Failed to start slots game! Please try again.").setEphemeral(true).queue();
            }
            
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleBlackjack(SlashCommandInteractionEvent event) {
        OptionMapping betOption = event.getOption("bet");
        
        if (betOption == null) {
            event.reply("❌ Please provide a bet amount! Example: `/gamble blackjack bet:200`").setEphemeral(true).queue();
            return;
        }
        
        int bet = betOption.getAsInt();
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet) + "\n" +
                    "💸 **Shortage**: $" + String.format("%,d", bet - balance))
                    .setEphemeral(true).queue();
                return;
            }
            
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo)
                    .setEphemeral(true).queue();
                return;
            }
            
            
            BlackjackSession blackjackSession = new BlackjackSession(userId, guildId, bet, databaseManager, event);
            if (GameSessionManager.getInstance().startSession(blackjackSession)) {
                blackjackSession.startGame();
            } else {
                event.reply("❌ Failed to start blackjack game! Please try again.").setEphemeral(true).queue();
            }
            
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleRussianRoulette(SlashCommandInteractionEvent event) {
        OptionMapping betOption = event.getOption("bet");
        
        if (betOption == null) {
            event.reply("❌ Please provide a bet amount! Example: `/gamble russian-roulette bet:500`").setEphemeral(true).queue();
            return;
        }
        
        int bet = betOption.getAsInt();
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet) + "\n" +
                    "💸 **Shortage**: $" + String.format("%,d", bet - balance))
                    .setEphemeral(true).queue();
                return;
            }
            
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo)
                    .setEphemeral(true).queue();
                return;
            }
            
            
            databaseManager.updateUserBalance(guildId, userId, balance - bet);
            
            
            RussianRouletteSession session = new RussianRouletteSession(userId, guildId, bet, databaseManager, event);
            if (GameSessionManager.getInstance().startSession(session)) {
                session.startGame();
            } else {
                event.reply("❌ Failed to start Russian Roulette game! Please try again.").setEphemeral(true).queue();
            }
            
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleCoinFlip(SlashCommandInteractionEvent event) {
        OptionMapping betOption = event.getOption("bet");
        
        if (betOption == null) {
            event.reply("❌ Please provide a bet amount! Example: `/gamble coin-flip bet:100`").setEphemeral(true).queue();
            return;
        }
        
        int bet = betOption.getAsInt();
        String guildId = event.getGuild().getId();
        String userId = event.getUser().getId();
        
        try {
            databaseManager.initializeUserBalance(guildId, userId, 1000);
            
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                event.reply("❌ **Insufficient Funds!**\n\n" +
                    "💰 **Your Balance**: $" + String.format("%,d", balance) + "\n" +
                    "🎯 **Required**: $" + String.format("%,d", bet) + "\n" +
                    "💸 **Shortage**: $" + String.format("%,d", bet - balance))
                    .setEphemeral(true).queue();
                return;
            }
            
            
            if (GameSessionManager.getInstance().hasActiveSession(guildId, userId)) {
                String sessionInfo = GameSessionManager.getInstance().getActiveSessionInfo(guildId, userId);
                event.reply("⚠️ **Active Game Detected!**\n\n" + sessionInfo)
                    .setEphemeral(true).queue();
                return;
            }
            
            
            CoinFlipSession session = new CoinFlipSession(userId, guildId, bet, databaseManager, event);
            if (GameSessionManager.getInstance().startSession(session)) {
                session.startGame();
            } else {
                event.reply("❌ Failed to start Coin Flip game! Please try again.").setEphemeral(true).queue();
            }
            
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("gamble", "Play casino games")
                .addSubcommands(
                        new SubcommandData("menu", "Casino menu"),
                        new SubcommandData("dice", "Roll dice game"),
                        new SubcommandData("roulette", "Spin the roulette wheel")
                                .addOption(OptionType.INTEGER, "bet", "Bet amount", true),
                        new SubcommandData("slots", "Spin slot machine")
                                .addOption(OptionType.INTEGER, "bet", "Bet amount", true),
                        new SubcommandData("blackjack", "Play blackjack")
                                .addOption(OptionType.INTEGER, "bet", "Bet amount", true),
                        new SubcommandData("russian-roulette", "Single-player Russian Roulette")
                                .addOption(OptionType.INTEGER, "bet", "Bet amount", true),
                        new SubcommandData("coin-flip", "Simple coin flip game")
                                .addOption(OptionType.INTEGER, "bet", "Bet amount", true)
                );
    }

    @Override
    public String getName() {
        return "gamble";
    }

    @Override
    public String getDescription() {
        return "Play casino games";
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