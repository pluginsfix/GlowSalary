package pluginsfix.glowsalary.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import pluginsfix.glowsalary.domain.SalaryProfile;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class SqliteSalaryRepository implements SalaryRepository {

    private final HikariDataSource dataSource;
    private final Executor ioExecutor;
    private final Logger logger;

    private static final String INIT_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS salary_profiles (
            player_uuid TEXT PRIMARY KEY,
            money_streak INTEGER NOT NULL DEFAULT 0,
            sapphire_streak INTEGER NOT NULL DEFAULT 0,
            last_claim_epoch INTEGER NOT NULL DEFAULT 0,
            total_money REAL NOT NULL DEFAULT 0.0,
            total_sapphires INTEGER NOT NULL DEFAULT 0
        );
        """;

    private static final String SELECT_PROFILE_SQL = """
        SELECT money_streak, sapphire_streak, last_claim_epoch, total_money, total_sapphires
        FROM salary_profiles
        WHERE player_uuid = ?;
        """;

    private static final String UPSERT_PROFILE_SQL = """
        INSERT INTO salary_profiles (player_uuid, money_streak, sapphire_streak, last_claim_epoch, total_money, total_sapphires)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(player_uuid) DO UPDATE SET
            money_streak = excluded.money_streak,
            sapphire_streak = excluded.sapphire_streak,
            last_claim_epoch = excluded.last_claim_epoch,
            total_money = excluded.total_money,
            total_sapphires = excluded.total_sapphires;
        """;

    private static final String RESET_PROFILE_SQL = """
        DELETE FROM salary_profiles WHERE player_uuid = ?;
        """;

    public SqliteSalaryRepository(Path databasePath, Logger logger) {
        this.logger = logger;
        this.ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

        HikariConfig config = new HikariConfig();
        config.setPoolName("GlowSalary-SQLite-Pool");
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + databasePath.toAbsolutePath());
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(600000);

        this.dataSource = new HikariDataSource(config);
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
            stmt.execute(INIT_TABLE_SQL);
        } catch (SQLException e) {
            logger.error("Failed to initialize SQLite database", e);
            throw new IllegalStateException("Could not initialize GlowSalary database", e);
        }
    }

    @Override
    public CompletableFuture<SalaryProfile> loadProfile(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SELECT_PROFILE_SQL)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new SalaryProfile(
                            playerId,
                            rs.getInt("money_streak"),
                            rs.getInt("sapphire_streak"),
                            rs.getLong("last_claim_epoch"),
                            rs.getDouble("total_money"),
                            rs.getInt("total_sapphires")
                        );
                    }
                }
            } catch (SQLException e) {
                logger.warn("Failed to load salary profile for player={}", playerId, e);
            }
            return SalaryProfile.initial(playerId);
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> saveProfile(SalaryProfile profile) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(UPSERT_PROFILE_SQL)) {
                stmt.setString(1, profile.playerId().toString());
                stmt.setInt(2, profile.moneyStreak());
                stmt.setInt(3, profile.sapphireStreak());
                stmt.setLong(4, profile.lastClaimEpochSeconds());
                stmt.setDouble(5, profile.totalMoneyClaimed());
                stmt.setInt(6, profile.totalSapphiresClaimed());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.warn("Failed to save salary profile for player={}", profile.playerId(), e);
            }
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> saveAll(Collection<SalaryProfile> profiles) {
        if (profiles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement stmt = conn.prepareStatement(UPSERT_PROFILE_SQL)) {
                    for (SalaryProfile profile : profiles) {
                        stmt.setString(1, profile.playerId().toString());
                        stmt.setInt(2, profile.moneyStreak());
                        stmt.setInt(3, profile.sapphireStreak());
                        stmt.setLong(4, profile.lastClaimEpochSeconds());
                        stmt.setDouble(5, profile.totalMoneyClaimed());
                        stmt.setInt(6, profile.totalSapphiresClaimed());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                logger.warn("Failed to execute batch save for {} salary profiles", profiles.size(), e);
            }
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> resetProfile(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(RESET_PROFILE_SQL)) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.warn("Failed to reset salary profile for player={}", playerId, e);
            }
        }, ioExecutor);
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
