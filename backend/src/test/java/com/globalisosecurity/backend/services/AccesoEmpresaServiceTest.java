package com.globalisosecurity.backend.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AccesoEmpresaServiceTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantMayAccessOnlyItsAssignedCompanyAndService() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        ServicioRepository services = mock(ServicioRepository.class);
        Usuario user = user("USUARIO_EMPRESA", "{}", 41L);
        when(users.findByEmail("empresa@example.test")).thenReturn(Optional.of(user));
        Servicio service = new Servicio();
        service.setId(900L);
        service.setEmpresa(user.getEmpresa());
        when(services.findById(900L)).thenReturn(Optional.of(service));
        authenticate();
        AccesoEmpresaService access = new AccesoEmpresaService(users, services);

        assertDoesNotThrow(() -> access.validarEmpresa(41L));
        assertDoesNotThrow(() -> access.servicioAutorizado(900L));
        assertThrows(BadRequestException.class, () -> access.validarEmpresa(42L));
    }

    @Test
    void customRoleCannotBypassTenantBoundaryByClaimingAdminProfile() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        ServicioRepository services = mock(ServicioRepository.class);
        when(users.findByEmail("empresa@example.test"))
                .thenReturn(Optional.of(user("GESTOR", "{\"perfilBase\":\"ADMINISTRADOR\"}", 41L)));
        authenticate();
        AccesoEmpresaService access = new AccesoEmpresaService(users, services);

        assertThrows(BadRequestException.class, () -> access.validarEmpresa(42L));
    }

    @Test
    void userWithoutCompanyCannotAccessTenantData() {
        UsuarioRepository users = mock(UsuarioRepository.class);
        ServicioRepository services = mock(ServicioRepository.class);
        when(users.findByEmail("empresa@example.test"))
                .thenReturn(Optional.of(user("IMPLEMENTADOR", "{}", null)));
        authenticate();
        AccesoEmpresaService access = new AccesoEmpresaService(users, services);

        assertThrows(BadRequestException.class, () -> access.validarEmpresa(41L));
    }

    private static void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("empresa@example.test", "token", List.of()));
    }

    private static Usuario user(String roleName, String permissions, Long companyId) {
        Rol role = new Rol();
        role.setNombre(roleName);
        role.setPermisos(permissions);
        Usuario user = new Usuario();
        user.setEmail("empresa@example.test");
        user.setRol(role);
        if (companyId != null) {
            Empresa company = new Empresa();
            company.setId(companyId);
            user.setEmpresa(company);
        }
        return user;
    }
}
