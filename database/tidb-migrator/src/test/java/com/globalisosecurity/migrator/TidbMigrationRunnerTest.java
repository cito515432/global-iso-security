package com.globalisosecurity.migrator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TidbMigrationRunnerTest {
    @Test
    void acceptsTiDbCloudUrlWithIdentityVerifiedTls() {
        assertDoesNotThrow(() -> TidbMigrationRunner.validateUrl(
                "jdbc:mysql://gateway01.us-east-1.prod.aws.tidbcloud.com:4000/globalisosecurity?sslMode=VERIFY_IDENTITY"));
    }

    @Test
    void rejectsUrlWithoutIdentityVerifiedTls() {
        assertThrows(IllegalArgumentException.class, () -> TidbMigrationRunner.validateUrl(
                "jdbc:mysql://gateway01.us-east-1.prod.aws.tidbcloud.com:4000/globalisosecurity?useSSL=true"));
    }

    @Test
    void rejectsHostsOutsideTiDbCloudSuffix() {
        assertThrows(IllegalArgumentException.class, () -> TidbMigrationRunner.validateUrl(
                "jdbc:mysql://localhost:4000/globalisosecurity?sslMode=VERIFY_IDENTITY"));
    }
}
