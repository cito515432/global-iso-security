package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.globalisosecurity.backend.dto.RolRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.repositories.RolRepository;
import org.junit.jupiter.api.Test;

class RolServiceTest {

    @Test
    void createsCustomRoleWithARestrictedBackendProfile() {
        RolRepository roles = mock(RolRepository.class);
        LogAuditoriaService audit = mock(LogAuditoriaService.class);
        when(roles.existsByNombreIgnoreCase("GESTOR_CONTROLES")).thenReturn(false);
        when(roles.save(any(Rol.class))).thenAnswer(call -> call.getArgument(0));
        RolService service = new RolService(roles, audit);

        RolRequest request = new RolRequest();
        request.setNombre("Gestor_Controles");
        request.setPerfilBase("AUDITOR");
        request.setDescripcion("Revisa controles seleccionados");
        Rol created = service.crearRol(request);

        assertEquals("GESTOR_CONTROLES", created.getNombre());
        assertEquals("AUDITOR", RolPerfilUtil.effectiveProfile(created));
        verify(audit).registrarLog("CREAR_ROL", "ROLES",
                "Se creó el rol GESTOR_CONTROLES con perfil base AUDITOR");
    }

    @Test
    void refusesAdminProfileForCustomRole() {
        RolRepository roles = mock(RolRepository.class);
        RolService service = new RolService(roles, mock(LogAuditoriaService.class));
        RolRequest request = new RolRequest();
        request.setNombre("GESTOR_CONTROLES");
        request.setPerfilBase("ADMINISTRADOR");

        assertThrows(BadRequestException.class, () -> service.crearRol(request));
    }

    @Test
    void reservesSystemRoleNames() {
        RolRepository roles = mock(RolRepository.class);
        RolService service = new RolService(roles, mock(LogAuditoriaService.class));
        RolRequest request = new RolRequest();
        request.setNombre("ADMINISTRADOR");
        request.setPerfilBase("AUDITOR");

        assertThrows(BadRequestException.class, () -> service.crearRol(request));
    }
}
