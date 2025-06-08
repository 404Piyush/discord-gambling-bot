package com.syndicate.bot.commands.management;

import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.commands.SlashCommand;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ManageSlashCommand implements SlashCommand {
    private final DatabaseManager databaseManager;

    public ManageSlashCommand(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        
        switch (subcommand) {
            case "committee":
                handleCommittee(event);
                break;
            case "bank":
                handleBank(event);
                break;
            case "settings":
                handleSettings(event);
                break;
            default:
                event.reply("❌ Unknown management command!").setEphemeral(true).queue();
        }
    }

    private void handleCommittee(SlashCommandInteractionEvent event) {
        OptionMapping actionOption = event.getOption("action");
        OptionMapping userOption = event.getOption("user");
        
        if (actionOption == null) {
            event.reply("❌ Please specify an action!").setEphemeral(true).queue();
            return;
        }
        
        String action = actionOption.getAsString();
        String guildId = event.getGuild().getId();
        
        try {
            switch (action.toLowerCase()) {
                case "add":
                    if (userOption == null) {
                        event.reply("❌ Please specify a user to add!").setEphemeral(true).queue();
                        return;
                    }
                    User userToAdd = userOption.getAsUser();
                    databaseManager.addCommitteeMember(guildId, userToAdd.getId(), "member");
                    
                    EmbedBuilder addEmbed = new EmbedBuilder()
                            .setTitle("✅ Committee Member Added")
                            .addField("User", userToAdd.getAsMention(), true)
                            .addField("Role", "Member", true)
                            .setColor(Color.GREEN)
                            .setTimestamp(java.time.Instant.now());
                    
                    event.replyEmbeds(addEmbed.build()).queue();
                    break;
                    
                case "remove":
                    if (userOption == null) {
                        event.reply("❌ Please specify a user to remove!").setEphemeral(true).queue();
                        return;
                    }
                    User userToRemove = userOption.getAsUser();
                    databaseManager.removeCommitteeMember(guildId, userToRemove.getId());
                    
                    EmbedBuilder removeEmbed = new EmbedBuilder()
                            .setTitle("✅ Committee Member Removed")
                            .addField("User", userToRemove.getAsMention(), true)
                            .setColor(Color.ORANGE)
                            .setTimestamp(java.time.Instant.now());
                    
                    event.replyEmbeds(removeEmbed.build()).queue();
                    break;
                    
                case "list":
                    List<String> members = databaseManager.getCommitteeMembers(guildId);
                    
                    EmbedBuilder listEmbed = new EmbedBuilder()
                            .setTitle("👥 Committee Members")
                            .setColor(Color.BLUE)
                            .setTimestamp(java.time.Instant.now());
                    
                    if (members.isEmpty()) {
                        listEmbed.setDescription("No committee members found.");
                    } else {
                        StringBuilder memberList = new StringBuilder();
                        for (String memberId : members) {
                            memberList.append("<@").append(memberId).append(">\n");
                        }
                        listEmbed.setDescription(memberList.toString());
                    }
                    
                    event.replyEmbeds(listEmbed.build()).queue();
                    break;
                    
                default:
                    event.reply("❌ Unknown action! Use: add, remove, or list").setEphemeral(true).queue();
            }
            
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleBank(SlashCommandInteractionEvent event) {
        String guildId = event.getGuild().getId();
        
        try {
            
            databaseManager.initializeCasinoBank(guildId, 1000000);
            
            long bankBalance = databaseManager.getCasinoBankBalance(guildId);
            
            
            String healthStatus;
            Color healthColor;
            
            if (bankBalance >= 500000) {
                healthStatus = "🟢 Excellent";
                healthColor = Color.GREEN;
            } else if (bankBalance >= 100000) {
                healthStatus = "🟡 Good";
                healthColor = Color.YELLOW;
            } else if (bankBalance >= 50000) {
                healthStatus = "🟠 Warning";
                healthColor = Color.ORANGE;
            } else {
                healthStatus = "🔴 Critical";
                healthColor = Color.RED;
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🏦 Casino Bank Status")
                    .addField("Current Balance", bankBalance + " credits", true)
                    .addField("Health Status", healthStatus, true)
                    .addField("Recommended Actions", 
                            bankBalance < 100000 ? "Consider adjusting game odds or limits" : "Bank is operating normally", 
                            false)
                    .setColor(healthColor)
                    .setTimestamp(java.time.Instant.now());
            
            event.replyEmbeds(embed.build()).queue();
            
        } catch (SQLException e) {
            event.reply("❌ Database error occurred!").setEphemeral(true).queue();
        }
    }

    private void handleSettings(SlashCommandInteractionEvent event) {
        event.reply("⚙️ Settings management coming soon!").setEphemeral(true).queue();
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("manage", "Admin commands")
                .addSubcommands(
                        new SubcommandData("committee", "Manage admin users")
                                .addOption(OptionType.STRING, "action", "What to do (add/remove/list)", true)
                                .addOption(OptionType.USER, "user", "Which user", false),
                        new SubcommandData("bank", "Check casino money"),
                        new SubcommandData("settings", "Change settings")
                );
    }

    @Override
    public String getName() {
        return "manage";
    }

    @Override
    public String getDescription() {
        return "Admin commands";
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