package com.syndicate.bot.commands.gambling;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.Command;
import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BlackjackCommand implements Command {
    private final Random random = new Random();

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        if (args.length < 1) {
            event.getChannel().sendMessage("❌ Usage: `!blackjack <bet_amount>`").queue();
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

            
            List<Card> deck = createDeck();
            Collections.shuffle(deck, random);

            
            List<Card> playerHand = new ArrayList<>();
            List<Card> dealerHand = new ArrayList<>();

            playerHand.add(deck.remove(0));
            dealerHand.add(deck.remove(0));
            playerHand.add(deck.remove(0));
            dealerHand.add(deck.remove(0));

            int playerValue = calculateHandValue(playerHand);
            int dealerValue = calculateHandValue(dealerHand);

            
            boolean playerBlackjack = playerValue == 21 && playerHand.size() == 2;
            boolean dealerBlackjack = dealerValue == 21 && dealerHand.size() == 2;

            String result;
            double multiplier;

            if (playerBlackjack && dealerBlackjack) {
                result = "**PUSH** - Both have Blackjack!";
                multiplier = 1.0; 
            } else if (playerBlackjack) {
                result = "**BLACKJACK!** You win!";
                multiplier = 2.5; 
            } else if (dealerBlackjack) {
                result = "**Dealer Blackjack** - You lose!";
                multiplier = 0.0;
            } else {
                
                
                while (dealerValue < 17) {
                    dealerHand.add(deck.remove(0));
                    dealerValue = calculateHandValue(dealerHand);
                }

                
                if (playerValue > 21) {
                    result = "**BUST!** You lose!";
                    multiplier = 0.0;
                } else if (dealerValue > 21) {
                    result = "**Dealer Bust!** You win!";
                    multiplier = 2.0;
                } else if (playerValue > dealerValue) {
                    result = "**You Win!** Higher hand!";
                    multiplier = 2.0;
                } else if (dealerValue > playerValue) {
                    result = "**Dealer Wins!** Higher hand!";
                    multiplier = 0.0;
                } else {
                    result = "**PUSH** - Tie game!";
                    multiplier = 1.0; 
                }
            }

            
            boolean won = multiplier > 1.0;
            boolean push = multiplier == 1.0;
            long winnings = 0;
            long newBalance;
            long casinoChange = betAmount;

            if (won) {
                winnings = (long) (betAmount * multiplier);
                newBalance = userBalance + winnings - betAmount;
                casinoChange = -winnings + betAmount;
            } else if (push) {
                newBalance = userBalance; 
                casinoChange = 0;
            } else {
                newBalance = userBalance - betAmount;
            }

            
            db.updateUserBalance(guildId, userId, newBalance);
            long casinoBalance = db.getCasinoBankBalance(guildId);
            db.updateCasinoBankBalance(guildId, casinoBalance + casinoChange);

            
            String transactionType = won ? "WIN" : (push ? "PUSH" : "LOSS");
            String description = String.format("Blackjack - Player: %d, Dealer: %d", playerValue, dealerValue);
            db.addTransaction(guildId, userId, transactionType, won ? winnings : (push ? 0 : -betAmount), "blackjack", description);

            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("♠️ Blackjack Results");
            embed.setColor(won ? Color.GREEN : (push ? Color.ORANGE : Color.RED));

            embed.addField("🃏 Your Hand", formatHand(playerHand, playerValue), true);
            embed.addField("🏠 Dealer's Hand", formatHand(dealerHand, dealerValue), true);
            embed.addField("💰 Bet Amount", betAmount + " credits", true);
            
            embed.addField("🎯 Result", result, false);

            if (won) {
                embed.addField("💵 Winnings", winnings + " credits", true);
                embed.addField("💳 New Balance", newBalance + " credits", true);
            } else if (push) {
                embed.addField("💳 Balance", newBalance + " credits (unchanged)", true);
            } else {
                embed.addField("💳 New Balance", newBalance + " credits", true);
            }

            embed.setFooter("The Syndicate's Den • Blackjack", event.getJDA().getSelfUser().getAvatarUrl());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ Invalid bet amount! Please enter a valid number.").queue();
        } catch (SQLException e) {
            event.getChannel().sendMessage("❌ Database error occurred. Please try again.").queue();
        }
    }

    private List<Card> createDeck() {
        List<Card> deck = new ArrayList<>();
        String[] suits = {"♠️", "♥️", "♦️", "♣️"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        int[] values = {11, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10};

        for (String suit : suits) {
            for (int i = 0; i < ranks.length; i++) {
                deck.add(new Card(ranks[i], suit, values[i]));
            }
        }

        return deck;
    }

    private int calculateHandValue(List<Card> hand) {
        int value = 0;
        int aces = 0;

        for (Card card : hand) {
            if (card.rank.equals("A")) {
                aces++;
                value += 11;
            } else {
                value += card.value;
            }
        }

        
        while (value > 21 && aces > 0) {
            value -= 10;
            aces--;
        }

        return value;
    }

    private String formatHand(List<Card> hand, int value) {
        StringBuilder sb = new StringBuilder();
        for (Card card : hand) {
            sb.append(card.toString()).append(" ");
        }
        sb.append("**[").append(value).append("]**");
        return sb.toString();
    }

    private static class Card {
        final String rank;
        final String suit;
        final int value;

        Card(String rank, String suit, int value) {
            this.rank = rank;
            this.suit = suit;
            this.value = value;
        }

        @Override
        public String toString() {
            return rank + suit;
        }
    }

    @Override
    public String getName() {
        return "blackjack";
    }

    @Override
    public String getDescription() {
        return "Play a simplified game of blackjack against the dealer";
    }

    @Override
    public String getUsage() {
        return "!blackjack <bet_amount>";
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