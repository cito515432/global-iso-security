package com.globalisosecurity.backend.services;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Streams evidence to a private clamd service using ClamAV's INSTREAM protocol. */
@Component
public class ClamAvScanner {

    private final boolean required;
    private final String host;
    private final int port;
    private final int timeoutMs;

    public ClamAvScanner(
            @Value("${app.security.upload.scan-required:true}") boolean required,
            @Value("${app.security.upload.clamav.host:}") String host,
            @Value("${app.security.upload.clamav.port:3310}") int port,
            @Value("${app.security.upload.clamav.timeout-ms:10000}") int timeoutMs) {
        this.required = required;
        this.host = host == null ? "" : host.trim();
        this.port = Math.max(1, Math.min(65535, port));
        this.timeoutMs = Math.max(1000, Math.min(30000, timeoutMs));
    }

    public void scan(Path file) {
        if (!required) return;
        if (host.isBlank()) {
            throw unavailable();
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    InputStream input = new BufferedInputStream(socket.getInputStream());
                    InputStream fileInput = Files.newInputStream(file)) {
                output.write(new byte[]{'z', 'I', 'N', 'S', 'T', 'R', 'E', 'A', 'M', 0});
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fileInput.read(buffer)) != -1) {
                    output.writeInt(read);
                    output.write(buffer, 0, read);
                }
                output.writeInt(0);
                output.flush();

                String response = readResponse(input);
                if (response.contains("FOUND")) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "El archivo fue rechazado por el análisis de seguridad");
                }
                if (!response.endsWith("OK")) throw new IllegalStateException("Scanner returned an unknown result");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            // Fail closed only for evidence uploads. The application and login remain available.
            throw unavailable();
        }
    }

    private static String readResponse(InputStream input) throws Exception {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1 && value != 0 && value != '\n') {
            if (response.size() >= 512) throw new IllegalStateException("Scanner response too long");
            response.write(value);
        }
        if (value == -1 || response.size() == 0) throw new IllegalStateException("Scanner response missing");
        return response.toString(java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El análisis de seguridad no está disponible; el archivo no se almacenó");
    }
}
