package com.globalisosecurity.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalisosecurity.backend.models.Rol;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import com.globalisosecurity.backend.models.Usuario;

/** Maps custom role labels to one of the application's fixed, least-privilege profiles. */
public final class RolPerfilUtil {

    private static final Set<String> SYSTEM_PROFILES = Set.of(
            "ADMINISTRADOR", "IMPLEMENTADOR", "AUDITOR", "CAPACITADOR", "USUARIO_EMPRESA");
    private static final Set<String> CUSTOM_PROFILES = Set.of(
            "IMPLEMENTADOR", "AUDITOR", "CAPACITADOR", "USUARIO_EMPRESA");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> PERMISSION_KEYS = List.of(
            "dashboard", "usuarios", "roles", "empresas", "reportes", "configuracion", "crearEditar");

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

    public static List<Rol> activeRoles(Usuario user) {
        LinkedHashSet<Rol> values = new LinkedHashSet<>();
        if (user != null && user.getRoles() != null) values.addAll(user.getRoles());
        if (user != null && user.getRol() != null) values.add(user.getRol());
        return values.stream().filter(role -> role != null && role.getNombre() != null
                && !Boolean.FALSE.equals(role.getActivo())).toList();
    }

    public static Set<String> permissionAuthorities(Rol role) {
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        if (role == null) return authorities;
        try {
            JsonNode value = JSON.readTree(role.getPermisos());
            // `reportes` and other module names existed in the legacy schema too;
            // `perfilBase` or the new `crearEditar` capability marks the new format.
            boolean currentPermissionModel = value != null
                    && (value.has("perfilBase") || value.has("crearEditar"));
            for (String key : PERMISSION_KEYS) {
                if (value != null && value.path(key).isBoolean() && value.path(key).booleanValue()) {
                    authorities.add("PERM_" + key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT));
                }
            }
            // Production roles created before the configurable permission model use module names.
            // Translate those legacy flags only when no current-model field is present.
            if (!currentPermissionModel && value != null) {
                if (hasAny(value, "auditoria", "evidencias", "soa", "riesgos", "rpm", "capacitaciones", "portalEmpresa")) {
                    authorities.add("PERM_DASHBOARD");
                }
                if (hasAny(value, "auditoria", "evidencias", "soa", "riesgos", "rpm", "capacitaciones", "portalEmpresa")) {
                    authorities.add("PERM_EMPRESAS");
                }
                if (hasAny(value, "evidencias", "soa", "riesgos", "capacitaciones", "auditoria")) {
                    authorities.add("PERM_CREAR_EDITAR");
                }
                if (hasAny(value, "rpm", "reportes")) authorities.add("PERM_REPORTES");
                if (value.path("usuarios").asBoolean(false)) authorities.add("PERM_USUARIOS");
                if (value.path("roles").asBoolean(false)) authorities.add("PERM_ROLES");
                if (value.path("configuracion").asBoolean(false)) authorities.add("PERM_CONFIGURACION");
            }
        } catch (Exception ignored) {
            return authorities;
        }
        return authorities;
    }

    private static boolean hasAny(JsonNode value, String... keys) {
        for (String key : keys) if (value.path(key).asBoolean(false)) return true;
        return false;
    }
}
