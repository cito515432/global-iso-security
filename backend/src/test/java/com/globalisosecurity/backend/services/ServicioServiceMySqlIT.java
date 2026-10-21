package com.globalisosecurity.backend.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.models.Sector;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.models.Usuario;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Sql({"/mysql-it-schema.sql", "/mysql-it-data.sql"})
class ServicioServiceMySqlIT {

    @Autowired private ServicioService servicioService;
    @Autowired private SoaService soaService;
    @Autowired private AuthService authService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ci-admin@example.test", "ignored",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
    }

    @Test
    void creatingServiceInitializesNinetyThreeControlsAndListingRepairsMissingOne() {
        String password = "SecureLogin2026!";
        jdbc.update("INSERT INTO usuarios (id, nombre, email, password, rol_id, empresa_id) "
                        + "VALUES (?, ?, ?, ?, ?, NULL)",
                2_000_000L, "Login CI", "login-ci@example.test", passwordEncoder.encode(password), 1L);

        ResponseEntity<?> login = authService.login("login-ci@example.test", password);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        assertThat(((Map<?, ?>) login.getBody()).get("token")).isNotNull();
        assertThat(((Map<?, ?>) login.getBody()).get("mfaConfigured")).isEqualTo(false);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(authService.login("missing-ci@example.test", "wrong").getStatusCode().value()).isEqualTo(401);
        }
        assertThat(authService.login("missing-ci@example.test", "wrong").getStatusCode().value()).isEqualTo(429);

        Empresa empresa = new Empresa();
        empresa.setId(1L);
        Sector sector = new Sector();
        sector.setId(1L);
        Servicio servicio = new Servicio();
        servicio.setEmpresa(empresa);
        servicio.setSector(sector);

        Servicio created = servicioService.crearServicio(servicio);

        assertThat(countSoa(created.getId())).isEqualTo(93);
        assertThat(countDistinctControls(created.getId())).isEqualTo(93);

        jdbc.update("DELETE FROM soa_controles WHERE servicio_id = ? LIMIT 1", created.getId());
        assertThat(countSoa(created.getId())).isEqualTo(92);

        assertThat(soaService.listar(created.getId())).hasSize(93);
        assertThat(countSoa(created.getId())).isEqualTo(93);
        assertThat(countDistinctControls(created.getId())).isEqualTo(93);
    }

    private int countSoa(Long serviceId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM soa_controles WHERE servicio_id = ?", Integer.class, serviceId);
    }

    private int countDistinctControls(Long serviceId) {
        return jdbc.queryForObject("SELECT COUNT(DISTINCT control_id) FROM soa_controles WHERE servicio_id = ?", Integer.class, serviceId);
    }
}
