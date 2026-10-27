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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One-shot migration entry point. Read-only "info" is the default; writes require
 * both an explicit command and independent backup/target confirmations.
 */
public final class TidbMigrationRunner {
    private static final String EXPECTED_DATABASE = "globalisosecurity";
    private static final String TARGET_VERSION = "4";
    private static final String MFA_VERSION = "1";
    private static final String SECURITY_VERSION = "2";
    private static final String SESSION_VERSION = "3";
    private static final String MULTI_ROLE_VERSION = "4";
    private static final Set<String> MFA_TABLES = Set.of(
            "usuario_mfa", "mfa_login_challenges", "mfa_recovery_codes");
    private static final Set<String> SECURITY_TABLES = Set.of(
            "login_attempt_buckets", "password_reset_tokens");
    private static final Set<String> SESSION_TABLES = Set.of("auth_sessions");
    private static final Set<String> MULTI_ROLE_TABLES = Set.of("usuario_roles");

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
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .cleanDisabled(true)
                .target(MigrationVersion.fromVersion(TARGET_VERSION))
                .load();

        MigrationInfoService info = flyway.info();
        verifyKnownHistory(info);
        List<MigrationInfo> pending = Arrays.stream(info.pending()).toList();
        Set<String> pendingVersions = pending.stream().filter(m -> m.getVersion() != null)
                .map(m -> m.getVersion().getVersion()).collect(Collectors.toSet());
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            verifyDatabase(connection);
            verifyMigrationState(connection, mode, pendingVersions);
        }
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

        if (pending.isEmpty()) {
            System.out.println("No pending migrations; no database changes made.");
            return;
        }

        requireApplyGate(pendingVersions);
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            verifyDatabase(connection);
            verifyMigrationState(connection, "migrate", pendingVersions);
        }
        var result = flyway.migrate();
        System.out.printf("Migration finished: applied=%d, target=%s.%n",
                result.migrationsExecuted, TARGET_VERSION);
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
                    !Set.of(MFA_VERSION, SECURITY_VERSION, SESSION_VERSION, MULTI_ROLE_VERSION)
                            .contains(migration.getVersion().getVersion());
        });
        if (unexpected) {
            throw new IllegalStateException("Unexpected Flyway history detected; inspect the database migration history manually.");
        }
    }

    private static void verifyMigrationState(Connection connection, String mode, Set<String> pendingVersions) throws Exception {
        Set<String> existingTables = new java.util.HashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE()")) {
            while (rows.next()) existingTables.add(rows.getString(1).toLowerCase(Locale.ROOT));
        }
        verifyTableGroup(existingTables, MFA_TABLES, MFA_VERSION, mode, pendingVersions, "MFA");
        verifyTableGroup(existingTables, SECURITY_TABLES, SECURITY_VERSION, mode, pendingVersions, "security controls");
        verifyTableGroup(existingTables, SESSION_TABLES, SESSION_VERSION, mode, pendingVersions, "idle session");
        verifyTableGroup(existingTables, MULTI_ROLE_TABLES, MULTI_ROLE_VERSION, mode, pendingVersions, "multi-role");
    }

    private static void verifyTableGroup(Set<String> existing, Set<String> expected, String version,
            String mode, Set<String> pendingVersions, String label) {
        long count = expected.stream().filter(existing::contains).count();
        if (count > 0 && count < expected.size()) {
            throw new IllegalStateException("Partial " + label + " schema detected; inspect it manually before retrying.");
        }
        if ("migrate".equals(mode) && pendingVersions.contains(version) && count > 0) {
            throw new IllegalStateException(label + " tables already exist while migration " + version
                    + " is pending; inspect Flyway history and schema manually before retrying.");
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

    private static void requireApplyGate(Set<String> pendingVersions) {
        if (pendingVersions.contains(MFA_VERSION)
                && !"APPLY_MFA_V1".equals(System.getenv("TIDB_MIGRATION_CONFIRM"))) {
            throw new IllegalStateException("Set TIDB_MIGRATION_CONFIRM=APPLY_MFA_V1 to authorize the MFA migration.");
        }
        if (pendingVersions.contains(SECURITY_VERSION)
                && !"APPLY_SECURITY_V2".equals(System.getenv("TIDB_SECURITY_MIGRATION_CONFIRM"))) {
            throw new IllegalStateException("Set TIDB_SECURITY_MIGRATION_CONFIRM=APPLY_SECURITY_V2 to authorize security migration V2.");
        }
        if (pendingVersions.contains(SESSION_VERSION)
                && !"APPLY_AUTH_SESSION_V3".equals(System.getenv("TIDB_SESSION_MIGRATION_CONFIRM"))) {
            throw new IllegalStateException("Set TIDB_SESSION_MIGRATION_CONFIRM=APPLY_AUTH_SESSION_V3 to authorize migration V3.");
        }
        if (pendingVersions.contains(MULTI_ROLE_VERSION)
                && !"APPLY_MULTI_ROLE_V4".equals(System.getenv("TIDB_MULTI_ROLE_MIGRATION_CONFIRM"))) {
            throw new IllegalStateException("Set TIDB_MULTI_ROLE_MIGRATION_CONFIRM=APPLY_MULTI_ROLE_V4 to authorize migration V4.");
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
