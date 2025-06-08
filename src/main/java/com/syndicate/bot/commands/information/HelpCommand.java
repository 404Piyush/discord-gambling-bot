package com.syndicate.bot.commands.information;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.Command;
import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.commands.CommandManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.Map;

public class HelpCommand implements Command {

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        CommandManager commandManager = SyndicateBot.getCommandManager();
        
        if (args.length > 0) {
            
            String commandName = args[0].toLowerCase();
            Command command = commandManager.getCommand(commandName);
            
            if (command != null) {
                showSpecificCommandHelp(event, command);
            } else {
                event.getChannel().sendMessage("❌ Command `" + commandName + "` not found!").queue();
            }
            return;
        }

        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🎰 The Syndicate's Den - Help");
        embed.setColor(Color.MAGENTA);
        embed.setDescription("Welcome to The Syndicate's Den! A committee-managed virtual casino.");

        
        Map<String, Command> allCommands = commandManager.getAllCommands();
        
        StringBuilder gamblingCommands = new StringBuilder();
        StringBuilder economyCommands = new StringBuilder();
        StringBuilder managementCommands = new StringBuilder();
        StringBuilder committeeCommands = new StringBuilder();
        StringBuilder infoCommands = new StringBuilder();

        for (Command command : allCommands.values()) {
            String commandLine = "`!" + command.getName() + "` - " + command.getDescription() + "\n";
            
            switch (command.getCategory()) {
                case GAMBLING:
                    gamblingCommands.append(commandLine);
                    break;
                case ECONOMY:
                    economyCommands.append(commandLine);
                    break;
                case MANAGEMENT:
                    managementCommands.append(commandLine);
                    break;
                case COMMITTEE:
                    committeeCommands.append(commandLine);
                    break;
                case INFORMATION:
                    infoCommands.append(commandLine);
                    break;
            }
        }

        
        if (gamblingCommands.length() > 0) {
            embed.addField("🎲 " + CommandCategory.GAMBLING.getDisplayName(), gamblingCommands.toString(), false);
        }
        
        if (economyCommands.length() > 0) {
            embed.addField("💰 " + CommandCategory.ECONOMY.getDisplayName(), economyCommands.toString(), false);
        }
        
        if (managementCommands.length() > 0) {
            embed.addField("⚙️ " + CommandCategory.MANAGEMENT.getDisplayName() + " (Committee Only)", managementCommands.toString(), false);
        }
        
        if (committeeCommands.length() > 0) {
            embed.addField("👥 " + CommandCategory.COMMITTEE.getDisplayName(), committeeCommands.toString(), false);
        }
        
        if (infoCommands.length() > 0) {
            embed.addField("ℹ️ " + CommandCategory.INFORMATION.getDisplayName(), infoCommands.toString(), false);
        }

        embed.addField("📖 Getting Started", 
                "1️⃣ Check your balance: `!balance`\n" +
                "2️⃣ Try a game: `!dice 100 high`\n" +
                "3️⃣ View leaderboard: `!leaderboard`\n" +
                "4️⃣ Get detailed help: `!help <command>`", false);

        embed.addField("🎯 Quick Examples", 
                "• `!dice 50 lucky7` - Bet 50 on lucky 7\n" +
                "• `!roulette 100 red` - Bet 100 on red\n" +
                "• `!slots 25` - Play slots with 25 credits\n" +
                "• `!pay @user 500` - Transfer 500 credits", false);

        embed.addField("🏛️ Committee Features", 
                "Committee members can manage the casino using commands like `!bankstatus`, `!setodds`, and `!committee`.\n" +
                "Ask an admin to add you to the committee!", false);

        embed.setFooter("The Syndicate's Den • Use !help <command> for detailed info", event.getJDA().getSelfUser().getAvatarUrl());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void showSpecificCommandHelp(MessageReceivedEvent event, Command command) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📚 Command Help: !" + command.getName());
        embed.setColor(Color.CYAN);

        embed.addField("📝 Description", command.getDescription(), false);
        embed.addField("💻 Usage", command.getUsage(), false);
        embed.addField("📂 Category", command.getCategory().getDisplayName(), true);
        embed.addField("🔐 Access", command.requiresCommitteeRole() ? "Committee Only" : "Everyone", true);

        
        String examples = getCommandExamples(command.getName());
        if (!examples.isEmpty()) {
            embed.addField("💡 Examples", examples, false);
        }

        embed.setFooter("The Syndicate's Den • Command Help", event.getJDA().getSelfUser().getAvatarUrl());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private String getCommandExamples(String commandName) {
        return switch (commandName.toLowerCase()) {
            case "dice" -> "• `!dice 100 high` - Bet 100 on high (8-12)\n" +
                          "• `!dice 50 low` - Bet 50 on low (2-6)\n" +
                          "• `!dice 25 7` - Bet 25 on exactly 7";
            case "roulette" -> "• `!roulette 100 red` - Bet 100 on red\n" +
                              "• `!roulette 50 odd` - Bet 50 on odd numbers\n" +
                              "• `!roulette 25 17` - Bet 25 on number 17";
            case "slots" -> "• `!slots 50` - Play slots with 50 credits\n" +
                           "• `!slots 100` - Higher bet for bigger potential wins";
            case "blackjack" -> "• `!blackjack 100` - Play blackjack with 100 credits\n" +
                               "• `!blackjack 250` - Higher stakes blackjack";
            case "pay" -> "• `!pay @user 500` - Send 500 credits to user\n" +
                         "• `!pay @friend 1000` - Transfer 1000 credits";
            case "committee" -> "• `!committee list` - Show committee members\n" +
                               "• `!committee add @user` - Add committee member\n" +
                               "• `!committee remove @user` - Remove member";
            case "setodds" -> "• `!setodds dice 0.05 5000` - Set dice house edge to 5%, max bet 5000\n" +
                             "• `!setodds roulette 0.03 10000` - Lower house edge, higher max bet";
            default -> "";
        };
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Show help information for all commands or a specific command";
    }

    @Override
    public String getUsage() {
        return "!help [command_name]";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.INFORMATION;
    }

    @Override
    public boolean requiresCommitteeRole() {
        return false;
    }
} 