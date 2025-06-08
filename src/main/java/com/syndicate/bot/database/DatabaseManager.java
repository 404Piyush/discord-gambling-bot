package com.syndicate.bot.database;

import com.syndicate.bot.commands.economy.LeaderboardEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DATABASE_URL = "jdbc:sqlite:syndicate_den.db";
    private Connection connection;

    public void initialize() throws SQLException {
        connection = DriverManager.getConnection(DATABASE_URL);
        createTables();
        logger.info("Database initialized successfully");
    }

    private void createTables() throws SQLException {
        
        String createCasinoTable = """
            CREATE TABLE IF NOT EXISTS casino_bank (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id TEXT NOT NULL,
                total_funds BIGINT DEFAULT 1000000,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(guild_id)
            )
        """;

        
        String createCommitteeTable = """
            CREATE TABLE IF NOT EXISTS committee_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                role TEXT NOT NULL,
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(guild_id, user_id)
            )
        """;

        
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS user_balances (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                balance BIGINT DEFAULT 1000,
                total_winnings BIGINT DEFAULT 0,
                total_losses BIGINT DEFAULT 0,
                games_played INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(guild_id, user_id)
            )
        """;

        
        String createGameConfigTable = """
            CREATE TABLE IF NOT EXISTS game_configurations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id TEXT NOT NULL,
                game_name TEXT NOT NULL,
                house_edge REAL DEFAULT 0.05,
                min_bet BIGINT DEFAULT 1,
                max_bet BIGINT DEFAULT 10000,
                is_active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(guild_id, game_name)
            )
        """;

        
        String createTransactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                transaction_type TEXT NOT NULL,
                amount BIGINT NOT NULL,
                game_name TEXT,
                description TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createCasinoTable);
            stmt.execute(createCommitteeTable);
            stmt.execute(createUsersTable);
            stmt.execute(createGameConfigTable);
            stmt.execute(createTransactionsTable);
        }
    }

    
    public void initializeCasinoBank(String guildId, long initialAmount) throws SQLException {
        String sql = "INSERT OR IGNORE INTO casino_bank (guild_id, total_funds) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setLong(2, initialAmount);
            stmt.executeUpdate();
        }
    }

    public long getCasinoBankBalance(String guildId) throws SQLException {
        String sql = "SELECT total_funds FROM casino_bank WHERE guild_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("total_funds");
            }
            return 0;
        }
    }

    public void updateCasinoBankBalance(String guildId, long newBalance) throws SQLException {
        String sql = "UPDATE casino_bank SET total_funds = ? WHERE guild_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, newBalance);
            stmt.setString(2, guildId);
            stmt.executeUpdate();
        }
    }

    
    public void initializeUserBalance(String guildId, String userId, long initialAmount) throws SQLException {
        String sql = "INSERT OR IGNORE INTO user_balances (guild_id, user_id, balance) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setString(2, userId);
            stmt.setLong(3, initialAmount);
            stmt.executeUpdate();
        }
    }

    public long getUserBalance(String guildId, String userId) throws SQLException {
        String sql = "SELECT balance FROM user_balances WHERE guild_id = ? AND user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("balance");
            }
            return 0;
        }
    }

    public void updateUserBalance(String guildId, String userId, long newBalance) throws SQLException {
        String sql = "UPDATE user_balances SET balance = ? WHERE guild_id = ? AND user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, newBalance);
            stmt.setString(2, guildId);
            stmt.setString(3, userId);
            stmt.executeUpdate();
        }
    }

    
    public void addCommitteeMember(String guildId, String userId, String role) throws SQLException {
        String sql = "INSERT OR REPLACE INTO committee_members (guild_id, user_id, role) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setString(2, userId);
            stmt.setString(3, role);
            stmt.executeUpdate();
        }
    }

    public void removeCommitteeMember(String guildId, String userId) throws SQLException {
        String sql = "DELETE FROM committee_members WHERE guild_id = ? AND user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        }
    }

    public boolean isCommitteeMember(String guildId, String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM committee_members WHERE guild_id = ? AND user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public List<String> getCommitteeMembers(String guildId) throws SQLException {
        String sql = "SELECT user_id FROM committee_members WHERE guild_id = ?";
        List<String> members = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                members.add(rs.getString("user_id"));
            }
        }
        return members;
    }

    
    public void addTransaction(String guildId, String userId, String type, long amount, String gameName, String description) throws SQLException {
        String sql = "INSERT INTO transactions (guild_id, user_id, transaction_type, amount, game_name, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setString(2, userId);
            stmt.setString(3, type);
            stmt.setLong(4, amount);
            stmt.setString(5, gameName);
            stmt.setString(6, description);
            stmt.executeUpdate();
        }
    }

    
    public void setGameConfiguration(String guildId, String gameName, double houseEdge, long minBet, long maxBet, boolean isActive) throws SQLException {
        String sql = "INSERT OR REPLACE INTO game_configurations (guild_id, game_name, house_edge, min_bet, max_bet, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setString(2, gameName);
            stmt.setDouble(3, houseEdge);
            stmt.setLong(4, minBet);
            stmt.setLong(5, maxBet);
            stmt.setBoolean(6, isActive);
            stmt.executeUpdate();
        }
    }

    
    public List<LeaderboardEntry> getTopPlayers(String guildId, int limit) throws SQLException {
        String sql = "SELECT user_id, balance FROM user_balances WHERE guild_id = ? ORDER BY balance DESC LIMIT ?";
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            int rank = 1;
            while (rs.next()) {
                leaderboard.add(new LeaderboardEntry(
                    rs.getString("user_id"),
                    rs.getLong("balance"),
                    rank++
                ));
            }
        }
        return leaderboard;
    }

    public int getUserRank(String guildId, String userId) throws SQLException {
        String sql = "SELECT COUNT(*) + 1 as rank FROM user_balances WHERE guild_id = ? AND balance > (SELECT balance FROM user_balances WHERE guild_id = ? AND user_id = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            stmt.setString(2, guildId);
            stmt.setString(3, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("rank");
            }
            return -1;
        }
    }

    public int getTotalPlayers(String guildId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_balances WHERE guild_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public long getTotalServerWealth(String guildId) throws SQLException {
        String sql = "SELECT SUM(balance) FROM user_balances WHERE guild_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }
} 