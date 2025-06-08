package com.syndicate.bot.game;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class GameSession {
    protected final String userId;
    protected final String guildId;
    protected final long bet;
    protected final long startTime;
    protected boolean isActive;
    protected CompletableFuture<Void> timeoutFuture;

    public GameSession(String userId, String guildId, long bet) {
        this.userId = userId;
        this.guildId = guildId;
        this.bet = bet;
        this.startTime = System.currentTimeMillis();
        this.isActive = true;
        
        
        this.timeoutFuture = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(5));
                if (isActive) {
                    timeout();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public abstract void timeout();
    
    public void end() {
        isActive = false;
        if (timeoutFuture != null) {
            timeoutFuture.cancel(true);
        }
    }

    public String getUserId() { return userId; }
    public String getGuildId() { return guildId; }
    public long getBet() { return bet; }
    public boolean isActive() { return isActive; }
    public long getStartTime() { return startTime; }
} 