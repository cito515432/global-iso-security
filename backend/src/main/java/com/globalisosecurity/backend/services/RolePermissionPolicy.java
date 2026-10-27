package com.globalisosecurity.backend.services;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/** Central mapping from API operations to the permissions configured on roles. */
public final class RolePermissionPolicy {

    private static final Set<String> SAFE_PREFIXES = Set.of(
            "/api/auth/", "/api/health", "/api/readiness");

    private RolePermissionPolicy() {}

    public static String requiredPermission(String rawPath, String rawMethod) {
        if (rawPath == null || !rawPath.startsWith("/api/")) return null;
        String path = rawPath;
        while (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        String method = rawMethod == null ? "GET" : rawMethod.toUpperCase(Locale.ROOT);

        if ("OPTIONS".equals(method) || SAFE_PREFIXES.stream().anyMatch(path::startsWith)
                || "/api/usuarios/me".equals(path)
                || "/api/constancias-capacitacion/verificar".equals(path)
                || path.startsWith("/api/constancias-capacitacion/verificar/")) {
            return null;
        }
        if (path.equals("/api/usuarios") || path.startsWith("/api/usuarios/")) return "PERM_USUARIOS";
        if (path.equals("/api/roles") || path.startsWith("/api/roles/")) return "PERM_ROLES";
        if (path.equals("/api/logs-auditoria") || path.startsWith("/api/logs-auditoria/")) return "PERM_CONFIGURACION";
        if (path.equals("/api/dashboard") || path.startsWith("/api/dashboard/")) return "PERM_DASHBOARD";
        if (path.equals("/api/reportes") || path.startsWith("/api/reportes/")) return "PERM_REPORTES";
        if (path.equals("/api/sectores") || path.startsWith("/api/sectores/")) {
            return isWrite(method) ? "PERM_CONFIGURACION" : "PERM_EMPRESAS";
        }

        // The existing role checks and tenant checks still decide which actions a profile may take.
        // This layer enforces the role's enabled capability before the controller is invoked.
        return isWrite(method) ? "PERM_CREAR_EDITAR" : "PERM_EMPRESAS";
    }

    public static boolean isAuthorized(Collection<String> authorities, String requiredPermission) {
        if (requiredPermission == null) return true;
        if (authorities == null) return false;
        return authorities.contains("ROLE_ADMINISTRADOR") || authorities.contains(requiredPermission);
    }

    private static boolean isWrite(String method) {
        return Set.of("POST", "PUT", "PATCH", "DELETE").contains(method);
    }
}
