package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Política mínima centralizada para contraseñas nuevas o rotadas.
 * No modifica contraseñas existentes hasta que el usuario/administrador las cambie.
 */
@Component
public class PasswordPolicy {

    private final int minLength;

    public PasswordPolicy(@Value("${app.security.password.min-length:12}") int minLength) {
        this.minLength = Math.max(12, minLength);
    }

    public void validate(String password) {
        if (password == null || password.isEmpty()) {
            throw new BadRequestException("La contraseña es obligatoria");
        }
        if (password.length() < minLength) {
            throw new BadRequestException("La contraseña debe tener al menos " + minLength + " caracteres");
        }
        if (password.length() > 128) {
            throw new BadRequestException("La contraseña no puede superar 128 caracteres");
        }
        if (password.chars().anyMatch(Character::isISOControl)) {
            throw new BadRequestException("La contraseña contiene caracteres de control no permitidos");
        }

        int groups = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) groups++;
        if (password.chars().anyMatch(Character::isLowerCase)) groups++;
        if (password.chars().anyMatch(Character::isDigit)) groups++;
        if (password.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) groups++;

        if (groups < 3) {
            throw new BadRequestException("La contraseña debe combinar al menos tres grupos: mayúsculas, minúsculas, números o símbolos");
        }
    }
}
