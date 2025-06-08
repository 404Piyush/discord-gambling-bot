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
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BlackjackSession extends GameSession {
    private final DatabaseManager databaseManager;
    private final SlashCommandInteractionEvent originalEvent;
    private final List<String> deck;
    private final List<String> playerHand;
    private final List<String> dealerHand;
    private List<String> splitHand; 
    private final Random random = new Random();
    private boolean playerStood = false;
    private boolean doubledDown = false;
    private boolean hasSplit = false;
    private boolean playingSplitHand = false; 
    private boolean gameEnded = false;
    private Message gameMessage;

    public BlackjackSession(String userId, String guildId, long bet, DatabaseManager databaseManager, SlashCommandInteractionEvent originalEvent) {
        super(userId, guildId, bet);
        this.databaseManager = databaseManager;
        this.originalEvent = originalEvent;
        this.deck = createDeck();
        this.playerHand = new ArrayList<>();
        this.dealerHand = new ArrayList<>();
        
        
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
    }

    private List<String> createDeck() {
        List<String> deck = new ArrayList<>();
        String[] suits = {"♠️", "♥️", "♦️", "♣️"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(rank + suit);
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    private String drawCard() {
        if (deck.isEmpty()) {
            deck.addAll(createDeck());
        }
        return deck.remove(0);
    }

    private int getCardValue(String card) {
        String rank = card.substring(0, card.length() - 2);
        switch (rank) {
            case "A": return 11;
            case "J": case "Q": case "K": return 10;
            default: return Integer.parseInt(rank);
        }
    }

    private int calculateHandValue(List<String> hand) {
        int value = 0;
        int aces = 0;
        
        for (String card : hand) {
            int cardValue = getCardValue(card);
            if (cardValue == 11) aces++;
            value += cardValue;
        }
        
        
        while (value > 21 && aces > 0) {
            value -= 10;
            aces--;
        }
        
        return value;
    }

    public void startGame() {
        
        int playerValue = calculateHandValue(playerHand);
        int dealerValue = calculateHandValue(dealerHand);
        
        if (playerValue == 21) {
            
            gameEnded = true;
            endGameOnBlackjack();
            return;
        }
        
        EmbedBuilder embed = createGameEmbed(false);
        
        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.primary("bj_hit", "🃏 Hit"));
        buttons.add(Button.secondary("bj_stand", "✋ Stand"));
        
        
        if (playerHand.size() == 2) {
            try {
                long balance = databaseManager.getUserBalance(guildId, userId);
                
                
                if (balance >= bet) {
                    buttons.add(Button.success("bj_double", "💰 Double Down"));
                }
                
                
                if (canSplit() && balance >= bet) {
                    buttons.add(Button.danger("bj_split", "🎯 Split"));
                }
            } catch (SQLException e) {
                
            }
        }

        originalEvent.replyEmbeds(embed.build())
                .addComponents(ActionRow.of(buttons))
                .queue(response -> {
                    response.retrieveOriginal().queue(message -> {
                        this.gameMessage = message;
                    });
                });
    }

    private void endGameOnBlackjack() {
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            long winAmount = bet + (bet * 3 / 2); 
            long newBalance = balance - bet + winAmount;
            databaseManager.updateUserBalance(guildId, userId, newBalance);
            
            
            databaseManager.addTransaction(guildId, userId, "blackjack", -bet, null, "Blackjack bet");
            databaseManager.addTransaction(guildId, userId, "blackjack_win", winAmount, null, "BLACKJACK! 3:2 Payout");
            
            EmbedBuilder embed = createGameEmbed(true);
            
            
            embed.addField("─────────────────", "**🎉 BLACKJACK! INSTANT WIN! 🎉**\n🃏 **NATURAL 21!** 🃏", false);
            
            
            long profit = winAmount - bet;
            embed.addField("💰 **Bet Amount**", "`$" + String.format("%,d", bet) + "`", true);
            embed.addField("💸 **Profit**", "🟢 `+$" + String.format("%,d", profit) + "`", true);
            embed.addField("💳 **New Balance**", "`$" + String.format("%,d", newBalance) + "`", true);
            
            embed.addField("🌟 **Blackjack Bonus**", "🔥 **3:2 PAYOUT!** 🔥\nNatural 21 pays extra!", false);
            
            embed.setColor(Color.MAGENTA)
                 .setFooter("The Syndicate's Den • BLACKJACK!");
            
            originalEvent.replyEmbeds(embed.build()).queue();
            
        } catch (SQLException e) {
            originalEvent.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
        
        end();
        GameSessionManager.getInstance().endSession(guildId, userId);
    }

    public void handleButtonInteraction(ButtonInteractionEvent event) {
        if (gameEnded || !isActive || !event.getUser().getId().equals(userId)) {
            if (!event.isAcknowledged()) {
                event.reply("❌ This is not your game or the game has ended!").setEphemeral(true).queue();
            }
            return;
        }

        String buttonId = event.getComponentId();
        
        switch (buttonId) {
            case "bj_hit":
                handleHit(event);
                break;
            case "bj_stand":
                handleStand(event);
                break;
            case "bj_double":
                handleDoubleDown(event);
                break;
            case "bj_split":
                handleSplit(event);
                break;
        }
    }

    private void handleHit(ButtonInteractionEvent event) {
        if (hasSplit) {
            
            if (playingSplitHand) {
                splitHand.add(drawCard());
                int splitValue = calculateHandValue(splitHand);
                if (splitValue > 21) {
                    
                    playingSplitHand = false;
                    if (calculateHandValue(playerHand) > 21) {
                        endGame(event, false, "Both Hands Bust!");
                    } else {
                        handleStand(event); 
                    }
                } else if (splitValue == 21) {
                    
                    playingSplitHand = false;
                    updateGameMessage(event, false);
                } else {
                    updateGameMessage(event, false);
                }
            } else {
                playerHand.add(drawCard());
                int playerValue = calculateHandValue(playerHand);
                if (playerValue > 21) {
                    
                    playingSplitHand = true;
                    if (calculateHandValue(splitHand) > 21) {
                        endGame(event, false, "Both Hands Bust!");
                    } else {
                        updateGameMessage(event, false);
                    }
                } else if (playerValue == 21) {
                    
                    playingSplitHand = true;
                    updateGameMessage(event, false);
                } else {
                    updateGameMessage(event, false);
                }
            }
        } else {
            
            playerHand.add(drawCard());
            
            int playerValue = calculateHandValue(playerHand);
            if (playerValue > 21) {
                endGame(event, false, "Player Bust!");
            } else if (playerValue == 21) {
                handleStand(event);
            } else {
                updateGameMessage(event, false);
            }
        }
    }

    private void handleStand(ButtonInteractionEvent event) {
        playerStood = true;
        
        
        while (calculateHandValue(dealerHand) < 17) {
            dealerHand.add(drawCard());
        }
        
        
        int playerValue = calculateHandValue(playerHand);
        int dealerValue = calculateHandValue(dealerHand);
        
        String result;
        boolean playerWon;
        
        if (dealerValue > 21) {
            result = "Dealer Bust! You Win!";
            playerWon = true;
        } else if (playerValue > dealerValue) {
            result = "You Win!";
            playerWon = true;
        } else if (playerValue == dealerValue) {
            result = "Push (Tie)";
            playerWon = false;
        } else {
            result = "You Lost";
            playerWon = false;
        }
        
        endGame(event, playerWon, result);
    }

    private void handleDoubleDown(ButtonInteractionEvent event) {
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                if (!event.isAcknowledged()) {
                    event.reply("❌ Insufficient funds to double down! You need $" + String.format("%,d", bet) + " more.").setEphemeral(true).queue();
                }
                return;
            }
            
            doubledDown = true;
            playerHand.add(drawCard()); 
            
            int playerValue = calculateHandValue(playerHand);
            if (playerValue > 21) {
                endGame(event, false, "Player Bust!");
            } else {
                handleStand(event); 
            }
        } catch (SQLException e) {
            if (!event.isAcknowledged()) {
                event.reply("❌ Database error occurred!").setEphemeral(true).queue();
            }
        }
    }

    private void updateGameMessage(ButtonInteractionEvent event, boolean gameEnded) {
        EmbedBuilder embed = createGameEmbed(gameEnded);
        
        if (!event.isAcknowledged()) {
            event.deferEdit().queue();
        }
        
        if (gameEnded) {
            event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
        } else {
            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.primary("bj_hit", "🃏 Hit"));
            buttons.add(Button.secondary("bj_stand", "✋ Stand"));
            
            event.getHook().editOriginalEmbeds(embed.build()).setComponents(ActionRow.of(buttons)).queue();
        }
    }

    private void endGame(ButtonInteractionEvent event, boolean playerWon, String result) {
        gameEnded = true;
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            long totalBet = doubledDown ? bet * 2 : bet;
            long winAmount = 0;
            
            if (playerWon) {
                winAmount = totalBet * 2;
            } else if (result.equals("Push (Tie)")) {
                winAmount = totalBet; 
            }
            
            long newBalance = balance - totalBet + winAmount;
            databaseManager.updateUserBalance(guildId, userId, newBalance);
            
            
            databaseManager.addTransaction(guildId, userId, "blackjack", -totalBet, null, "Blackjack bet" + (doubledDown ? " (doubled)" : ""));
            if (winAmount > 0) {
                databaseManager.addTransaction(guildId, userId, "blackjack_" + (playerWon ? "win" : "push"), winAmount, null, "Blackjack " + (playerWon ? "win" : "push"));
            }
            
            EmbedBuilder embed = createGameEmbed(true);
            
            
            String resultEmoji = playerWon ? "🎉" : (result.equals("Push (Tie)") ? "🤝" : "💔");
            String resultTitle = playerWon ? "**🎉 CONGRATULATIONS! 🎉**" : (result.equals("Push (Tie)") ? "**🤝 PUSH - TIE GAME 🤝**" : "**💔 HOUSE WINS 💔**");
            
            embed.addField("─────────────────", resultTitle + "\n" + resultEmoji + " **" + result + "** " + resultEmoji, false);
            
            
            long profit = winAmount - totalBet;
            String profitColor = profit > 0 ? "🟢" : (profit == 0 ? "🟡" : "🔴");
            String profitSign = profit >= 0 ? "+" : "";
            
            embed.addField("💰 **Bet Amount**", "`$" + String.format("%,d", totalBet) + "`", true);
            embed.addField("💸 **Profit/Loss**", profitColor + " `" + profitSign + "$" + String.format("%,d", profit) + "`", true);
            embed.addField("💳 **New Balance**", "`$" + String.format("%,d", newBalance) + "`", true);
            
            if (playerWon && profit >= totalBet * 2) {
                embed.addField("🌟 **Bonus**", "🔥 **EXCELLENT WIN!** 🔥\nYou're on fire!", false);
            }
            
            embed.setColor(playerWon ? Color.GREEN : (result.equals("Push (Tie)") ? Color.YELLOW : Color.RED))
                 .setFooter("The Syndicate's Den • Game Complete");
            
            if (!event.isAcknowledged()) {
                event.deferEdit().queue();
            }
            event.getHook().editOriginalEmbeds(embed.build()).setComponents().queue();
            
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
        
        end();
        GameSessionManager.getInstance().endSession(guildId, userId);
    }

    private EmbedBuilder createGameEmbed(boolean showDealerCards) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🃏 **Blackjack Table**")
                .setColor(Color.BLUE);

        
        if (hasSplit) {
            
            StringBuilder playerCards = new StringBuilder();
            for (String card : playerHand) {
                playerCards.append("`").append(card).append("` ");
            }
            int playerValue = calculateHandValue(playerHand);
            String playerStatus = playerValue > 21 ? " **[BUST]**" : (playerValue == 21 ? " **[BLACKJACK!]**" : "");
            String activeMarker = !playingSplitHand ? " **←** " : "";
            
            embed.addField("🎲 **Hand 1** (" + playerValue + ")" + playerStatus + activeMarker, 
                          "┌─────────────┐\n" +
                          "│ " + playerCards.toString() + "│\n" +
                          "└─────────────┘", false);
            
            
            StringBuilder splitCards = new StringBuilder();
            for (String card : splitHand) {
                splitCards.append("`").append(card).append("` ");
            }
            int splitValue = calculateHandValue(splitHand);
            String splitStatus = splitValue > 21 ? " **[BUST]**" : (splitValue == 21 ? " **[BLACKJACK!]**" : "");
            String splitActiveMarker = playingSplitHand ? " **←** " : "";
            
            embed.addField("🎯 **Hand 2** (" + splitValue + ")" + splitStatus + splitActiveMarker, 
                          "┌─────────────┐\n" +
                          "│ " + splitCards.toString() + "│\n" +
                          "└─────────────┘", false);
        } else {
            
            StringBuilder playerCards = new StringBuilder();
            for (String card : playerHand) {
                playerCards.append("`").append(card).append("` ");
            }
            int playerValue = calculateHandValue(playerHand);
            String playerStatus = playerValue > 21 ? " **[BUST]**" : (playerValue == 21 ? " **[BLACKJACK!]**" : "");
            
            embed.addField("🎲 **Your Hand** (" + playerValue + ")" + playerStatus, 
                          "┌─────────────┐\n" +
                          "│ " + playerCards.toString() + "│\n" +
                          "└─────────────┘", false);
        }

        
        StringBuilder dealerCards = new StringBuilder();
        if (showDealerCards || playerStood || gameEnded) {
            for (String card : dealerHand) {
                dealerCards.append("`").append(card).append("` ");
            }
            int dealerValue = calculateHandValue(dealerHand);
            String dealerStatus = dealerValue > 21 ? " **[BUST]**" : (dealerValue == 21 ? " **[BLACKJACK!]**" : "");
            
            embed.addField("🎰 **Dealer Hand** (" + dealerValue + ")" + dealerStatus,
                          "┌─────────────┐\n" +
                          "│ " + dealerCards.toString() + "│\n" +
                          "└─────────────┘", false);
        } else {
            
            String visibleCard = dealerHand.get(0);
            dealerCards.append("`").append(visibleCard).append("` `❓❓`");
            embed.addField("🎰 **Dealer Hand** (?)",
                          "┌─────────────┐\n" +
                          "│ " + dealerCards.toString() + " │\n" +
                          "└─────────────┘", false);
        }

        
        String betInfo;
        if (hasSplit) {
            long totalBet = bet * 2; 
            betInfo = "$" + String.format("%,d", totalBet) + " **[SPLIT]**";
        } else {
            betInfo = "$" + String.format("%,d", doubledDown ? bet * 2 : bet) + (doubledDown ? " **[DOUBLED]**" : "");
        }
        embed.addField("💰 **Current Bet**", betInfo, true);
        
        if (!showDealerCards && !playerStood && !gameEnded) {
            embed.addField("🎯 **Next Move**", "Choose your action below!", true);
        }
        
        embed.setFooter("The Syndicate's Den • Blackjack")
             .setTimestamp(java.time.Instant.now());

        return embed;
    }

    private boolean canSplit() {
        
        if (playerHand.size() != 2 || hasSplit) return false;
        
        String card1 = playerHand.get(0);
        String card2 = playerHand.get(1);
        
        
        String rank1 = card1.substring(0, card1.length() - 2);
        String rank2 = card2.substring(0, card2.length() - 2);
        
        
        boolean card1Is10 = rank1.equals("10") || rank1.equals("J") || rank1.equals("Q") || rank1.equals("K");
        boolean card2Is10 = rank2.equals("10") || rank2.equals("J") || rank2.equals("Q") || rank2.equals("K");
        
        return rank1.equals(rank2) || (card1Is10 && card2Is10);
    }
    
    private void handleSplit(ButtonInteractionEvent event) {
        try {
            long balance = databaseManager.getUserBalance(guildId, userId);
            if (balance < bet) {
                if (!event.isAcknowledged()) {
                    event.reply("❌ Insufficient funds to split! You need $" + String.format("%,d", bet) + " more.").setEphemeral(true).queue();
                }
                return;
            }
            
            
            hasSplit = true;
            splitHand = new ArrayList<>();
            splitHand.add(playerHand.remove(1)); 
            
            
            playerHand.add(drawCard());
            splitHand.add(drawCard());
            
            
            updateGameMessage(event, false);
            
        } catch (SQLException e) {
            if (!event.isAcknowledged()) {
                event.reply("❌ Database error occurred!").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void timeout() {
        gameEnded = true;
        if (gameMessage != null) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🃏 Blackjack Game - Timed Out")
                    .setDescription("Game timed out after 5 minutes. Your bet has been returned.")
                    .setColor(Color.ORANGE);
            
            gameMessage.editMessageEmbeds(embed.build()).setComponents().queue();
        }
        
        GameSessionManager.getInstance().endSession(guildId, userId);
    }
} 