package com.globalisosecurity.backend;

/**
 * Test-only JVM used to verify that the diagnostic watchdog signals the JVM
 * process itself when startup remains blocked.
 */
public final class StartupDiagnosticProbe {

    private StartupDiagnosticProbe() {
    }

    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(6_000L);
    }
}
