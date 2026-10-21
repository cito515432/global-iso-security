package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ClamAvScannerTest {

    @Test
    void acceptsFileOnlyWhenScannerReturnsOk() throws Exception {
        runFakeClamAv("stream: OK\0", (scanner, file) -> scanner.scan(file));
    }

    @Test
    void rejectsInfectedFile() throws Exception {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> runFakeClamAv("stream: Eicar-Test-Signature FOUND\0", (scanner, file) -> scanner.scan(file)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }

    @Test
    void failsClosedWhenRequiredScannerHasNoHost() {
        ClamAvScanner scanner = new ClamAvScanner(true, "", 3310, 1000);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> scanner.scan(Path.of("missing-file")));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    private static void runFakeClamAv(String result, ThrowingRunnable assertion) throws Exception {
        Path file = Files.createTempFile("gis-scan-test", ".pdf");
        Files.writeString(file, "%PDF-1.7 scanner test");
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        try (ServerSocket listener = new ServerSocket(0)) {
            Thread server = new Thread(() -> {
                try (Socket client = listener.accept()) {
                    DataInputStream input = new DataInputStream(client.getInputStream());
                    DataOutputStream output = new DataOutputStream(client.getOutputStream());
                    byte[] command = input.readNBytes(10);
                    if (command.length != 10) throw new IllegalStateException("INSTREAM command was incomplete");
                    int chunk;
                    while ((chunk = input.readInt()) > 0) input.skipNBytes(chunk);
                    output.write(result.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    output.flush();
                } catch (Throwable failure) {
                    serverFailure.set(failure);
                }
            });
            server.start();
            assertion.run(new ClamAvScanner(true, "127.0.0.1", listener.getLocalPort(), 2000), file);
            server.join(2500);
            if (serverFailure.get() != null) throw new AssertionError(serverFailure.get());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run(ClamAvScanner scanner, Path file) throws Exception;
    }
}
