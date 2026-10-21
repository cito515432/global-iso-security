package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Política mínima centralizada para contraseñas nuevas o rotadas.
 * No modifica contraseñas existentes hasta que el usuario/administrador las cambie.
 */
@Component
public class PasswordPolicy {

    private static final int MINIMUM_LENGTH = 8;
    private static final int MAXIMUM_LENGTH = 128;
    private static final List<String> COMMON_FRAGMENTS = List.of(
            "password", "qwerty", "letmein", "welcome", "admin");
    private final int minLength;

    public PasswordPolicy(@Value("${app.security.password.min-length:15}") int minLength) {
        this.minLength = Math.max(MINIMUM_LENGTH, minLength);
    }

    public void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new BadRequestException("La contraseña es obligatoria");
        }
        int length = password.codePointCount(0, password.length());
        if (length < minLength) {
            throw new BadRequestException("La contraseña debe tener al menos " + minLength + " caracteres");
        }
        if (length > MAXIMUM_LENGTH) {
            throw new BadRequestException("La contraseña no puede superar " + MAXIMUM_LENGTH + " caracteres");
        }
        if (password.chars().anyMatch(Character::isISOControl)) {
            throw new BadRequestException("La contraseña contiene caracteres de control no permitidos");
        }
        if (password.isBlank()) {
            throw new BadRequestException("La contraseña no puede contener solo espacios");
        }
        int[] categories = password.codePoints().toArray();
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (int codePoint : categories) {
            upper |= Character.isUpperCase(codePoint);
            lower |= Character.isLowerCase(codePoint);
            digit |= Character.isDigit(codePoint);
            special |= !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
        }
        if (!(upper && lower && digit && special)) {
            throw new BadRequestException("La contraseña debe incluir mayúscula, minúscula, número y carácter especial");
        }
        String normalized = password.toLowerCase(Locale.ROOT);
        if (COMMON_FRAGMENTS.stream().anyMatch(normalized::contains)) {
            throw new BadRequestException("Evite palabras de contraseña comunes; use una frase única");
        }
        int[] codePoints = categories;
        for (int period = 1; period <= 4; period++) {
            if (codePoints.length <= period || codePoints.length % period != 0) continue;
            boolean repeated = true;
            for (int index = period; index < codePoints.length; index++) {
                if (codePoints[index] != codePoints[index % period]) {
                    repeated = false;
                    break;
                }
            }
            if (repeated) {
                throw new BadRequestException("Evite repetir un carácter o patrón para completar la longitud");
            }
        }
    }
}
