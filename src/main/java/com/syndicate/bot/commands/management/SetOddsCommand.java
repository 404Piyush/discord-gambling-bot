package com.syndicate.bot.commands.management;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.Command;
import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;

public class SetOddsCommand implements Command {

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        try {
            DatabaseManager db = SyndicateBot.getDatabaseManager();
            String guildId = event.getGuild().getId();
            String userId = event.getAuthor().getId();

            
            if (!db.isCommitteeMember(guildId, userId)) {
                event.getChannel().sendMessage("❌ This command is restricted to committee members only!").queue();
                return;
            }

            if (args.length == 0) {
                showCurrentOdds(event);
                return;
            }

            if (args.length < 3) {
                event.getChannel().sendMessage("❌ Usage: `!setodds <game> <house_edge> <max_bet>`\n" +
                        "Games: dice, roulette, slots, blackjack\n" +
                        "Example: `!setodds dice 0.05 5000`").queue();
                return;
            }

            String gameName = args[0].toLowerCase();
            double houseEdge;
            long maxBet;

            try {
                houseEdge = Double.parseDouble(args[1]);
                maxBet = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                event.getChannel().sendMessage("❌ Invalid numbers! House edge should be decimal (e.g., 0.05 for 5%) and max bet should be a whole number.").queue();
                return;
            }

            
            if (!isValidGame(gameName)) {
                event.getChannel().sendMessage("❌ Invalid game! Available games: dice, roulette, slots, blackjack").queue();
                return;
            }

            if (houseEdge < 0 || houseEdge > 0.5) {
                event.getChannel().sendMessage("❌ House edge must be between 0% and 50% (0.0 - 0.5)").queue();
                return;
            }

            if (maxBet < 1 || maxBet > 1000000) {
                event.getChannel().sendMessage("❌ Max bet must be between 1 and 1,000,000 credits").queue();
                return;
            }

            
            db.setGameConfiguration(guildId, gameName, houseEdge, 1, maxBet, true);

            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("⚙️ Game Settings Updated");
            embed.setColor(Color.GREEN);

            embed.addField("🎮 Game", capitalizeFirst(gameName), true);
            embed.addField("📊 House Edge", String.format("%.1f%%", houseEdge * 100), true);
            embed.addField("💰 Max Bet", String.format("%,d credits", maxBet), true);

            embed.addField("📈 Impact", getImpactDescription(houseEdge, maxBet), false);
            
            embed.addField("⚠️ Notice", 
                    "These settings will apply to all future games.\n" +
                    "Current active games will not be affected.", false);

            embed.setFooter("The Syndicate's Den • Game Configuration", event.getJDA().getSelfUser().getAvatarUrl());

            event.getChannel().sendMessageEmbeds(embed.build()).queue();

        } catch (SQLException e) {
            event.getChannel().sendMessage("❌ Database error occurred. Please try again.").queue();
        }
    }

    private void showCurrentOdds(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("⚙️ Current Game Settings");
        embed.setColor(Color.CYAN);
        embed.setDescription("Default settings for all casino games");

        embed.addField("🎲 Dice", "House Edge: 5%\nMax Bet: 10,000 credits", true);
        embed.addField("🎰 Roulette", "House Edge: 5%\nMax Bet: 10,000 credits", true);
        embed.addField("🎰 Slots", "House Edge: 5%\nMax Bet: 10,000 credits", true);
        embed.addField("♠️ Blackjack", "House Edge: 5%\nMax Bet: 10,000 credits", true);

        embed.addField("📝 Usage", 
                "• `!setodds <game> <edge> <max_bet>` - Update settings\n" +
                "• `!setodds` - View current settings\n" +
                "• House edge: 0.01 = 1%, 0.05 = 5%", false);

        embed.addField("💡 Recommendations", 
                "• Keep house edge between 2-8% for fairness\n" +
                "• Adjust max bets based on casino funds\n" +
                "• Monitor game profitability regularly", false);

        embed.setFooter("The Syndicate's Den • Game Configuration", event.getJDA().getSelfUser().getAvatarUrl());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private boolean isValidGame(String gameName) {
        return gameName.equals("dice") || gameName.equals("roulette") || 
               gameName.equals("slots") || gameName.equals("blackjack");
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String getImpactDescription(double houseEdge, long maxBet) {
        StringBuilder impact = new StringBuilder();
        
        if (houseEdge <= 0.02) {
            impact.append("🔴 **Very Low House Edge** - High risk, player-favorable\n");
        } else if (houseEdge <= 0.05) {
            impact.append("🟡 **Standard House Edge** - Balanced risk/reward\n");
        } else if (houseEdge <= 0.1) {
            impact.append("🟠 **High House Edge** - Lower risk, casino-favorable\n");
        } else {
            impact.append("🔴 **Very High House Edge** - May discourage players\n");
        }
        
        if (maxBet >= 50000) {
            impact.append("💎 **High Stakes** - Attracts high rollers, higher volatility");
        } else if (maxBet >= 10000) {
            impact.append("💰 **Medium Stakes** - Balanced for most players");
        } else {
            impact.append("🎯 **Low Stakes** - Conservative approach, lower risk");
        }
        
        return impact.toString();
    }

    @Override
    public String getName() {
        return "setodds";
    }

    @Override
    public String getDescription() {
        return "Configure game odds and betting limits (Committee only)";
    }

    @Override
    public String getUsage() {
        return "!setodds [game] [house_edge] [max_bet]";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.MANAGEMENT;
    }

    @Override
    public boolean requiresCommitteeRole() {
        return true;
    }
} 