package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.models.Usuario;
import java.util.LinkedHashSet;
import java.util.Set;
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

    @Test
    void multipleActiveRolesContributeTheirConfiguredPermissions() {
        Rol reportRole = new Rol();
        reportRole.setNombre("LECTOR_REPORTES");
        reportRole.setActivo(true);
        reportRole.setPermisos("{\"perfilBase\":\"USUARIO_EMPRESA\",\"reportes\":true}");
        Rol editorRole = new Rol();
        editorRole.setNombre("EDITOR_EMPRESA");
        editorRole.setActivo(true);
        editorRole.setPermisos("{\"perfilBase\":\"IMPLEMENTADOR\",\"crearEditar\":true}");
        Rol inactiveRole = new Rol();
        inactiveRole.setNombre("INACTIVO");
        inactiveRole.setActivo(false);

        Usuario user = new Usuario();
        user.setRol(reportRole);
        user.setRoles(new LinkedHashSet<>(Set.of(reportRole, editorRole, inactiveRole)));

        var activeRoles = RolPerfilUtil.activeRoles(user);
        assertEquals(2, activeRoles.size());
        assertTrue(RolPerfilUtil.permissionAuthorities(reportRole).contains("PERM_REPORTES"));
        assertTrue(RolPerfilUtil.permissionAuthorities(editorRole).contains("PERM_CREAR_EDITAR"));
    }
}
