package com.globalisosecurity.backend.utils;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import java.net.URI;
import java.net.URISyntaxException;

/** Validates URLs that will be presented as clickable user supplied links. */
public final class SafeLink {
    private static final int MAX_LENGTH = 2048;

    private SafeLink() {}

    public static String validate(String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        String candidate = value.trim();
        if (candidate.length() > MAX_LENGTH || candidate.chars().anyMatch(Character::isISOControl)
                || candidate.indexOf('\\') >= 0) {
            throw invalid(fieldName);
        }

        try {
            URI uri = new URI(candidate);
            if (candidate.startsWith("/")) {
                if (candidate.startsWith("//") || uri.isAbsolute() || uri.getRawAuthority() != null) {
                    throw invalid(fieldName);
                }
                return candidate;
            }
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null) {
                throw invalid(fieldName);
            }
            return uri.toASCIIString();
        } catch (URISyntaxException ex) {
            throw invalid(fieldName);
        }
    }

    private static BadRequestException invalid(String fieldName) {
        return new BadRequestException(fieldName + " debe ser una URL HTTPS válida o una ruta local segura");
    }
}
