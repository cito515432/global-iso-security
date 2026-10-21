package com.globalisosecurity.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalisosecurity.backend.models.Rol;
import java.util.Locale;
import java.util.Set;

/** Maps custom role labels to one of the application's fixed, least-privilege profiles. */
public final class RolPerfilUtil {

    private static final Set<String> SYSTEM_PROFILES = Set.of(
            "ADMINISTRADOR", "IMPLEMENTADOR", "AUDITOR", "CAPACITADOR", "USUARIO_EMPRESA");
    private static final Set<String> CUSTOM_PROFILES = Set.of(
            "IMPLEMENTADOR", "AUDITOR", "CAPACITADOR", "USUARIO_EMPRESA");
    private static final ObjectMapper JSON = new ObjectMapper();

    private RolPerfilUtil() {}

    public static String effectiveProfile(Rol role) {
        if (role == null || role.getNombre() == null) return "USUARIO_EMPRESA";
        String name = role.getNombre().trim().toUpperCase(Locale.ROOT);
        if (SYSTEM_PROFILES.contains(name)) return name;
        try {
            JsonNode value = JSON.readTree(role.getPermisos());
            String base = value == null ? "" : value.path("perfilBase").asText("").trim().toUpperCase(Locale.ROOT);
            return CUSTOM_PROFILES.contains(base) ? base : "USUARIO_EMPRESA";
        } catch (Exception ignored) {
            return "USUARIO_EMPRESA";
        }
    }

    public static boolean isCustomProfile(String value) {
        return value != null && CUSTOM_PROFILES.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}
