package com.syndicate.bot.commands;

public enum CommandCategory {
    GAMBLING("🎰 Gambling"),
    MANAGEMENT("⚙️ Management"),
    ECONOMY("💰 Economy"),
    COMMITTEE("👥 Committee"),
    INFORMATION("ℹ️ Information");

    private final String displayName;

    CommandCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 