package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.globalisosecurity.backend.models.Rol;
import org.junit.jupiter.api.Test;

class RolPerfilUtilTest {

    @Test
    void customRoleUsesOnlyItsApprovedBaseProfile() {
        Rol role = new Rol();
        role.setNombre("GESTOR_CONTROLES");
        role.setPermisos("{\"perfilBase\":\"AUDITOR\",\"roles\":true}");
        assertEquals("AUDITOR", RolPerfilUtil.effectiveProfile(role));
    }

    @Test
    void customRoleCannotEscalateToAdministratorThroughStoredPermissions() {
        Rol role = new Rol();
        role.setNombre("GESTOR_CONTROLES");
        role.setPermisos("{\"perfilBase\":\"ADMINISTRADOR\"}");
        assertEquals("USUARIO_EMPRESA", RolPerfilUtil.effectiveProfile(role));
    }

    @Test
    void fixedAdministratorRoleRetainsItsSystemAuthority() {
        Rol role = new Rol();
        role.setNombre("ADMINISTRADOR");
        role.setPermisos("{}");
        assertEquals("ADMINISTRADOR", RolPerfilUtil.effectiveProfile(role));
    }
}
