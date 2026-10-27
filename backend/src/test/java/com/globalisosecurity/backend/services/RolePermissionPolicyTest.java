package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RolePermissionPolicyTest {

    @Test
    void mapsBusinessReadsAndWritesToDifferentPermissions() {
        assertEquals("PERM_EMPRESAS", RolePermissionPolicy.requiredPermission("/api/riesgos/servicio/4", "GET"));
        assertEquals("PERM_CREAR_EDITAR", RolePermissionPolicy.requiredPermission("/api/riesgos", "POST"));
        assertEquals("PERM_REPORTES", RolePermissionPolicy.requiredPermission("/api/reportes/empresa/4/pdf", "GET"));
        assertEquals("PERM_DASHBOARD", RolePermissionPolicy.requiredPermission("/api/dashboard/resumen", "GET"));
    }

    @Test
    void respectsDisabledPermissionAndKeepsAdminOperationsReserved() {
        assertFalse(RolePermissionPolicy.isAuthorized(Set.of("ROLE_IMPLEMENTADOR"), "PERM_REPORTES"));
        assertTrue(RolePermissionPolicy.isAuthorized(Set.of("ROLE_IMPLEMENTADOR", "PERM_REPORTES"), "PERM_REPORTES"));
        assertTrue(RolePermissionPolicy.isAuthorized(Set.of("ROLE_ADMINISTRADOR"), "PERM_REPORTES"));
        assertEquals("PERM_USUARIOS", RolePermissionPolicy.requiredPermission("/api/usuarios", "GET"));
        assertEquals("PERM_ROLES", RolePermissionPolicy.requiredPermission("/api/roles", "PUT"));
    }

    @Test
    void exemptsSelfProfileAndPublicVerificationRoutes() {
        assertNull(RolePermissionPolicy.requiredPermission("/api/usuarios/me", "PUT"));
        assertNull(RolePermissionPolicy.requiredPermission("/api/constancias-capacitacion/verificar/ABC", "GET"));
        assertNull(RolePermissionPolicy.requiredPermission("/api/auth/login", "POST"));
    }
}
