package com.syndicate.bot.commands.economy;

public class LeaderboardEntry {
    private final String userId;
    private final long balance;
    private final int rank;
    
    public LeaderboardEntry(String userId, long balance, int rank) {
        this.userId = userId;
        this.balance = balance;
        this.rank = rank;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public long getBalance() {
        return balance;
    }
    
    public int getRank() {
        return rank;
    }
} 