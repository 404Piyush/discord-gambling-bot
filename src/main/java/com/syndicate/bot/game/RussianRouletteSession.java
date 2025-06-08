package com.syndicate.bot.game;

import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class RussianRouletteSession extends GameSession {
    private final int originalBet;
    private final DatabaseManager databaseManager;
    private final SlashCommandInteractionEvent originalEvent;
    
    
    private int bulletCount = 0;
    private double currentMultiplier = 1.0;
    private boolean gameStarted = false;
    private boolean isVsBot = false;
    private boolean isPlayerTurn = true;
    private List<Boolean> chambers; 
    private int currentChamber = 0;
    private int shotsPerformed = 0;
    
    
    private static final double HOUSE_EDGE = 0.95; 

    public RussianRouletteSession(String playerId, String guildId, int bet, DatabaseManager databaseManager, SlashCommandInteractionEvent event) {
        super(playerId, guildId, bet);
        this.originalBet = bet;
        this.databaseManager = databaseManager;
        this.originalEvent = event;
    }

    public void startGame() {
        showBulletSelection();
    }

    private void showBulletSelection() {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔫 Russian Roulette - Bullet Selection")
                .setDescription("**💀 Welcome to the most dangerous game!**\n\n" +
                    "📊 **Your Bet**: $" + String.format("%,d", originalBet) + "\n\n" +
                    "🔢 **Choose how many bullets to load:**\n" +
                    "• More bullets = Higher risk = Better multipliers\n" +
                    "• You can quit anytime to claim current multiplier\n" +
                    "• House takes 5% edge on quit rewards\n\n" +
                    "🎯 **Select bullet count to see potential rewards:**")
                .addField("1 Bullet", "Survival: 83% | Multiplier: 1.1x", true)
                .addField("2 Bullets", "Survival: 67% | Multiplier: 1.4x", true)
                .addField("3 Bullets", "Survival: 50% | Multiplier: 1.9x", true)
                .addField("4 Bullets", "Survival: 33% | Multiplier: 2.8x", true)
                .addField("5 Bullets", "Survival: 17% | Multiplier: 5.7x", true)
                .addField("💀 Risk vs Reward", "More bullets = Higher risk = Better multipliers", false)
                .setColor(Color.DARK_GRAY)
                .setFooter("The Syndicate's Den • High Stakes Gaming")
                .setTimestamp(java.time.Instant.now());

        originalEvent.replyEmbeds(embed.build())
            .addComponents(
                ActionRow.of(
                    Button.primary("rr_bullets_1", "1 Bullet"),
                    Button.primary("rr_bullets_2", "2 Bullets"),
                    Button.primary("rr_bullets_3", "3 Bullets")
                ),
                ActionRow.of(
                    Button.danger("rr_bullets_4", "4 Bullets"),
                    Button.danger("rr_bullets_5", "5 Bullets")
                ),
                ActionRow.of(
                    Button.secondary("rr_cancel", "❌ Cancel Game")
                )
            ).queue();
    }

    private void showGameModeSelection() {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔫 Russian Roulette - Game Mode")
                .setDescription("**🎲 Select your game mode:**\n\n" +
                    "📊 **Bullets Loaded**: " + bulletCount + "\n" +
                    "💰 **Your Bet**: $" + String.format("%,d", originalBet) + "\n\n" +
                    "🎮 **Choose how you want to play:**")
                .addField("🎯 Solo Mode", 
                    "Play alone against the revolver\n" +
                    "• Build climbing multipliers\n" +
                    "• Quit anytime for current reward\n" +
                    "• Maximum control over your fate", false)
                .addField("🤖 Vs Bot Mode", 
                    "Take turns with AI opponent\n" +
                    "• Winner takes " + String.format("%.1f", calculateBulletMultiplier()) + "x the pot\n" +
                    "• Turn-based tension\n" +
                    "• First to die loses everything", false)
                .setColor(Color.ORANGE)
                .setFooter("The Syndicate's Den • Choose Your Destiny")
                .setTimestamp(java.time.Instant.now());

        originalEvent.getHook().editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.primary("rr_mode_solo", "🎯 Solo Mode"),
                    Button.danger("rr_mode_bot", "🤖 Vs Bot Mode")
                ),
                ActionRow.of(
                    Button.secondary("rr_back", "⬅️ Back to Bullet Selection"),
                    Button.secondary("rr_cancel", "❌ Cancel Game")
                )
            ).queue();
    }

    private void startSoloGame() {
        isVsBot = false;
        gameStarted = true;
        setupChambers();
        showSoloGameState();
    }

    private void startVsBotGame() {
        isVsBot = true;
        gameStarted = true;
        isPlayerTurn = new Random().nextBoolean(); 
        setupChambers();
        showVsBotGameState();
    }

    private void setupChambers() {
        chambers = new ArrayList<>();
        
        
        for (int i = 0; i < bulletCount; i++) {
            chambers.add(true);
        }
        
        
        for (int i = bulletCount; i < 6; i++) {
            chambers.add(false);
        }
        
        
        Collections.shuffle(chambers);
        
        currentChamber = 0;
        shotsPerformed = 0;
        currentMultiplier = calculateBaseMultiplier();
    }

    private double calculateBaseMultiplier() {
        
        
        int emptyChambers = 6 - bulletCount;
        if (emptyChambers <= 0) return 1.0; 
        
        double fairOdds = 6.0 / emptyChambers;
        double withHouseEdge = fairOdds * 0.95; 
        
        
        return Math.round(withHouseEdge * 10.0) / 10.0;
    }

    private double calculateBulletMultiplier() {
        
        if (bulletCount == 0) return 1.9; 
        
        int emptyChambers = 6 - bulletCount;
        if (emptyChambers <= 0) return 1.0;
        
        double fairOdds = 6.0 / emptyChambers;
        double withHouseEdge = fairOdds * 0.95; 
        
        return Math.round(withHouseEdge * 10.0) / 10.0;
    }

    private void showSoloGameState() {
        int emptyRemaining = 6 - bulletCount - shotsPerformed;
        int bulletsRemaining = bulletCount;
        
        
        for (int i = currentChamber; i < chambers.size(); i++) {
            if (chambers.get(i)) {
                
            }
        }

        String chambersDisplay = generateChambersDisplay();
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔫 Russian Roulette - Solo Mode")
                .setDescription("**💀 The cylinder spins... your fate awaits**\n\n" +
                    chambersDisplay + "\n" +
                    "🎯 **Current Status:**\n" +
                    "• Shots Fired: " + shotsPerformed + "/6\n" +
                    "• Bullets Remaining: " + bulletsRemaining + "\n" +
                    "• Empty Chambers Left: " + emptyRemaining + "\n\n" +
                    "💰 **Rewards:**\n" +
                    "• Current Multiplier: **" + String.format("%.1f", currentMultiplier) + "x**\n" +
                    "• Quit Reward: $" + String.format("%,d", (int)(originalBet * currentMultiplier * HOUSE_EDGE)) + "\n" +
                    "• Potential Next: **" + String.format("%.1f", currentMultiplier + calculateMultiplierIncrease()) + "x**")
                .setColor(Color.RED)
                .setFooter("The Syndicate's Den • One Pull Could Change Everything")
                .setTimestamp(java.time.Instant.now());

        originalEvent.getHook().editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.danger("rr_pull", "🔫 Pull Trigger"),
                    Button.success("rr_quit", "💰 Quit & Take Reward")
                ),
                ActionRow.of(
                    Button.secondary("rr_spin", "🔄 Spin Cylinder (Animation)"),
                    Button.secondary("rr_stats", "📊 Game Statistics")
                )
            ).queue();
    }

    private void showVsBotGameState() {
        String chambersDisplay = generateChambersDisplay();
        String turnIndicator = isPlayerTurn ? "👤 **YOUR TURN**" : "🤖 **BOT'S TURN**";
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔫 Russian Roulette - Vs Bot Mode")
                .setDescription("**💀 Turn-based death match!**\n\n" +
                    chambersDisplay + "\n" +
                    turnIndicator + "\n\n" +
                    "🎯 **Game Status:**\n" +
                    "• Shots Fired: " + shotsPerformed + "/6\n" +
                    "• Bullets Loaded: " + bulletCount + "\n" +
                    "• Winner Gets: $" + String.format("%,d", (int)(originalBet * currentMultiplier)) + " (" + String.format("%.1f", currentMultiplier) + "x)\n\n" +
                    (isPlayerTurn ? "⚡ **Make your move!**" : "🤖 **Bot is deciding...**"))
                .setColor(isPlayerTurn ? Color.BLUE : Color.GRAY)
                .setFooter("The Syndicate's Den • Death or Glory")
                .setTimestamp(java.time.Instant.now());

        if (isPlayerTurn) {
            originalEvent.getHook().editOriginalEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.danger("rr_pull_vs", "🔫 Pull Trigger"),
                        Button.secondary("rr_spin", "🔄 Spin Cylinder")
                    )
                ).queue();
        } else {
            originalEvent.getHook().editOriginalEmbeds(embed.build())
                .setComponents(
                    ActionRow.of(
                        Button.secondary("rr_waiting", "⏳ Waiting for Bot...").asDisabled()
                    )
                ).queue();
            
            
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    botTakeTurn();
                }
            }, 2000 + new Random().nextInt(2000));
        }
    }

    private String generateChambersDisplay() {
        StringBuilder display = new StringBuilder("🔫 **Revolver Chambers**: ");
        
        for (int i = 0; i < 6; i++) {
            if (i < shotsPerformed) {
                display.append("💨 "); 
            } else if (i == currentChamber && gameStarted) {
                display.append("➡️ "); 
            } else {
                display.append("❓ "); 
            }
        }
        
        return display.toString();
    }

    private double calculateMultiplierIncrease() {
        
        int remainingBullets = 0;
        int remainingChambers = 6 - shotsPerformed;
        
        
        for (int i = currentChamber; i < chambers.size(); i++) {
            if (chambers.get(i)) {
                remainingBullets++;
            }
        }
        
        if (remainingChambers <= 0 || remainingBullets <= 0) return 0.1;
        
        
        double survivalChance = (double)(remainingChambers - remainingBullets) / remainingChambers;
        double increase = Math.max(0.1, survivalChance * 0.2); 
        
        return Math.round(increase * 10.0) / 10.0;
    }

    private void botTakeTurn() {
        
        performShot(false);
    }

    public void handleButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        try {
            if (buttonId.startsWith("rr_bullets_")) {
                bulletCount = Integer.parseInt(buttonId.substring(11));
                event.deferEdit().queue();
                showGameModeSelection();
                
            } else if (buttonId.equals("rr_mode_solo")) {
                event.deferEdit().queue();
                startSoloGame();
                
            } else if (buttonId.equals("rr_mode_bot")) {
                event.deferEdit().queue();
                startVsBotGame();
                
            } else if (buttonId.equals("rr_pull")) {
                event.deferEdit().queue();
                performShot(true);
                
            } else if (buttonId.equals("rr_pull_vs")) {
                if (isPlayerTurn) {
                    event.deferEdit().queue();
                    performShot(false);
                } else {
                    event.reply("❌ It's not your turn!").setEphemeral(true).queue();
                }
                
            } else if (buttonId.equals("rr_quit")) {
                event.deferEdit().queue();
                quitGame();
                
            } else if (buttonId.equals("rr_spin")) {
                event.deferEdit().queue();
                showSpinAnimation();
                
            } else if (buttonId.equals("rr_cancel")) {
                event.deferEdit().queue();
                cancelGame();
                
            } else if (buttonId.equals("rr_back")) {
                event.deferEdit().queue();
                bulletCount = 0; 
                gameStarted = false;
                showBulletSelection();
            }
            
        } catch (Exception e) {
            event.reply("❌ An error occurred!").setEphemeral(true).queue();
        }
    }

    private void performShot(boolean isSoloMode) {
        boolean hitBullet = chambers.get(currentChamber);
        shotsPerformed++;
        currentChamber++;
        
        if (hitBullet) {
            
            if (isSoloMode || isPlayerTurn) {
                showPlayerDeath();
            } else {
                showBotDeath();
            }
        } else {
            
            if (isSoloMode) {
                currentMultiplier += calculateMultiplierIncrease();
                showEmptyChambcerSolo();
            } else {
                
                isPlayerTurn = !isPlayerTurn;
                showEmptyChamberVsBot();
            }
        }
    }

    private void showEmptyChambcerSolo() {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("💨 CLICK! Empty Chamber")
                .setDescription("**🎯 You survived another round!**\n\n" +
                    "🔥 **Multiplier Increased!**\n" +
                    "• Previous: " + String.format("%.1f", currentMultiplier - calculateMultiplierIncrease()) + "x\n" +
                    "• Current: **" + String.format("%.1f", currentMultiplier) + "x**\n" +
                    "• Potential Reward: $" + String.format("%,d", (int)(originalBet * currentMultiplier * HOUSE_EDGE)) + "\n\n" +
                    "⚡ **The tension builds...**")
                .setColor(Color.GREEN)
                .setFooter("Lucky... but how long will it last?")
                .setTimestamp(java.time.Instant.now());

        originalEvent.getHook().editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.success("rr_continue", "▶️ Continue Game")
                )
            ).queue();
        
        
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (shotsPerformed >= 6 - bulletCount) {
                    
                    showSoloVictory();
                } else {
                    showSoloGameState();
                }
            }
        }, 3000);
    }

    private void showEmptyChamberVsBot() {
        String survivor = isPlayerTurn ? "Bot" : "You";
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("💨 CLICK! Empty Chamber")
                .setDescription("**" + (!isPlayerTurn ? "🤖 Bot" : "👤 You") + " survived this round!**\n\n" +
                    "🔄 **Turn switches to " + (isPlayerTurn ? "👤 **YOU**" : "🤖 **BOT**") + "\n\n" +
                    "⚡ **The game continues...**")
                .setColor(Color.YELLOW)
                .setFooter("Tension builds with each empty chamber...")
                .setTimestamp(java.time.Instant.now());

        originalEvent.getHook().editOriginalEmbeds(embed.build())
            .setComponents(
                ActionRow.of(
                    Button.secondary("rr_continue_vs", "▶️ Continue Turn-Based Game")
                )
            ).queue();
        
        
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                showVsBotGameState();
            }
        }, 2000);
    }

    private void showPlayerDeath() {
        try {
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("💀 BANG! You're Dead!")
                    .setDescription("**🔫 The bullet finds its mark...**\n\n" +
                        "💸 **You Lost**: $" + String.format("%,d", originalBet) + "\n" +
                        "💰 **Remaining Balance**: $" + String.format("%,d", databaseManager.getUserBalance(guildId, userId)) + "\n\n" +
                        "🎲 **Better luck next time!**")
                    .setColor(Color.BLACK)
                    .setFooter("The house always wins in the end...")
                    .setTimestamp(java.time.Instant.now());

            originalEvent.getHook().editOriginalEmbeds(embed.build())
                .setComponents().queue();
                
        } catch (SQLException e) {
            
        }
        
        endGame();
    }

    private void showBotDeath() {
        try {
            
            int totalPayout = (int)(originalBet * currentMultiplier);
            int profit = totalPayout - originalBet;
            
            
            long currentBalance = databaseManager.getUserBalance(guildId, userId);
            databaseManager.updateUserBalance(guildId, userId, currentBalance + totalPayout);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🎉 BANG! Bot is Dead!")
                    .setDescription("**🤖 The bot drew the short straw!**\n\n" +
                        "💰 **Total Payout**: $" + String.format("%,d", totalPayout) + " (" + String.format("%.1f", currentMultiplier) + "x)\n" +
                        "🎯 **Your Profit**: $" + String.format("%,d", profit) + "\n" +
                        "💰 **New Balance**: $" + String.format("%,d", databaseManager.getUserBalance(guildId, userId)) + "\n\n" +
                        "🏆 **Victory is yours!**")
                    .setColor(new Color(255, 215, 0))
                    .setFooter("Sometimes it pays to be lucky!")
                    .setTimestamp(java.time.Instant.now());

            originalEvent.getHook().editOriginalEmbeds(embed.build())
                .setComponents().queue();
                
        } catch (SQLException e) {
            
        }
        
        endGame();
    }

    private void showSoloVictory() {
        try {
            
            double finalMultiplier = currentMultiplier + 1.0; 
            int totalPayout = (int)(originalBet * finalMultiplier);
            int profit = totalPayout - originalBet;
            long currentBalance = databaseManager.getUserBalance(guildId, userId);
            databaseManager.updateUserBalance(guildId, userId, currentBalance + totalPayout);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("👑 ULTIMATE SURVIVAL!")
                    .setDescription("**🎯 You survived ALL empty chambers!**\n\n" +
                        "🔥 **Final Multiplier**: " + String.format("%.1f", finalMultiplier) + "x\n" +
                        "💰 **Total Payout**: $" + String.format("%,d", totalPayout) + "\n" +
                        "🎯 **Your Profit**: $" + String.format("%,d", profit) + "\n" +
                        "💎 **New Balance**: $" + String.format("%,d", databaseManager.getUserBalance(guildId, userId)) + "\n\n" +
                        "🏆 **LEGENDARY PERFORMANCE!**")
                    .setColor(Color.MAGENTA)
                    .setFooter("Few dare to go this far... fewer survive!")
                    .setTimestamp(java.time.Instant.now());

            originalEvent.getHook().editOriginalEmbeds(embed.build())
                .setComponents().queue();
                
        } catch (SQLException e) {
            
        }
        
        endGame();
    }

    private void quitGame() {
        try {
            
            int totalPayout = (int)(originalBet * currentMultiplier * HOUSE_EDGE);
            int profit = totalPayout - originalBet;
            long currentBalance = databaseManager.getUserBalance(guildId, userId);
            databaseManager.updateUserBalance(guildId, userId, currentBalance + totalPayout);
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("💰 Smart Exit!")
                    .setDescription("**🧠 You chose to quit while ahead!**\n\n" +
                        "📊 **Final Multiplier**: " + String.format("%.1f", currentMultiplier) + "x (5% house edge)\n" +
                        "💰 **Total Payout**: $" + String.format("%,d", totalPayout) + "\n" +
                        "🎯 **Your Profit**: $" + String.format("%,d", profit) + "\n" +
                        "💎 **New Balance**: $" + String.format("%,d", databaseManager.getUserBalance(guildId, userId)) + "\n\n" +
                        "🎯 **Sometimes discretion is the better part of valor!**")
                    .setColor(Color.BLUE)
                    .setFooter("Live to gamble another day!")
                    .setTimestamp(java.time.Instant.now());

            originalEvent.getHook().editOriginalEmbeds(embed.build())
                .setComponents().queue();
                
        } catch (SQLException e) {
            
        }
        
        endGame();
    }

    private void cancelGame() {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Game Cancelled")
                .setDescription("**No bets were placed.**\n\nYour balance remains unchanged.")
                .setColor(Color.GRAY)
                .setFooter("Perhaps another time...")
                .setTimestamp(java.time.Instant.now());

        originalEvent.getHook().editOriginalEmbeds(embed.build())
            .setComponents().queue();
            
        endGame();
    }

    private void showSpinAnimation() {
        String[] spinFrames = {
            "🔫 ⚡ ◯ ◯ ◯ ◯",
            "🔫 ◯ ⚡ ◯ ◯ ◯", 
            "🔫 ◯ ◯ ⚡ ◯ ◯",
            "🔫 ◯ ◯ ◯ ⚡ ◯",
            "🔫 ◯ ◯ ◯ ◯ ⚡",
            "🔫 ⚡ ◯ ◯ ◯ ◯"
        };
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔄 Spinning Cylinder...")
                .setDescription("**The cylinder spins rapidly...**\n\n" + spinFrames[0])
                .setColor(Color.YELLOW)
                .setTimestamp(java.time.Instant.now());

        originalEvent.getHook().editOriginalEmbeds(embed.build())
            .setComponents().queue();
        
        
        Timer timer = new Timer();
        final int[] frame = {0};
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                frame[0]++;
                if (frame[0] >= spinFrames.length * 3) {
                    timer.cancel();
                    
                    if (isVsBot) {
                        showVsBotGameState();
                    } else {
                        showSoloGameState();
                    }
                    return;
                }
                
                embed.setDescription("**The cylinder spins rapidly...**\n\n" + 
                    spinFrames[frame[0] % spinFrames.length]);
                    
                originalEvent.getHook().editOriginalEmbeds(embed.build()).queue();
            }
        }, 200, 200);
    }

    private void endGame() {
        GameSessionManager.getInstance().endSession(guildId, userId);
    }

    @Override
    public void timeout() {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⏰ Game Timeout")
                .setDescription("**Russian Roulette game timed out.**\n\nNo bets were deducted.")
                .setColor(Color.GRAY)
                .setTimestamp(java.time.Instant.now());

        originalEvent.getHook().editOriginalEmbeds(embed.build())
            .setComponents().queue();
        
        endGame();
    }

    public String getGameInfo() {
        if (!gameStarted) {
            return "🔫 Russian Roulette (Setting up - " + bulletCount + " bullets)";
        }
        
        if (isVsBot) {
            return "🔫 Russian Roulette vs Bot (" + shotsPerformed + "/6 shots, " + 
                (isPlayerTurn ? "Your turn" : "Bot's turn") + ")";
        } else {
            return "🔫 Russian Roulette Solo (" + shotsPerformed + "/6 shots, " + 
                String.format("%.1f", currentMultiplier) + "x multiplier)";
        }
    }
} 