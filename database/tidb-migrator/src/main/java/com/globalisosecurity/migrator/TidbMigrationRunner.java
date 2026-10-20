package com.globalisosecurity.migrator;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationVersion;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One-shot migration entry point. Read-only "info" is the default; writes require
 * both an explicit command and independent backup/target confirmations.
 */
public final class TidbMigrationRunner {
    private static final String EXPECTED_DATABASE = "globalisosecurity";
    private static final String MIGRATION_VERSION = "1";
    private static final Set<String> MFA_TABLES = Set.of(
            "usuario_mfa", "mfa_login_challenges", "mfa_recovery_codes");

    private TidbMigrationRunner() {}

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception exception) {
            // Do not print connection URLs, SQL parameters, or credential-bearing exceptions.
            System.err.println("Migration runner stopped: " + safeMessage(exception));
            System.exit(1);
        }
    }

    static void run(String[] args) throws Exception {
        String mode = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        if (!Set.of("info", "validate", "migrate").contains(mode)) {
            throw new IllegalArgumentException("Use one mode: info, validate, or migrate.");
        }

        String url = requiredEnv("TIDB_JDBC_URL");
        String username = requiredEnv("TIDB_USERNAME");
        String password = requiredEnv("TIDB_PASSWORD");
        validateUrl(url);

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            verifyDatabase(connection);
            verifyMigrationState(connection, "info");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .cleanDisabled(true)
                .target(MigrationVersion.fromVersion(MIGRATION_VERSION))
                .load();

        MigrationInfoService info = flyway.info();
        verifyKnownHistory(info);
        List<MigrationInfo> pending = Arrays.stream(info.pending()).toList();
        System.out.printf("TiDB migration mode=%s; applied=%d; pending=%d.%n",
                mode, info.applied().length, pending.size());
        for (MigrationInfo migration : pending) {
            System.out.printf("Pending version=%s description=%s type=%s.%n",
                    migration.getVersion(), migration.getDescription(), migration.getType());
        }

        if ("info".equals(mode)) {
            return;
        }
        if ("validate".equals(mode)) {
            if (info.applied().length > 0) {
                flyway.validate();
            } else {
                System.out.println("No Flyway history yet; validation has no applied checksum to compare.");
            }
            System.out.println("Validation completed.");
            return;
        }

        if (pending.stream().noneMatch(m -> MIGRATION_VERSION.equals(m.getVersion().getVersion()))) {
            System.out.println("No pending version-1 migration; no database changes made.");
            return;
        }

        requireApplyGate();
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            verifyDatabase(connection);
            verifyMigrationState(connection, "migrate");
        }
        var result = flyway.migrate();
        System.out.printf("Migration finished: applied=%d, target=%s.%n",
                result.migrationsExecuted, MIGRATION_VERSION);
    }

    private static void verifyDatabase(Connection connection) throws Exception {
        String actual = connection.getCatalog();
        if (actual == null || !EXPECTED_DATABASE.equalsIgnoreCase(actual)) {
            throw new IllegalStateException("Connected schema does not match the expected application database.");
        }
    }

    private static void verifyKnownHistory(MigrationInfoService info) {
        boolean unexpected = Arrays.stream(info.applied()).anyMatch(migration -> {
            String type = migration.getType() == null ? "" : migration.getType().toString();
            if ("BASELINE".equalsIgnoreCase(type)) {
                return false;
            }
            return migration.getVersion() == null ||
                    !MIGRATION_VERSION.equals(migration.getVersion().getVersion());
        });
        if (unexpected) {
            throw new IllegalStateException("Unexpected Flyway history detected; inspect the database migration history manually.");
        }
    }

    private static void verifyMigrationState(Connection connection, String mode) throws Exception {
        Map<String, Boolean> present = MFA_TABLES.stream()
                .collect(Collectors.toMap(name -> name, name -> false));
        String sql = "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name IN " +
                "('usuario_mfa','mfa_login_challenges','mfa_recovery_codes')";
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                String table = rows.getString(1).toLowerCase(Locale.ROOT);
                if (present.containsKey(table)) {
                    present.put(table, true);
                }
            }
        }

        long existing = present.values().stream().filter(Boolean::booleanValue).count();
        if ("migrate".equals(mode) && existing != 0) {
            throw new IllegalStateException("MFA tables already exist or are partial; inspect schema history manually before retrying.");
        }
        if (existing > 0 && existing < MFA_TABLES.size()) {
            throw new IllegalStateException("Partial MFA schema detected; no automatic repair was attempted.");
        }
    }

    static void validateUrl(String url) {
        if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("TIDB_JDBC_URL must use the MySQL-compatible TiDB JDBC scheme.");
        }
        URI uri;
        try {
            uri = URI.create(url.substring("jdbc:mysql:".length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("TIDB_JDBC_URL is malformed.");
        }
        String host = uri.getHost();
        String suffix = System.getenv().getOrDefault("TIDB_ALLOWED_HOST_SUFFIX", "tidbcloud.com")
                .toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
        if (host == null || !host.toLowerCase(Locale.ROOT).endsWith(suffix)) {
            throw new IllegalArgumentException("Target host is not within the configured TiDB host suffix.");
        }
        String query = uri.getRawQuery();
        if (query == null || Arrays.stream(query.split("&"))
                .map(TidbMigrationRunner::decodeQueryPart)
                .noneMatch(part -> part.equalsIgnoreCase("sslMode=VERIFY_IDENTITY"))) {
            throw new IllegalArgumentException("The JDBC URL must enforce sslMode=VERIFY_IDENTITY.");
        }
    }

    private static String decodeQueryPart(String pair) {
        return java.net.URLDecoder.decode(pair, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void requireApplyGate() {
        if (!"APPLY_MFA_V1".equals(System.getenv("TIDB_MIGRATION_CONFIRM"))) {
            throw new IllegalStateException("Set TIDB_MIGRATION_CONFIRM=APPLY_MFA_V1 to authorize schema writes.");
        }
        if (!"true".equalsIgnoreCase(System.getenv("TIDB_BACKUP_VERIFIED")) ||
                blank(System.getenv("TIDB_BACKUP_REFERENCE"))) {
            throw new IllegalStateException("A verified, restorable backup and its reference are required before writes.");
        }
        if (!"globalisosecurity".equalsIgnoreCase(System.getenv("TIDB_EXPECTED_DATABASE"))) {
            throw new IllegalStateException("TIDB_EXPECTED_DATABASE must explicitly confirm globalisosecurity.");
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (blank(value)) {
            throw new IllegalStateException("Required environment variable is missing: " + name);
        }
        return value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String safeMessage(Exception exception) {
        if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
            return exception.getMessage();
        }
        return "connection or migration operation failed; inspect the protected runtime logs.";
    }
}
