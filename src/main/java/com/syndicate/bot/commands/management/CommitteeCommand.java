package com.syndicate.bot.commands.management;

import com.syndicate.bot.SyndicateBot;
import com.syndicate.bot.commands.Command;
import com.syndicate.bot.commands.CommandCategory;
import com.syndicate.bot.database.DatabaseManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class CommitteeCommand implements Command {

    @Override
    public void execute(MessageReceivedEvent event, String[] args) {
        
        boolean hasPermission = event.getMember().hasPermission(Permission.ADMINISTRATOR);
        
        try {
            DatabaseManager db = SyndicateBot.getDatabaseManager();
            String guildId = event.getGuild().getId();
            String userId = event.getAuthor().getId();
            
            if (!hasPermission && !db.isCommitteeMember(guildId, userId)) {
                event.getChannel().sendMessage("❌ You need Administrator permissions or committee membership to use this command!").queue();
                return;
            }

            if (args.length == 0) {
                
                showCommitteeMembers(event, db, guildId);
                return;
            }

            String action = args[0].toLowerCase();

            switch (action) {
                case "add":
                    if (args.length < 2 || event.getMessage().getMentions().getUsers().isEmpty()) {
                        event.getChannel().sendMessage("❌ Usage: `!committee add @user`").queue();
                        return;
                    }
                    addCommitteeMember(event, db, guildId);
                    break;

                case "remove":
                    if (args.length < 2 || event.getMessage().getMentions().getUsers().isEmpty()) {
                        event.getChannel().sendMessage("❌ Usage: `!committee remove @user`").queue();
                        return;
                    }
                    removeCommitteeMember(event, db, guildId);
                    break;

                case "list":
                    showCommitteeMembers(event, db, guildId);
                    break;

                default:
                    event.getChannel().sendMessage("❌ Valid actions: `add`, `remove`, `list`\n" +
                            "Usage: `!committee <action> [@user]`").queue();
                    break;
            }

        } catch (SQLException e) {
            event.getChannel().sendMessage("❌ Database error occurred. Please try again.").queue();
        }
    }

    private void addCommitteeMember(MessageReceivedEvent event, DatabaseManager db, String guildId) throws SQLException {
        User targetUser = event.getMessage().getMentions().getUsers().get(0);
        
        if (targetUser.isBot()) {
            event.getChannel().sendMessage("❌ Cannot add bots to the committee!").queue();
            return;
        }

        if (db.isCommitteeMember(guildId, targetUser.getId())) {
            event.getChannel().sendMessage("❌ " + targetUser.getAsMention() + " is already a committee member!").queue();
            return;
        }

        db.addCommitteeMember(guildId, targetUser.getId(), "member");

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("✅ Committee Member Added");
        embed.setColor(Color.GREEN);
        embed.addField("👤 New Member", targetUser.getAsMention(), true);
        embed.addField("🎯 Role", "Committee Member", true);
        embed.addField("🔑 Permissions", "• View bank status\n• Adjust game settings\n• Manage committee", false);
        embed.setFooter("The Syndicate's Den • Committee Management", event.getJDA().getSelfUser().getAvatarUrl());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void removeCommitteeMember(MessageReceivedEvent event, DatabaseManager db, String guildId) throws SQLException {
        User targetUser = event.getMessage().getMentions().getUsers().get(0);

        if (!db.isCommitteeMember(guildId, targetUser.getId())) {
            event.getChannel().sendMessage("❌ " + targetUser.getAsMention() + " is not a committee member!").queue();
            return;
        }

        db.removeCommitteeMember(guildId, targetUser.getId());

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("❌ Committee Member Removed");
        embed.setColor(Color.RED);
        embed.addField("👤 Removed Member", targetUser.getAsMention(), true);
        embed.addField("🔒 Access", "Committee access revoked", true);
        embed.setFooter("The Syndicate's Den • Committee Management", event.getJDA().getSelfUser().getAvatarUrl());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void showCommitteeMembers(MessageReceivedEvent event, DatabaseManager db, String guildId) throws SQLException {
        List<String> memberIds = db.getCommitteeMembers(guildId);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("👥 Committee Members");
        embed.setColor(Color.CYAN);
        embed.setDescription("Current members of The Syndicate's Den Committee");

        if (memberIds.isEmpty()) {
            embed.addField("📋 Members", "No committee members found.\nUse `!committee add @user` to add members.", false);
        } else {
            StringBuilder memberList = new StringBuilder();
            for (String memberId : memberIds) {
                User user = event.getJDA().getUserById(memberId);
                String displayName = user != null ? user.getAsMention() : "Unknown User (ID: " + memberId + ")";
                memberList.append("• ").append(displayName).append("\n");
            }
            embed.addField("📋 Members (" + memberIds.size() + ")", memberList.toString(), false);
        }

        embed.addField("⚙️ Management", 
                "• `!committee add @user` - Add member\n" +
                "• `!committee remove @user` - Remove member\n" +
                "• `!committee list` - Show members", false);

        embed.setFooter("The Syndicate's Den • Committee Management", event.getJDA().getSelfUser().getAvatarUrl());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    @Override
    public String getName() {
        return "committee";
    }

    @Override
    public String getDescription() {
        return "Manage committee members who can control casino operations";
    }

    @Override
    public String getUsage() {
        return "!committee [add/remove/list] [@user]";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.COMMITTEE;
    }

    @Override
    public boolean requiresCommitteeRole() {
        return false; 
    }
} 