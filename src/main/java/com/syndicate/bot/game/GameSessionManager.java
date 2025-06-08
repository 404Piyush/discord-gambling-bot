package com.syndicate.bot.game;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GameSessionManager {
    private static final GameSessionManager INSTANCE = new GameSessionManager();
    private final ConcurrentMap<String, GameSession> activeSessions = new ConcurrentHashMap<>();

    private GameSessionManager() {}

    public static GameSessionManager getInstance() {
        return INSTANCE;
    }

    public boolean startSession(GameSession session) {
        String key = session.getGuildId() + ":" + session.getUserId();
        GameSession existing = activeSessions.get(key);
        
        if (existing != null && existing.isActive()) {
            return false; 
        }
        
        activeSessions.put(key, session);
        return true;
    }

    public void endSession(String guildId, String userId) {
        String key = guildId + ":" + userId;
        GameSession session = activeSessions.remove(key);
        if (session != null) {
            session.end();
        }
    }

    public GameSession getSession(String guildId, String userId) {
        String key = guildId + ":" + userId;
        return activeSessions.get(key);
    }

    public boolean hasActiveSession(String guildId, String userId) {
        String key = guildId + ":" + userId;
        GameSession session = activeSessions.get(key);
        return session != null && session.isActive();
    }

    
    public String getActiveSessionInfo(String guildId, String userId) {
        String key = guildId + ":" + userId;
        GameSession session = activeSessions.get(key);
        
        if (session == null || !session.isActive()) {
            return null;
        }
        
        String gameType = getGameTypeName(session);
        long timeRunning = (Instant.now().toEpochMilli() - session.getStartTime()) / 1000;
        
        return String.format("**🎮 Active Game Found!**\n" +
            "🎯 **Game Type**: %s\n" +
            "💰 **Bet Amount**: $%d\n" +
            "⏰ **Duration**: %d seconds\n" +
            "📍 **Status**: In Progress\n\n" +
            "💡 *Use the same command to resume your game!*", 
            gameType, session.getBet(), timeRunning);
    }

    
    public void resumeOrRestartSession(GameSession newSession) {
        String key = newSession.getGuildId() + ":" + newSession.getUserId();
        GameSession existing = activeSessions.get(key);
        
        if (existing != null) {
            existing.end(); 
        }
        
        activeSessions.put(key, newSession);
    }

    private String getGameTypeName(GameSession session) {
        String className = session.getClass().getSimpleName();
        switch (className) {
            case "BlackjackSession": return "♠️ Blackjack";
            case "RouletteSession": return "🎰 Roulette";
            case "SlotsSession": return "🎰 Slots";
            case "DiceSession": return "🎲 Dice";
            case "RussianRouletteSession": return "💀 Russian Roulette";
            case "CoinFlipSession": return "🪙 Coin Flip";
            default: return "🎮 Unknown Game";
        }
    }

    public void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> !entry.getValue().isActive());
    }

    
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    
    public long getActiveSessionsForGuild(String guildId) {
        return activeSessions.keySet().stream()
            .filter(key -> key.startsWith(guildId + ":"))
            .count();
    }
} 