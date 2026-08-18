package com.globalisosecurity.backend.config;

import com.globalisosecurity.backend.models.ControlCatalogo;
import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.models.PerfilOrganizacional;
import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.models.Sector;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.models.SoaControl;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.ControlCatalogoRepository;
import com.globalisosecurity.backend.repositories.EmpresaRepository;
import com.globalisosecurity.backend.repositories.PerfilOrganizacionalRepository;
import com.globalisosecurity.backend.repositories.RolRepository;
import com.globalisosecurity.backend.repositories.SectorRepository;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import com.globalisosecurity.backend.repositories.SoaControlRepository;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.services.CatalogoControlesService;
import com.globalisosecurity.backend.services.ControlRelevanciaService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicializa únicamente datos estructurales e idempotentes. La carga puede
 * ejecutarse varias veces sin duplicar controles, roles, perfiles o usuarios.
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final String DEMO_COMPANY = "Organización Demo RPM";
    private static final String DEMO_PASSWORD = "Demo123*";

    private final RolRepository roles;
    private final SectorRepository sectores;
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final ServicioRepository servicios;
    private final PerfilOrganizacionalRepository perfiles;
    private final ControlCatalogoRepository controles;
    private final SoaControlRepository soa;
    private final CatalogoControlesService catalogo;
    private final ControlRelevanciaService relevancia;
    private final PasswordEncoder encoder;

    @Value("${app.seed.admin-email:admin@globalisosecurity.com}")
    private String adminEmail;

    @Value("${app.seed.admin-password:Admin123*}")
    private String adminPassword;

    @Value("${app.seed.demo-data:false}")
    private boolean demoData;

    public DataInitializer(
            RolRepository roles,
            SectorRepository sectores,
            UsuarioRepository usuarios,
            EmpresaRepository empresas,
            ServicioRepository servicios,
            PerfilOrganizacionalRepository perfiles,
            ControlCatalogoRepository controles,
            SoaControlRepository soa,
            CatalogoControlesService catalogo,
            ControlRelevanciaService relevancia,
            PasswordEncoder encoder) {
        this.roles = roles;
        this.sectores = sectores;
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.servicios = servicios;
        this.perfiles = perfiles;
        this.controles = controles;
        this.soa = soa;
        this.catalogo = catalogo;
        this.relevancia = relevancia;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Rol> seededRoles = seedRoles();
        Map<String, Sector> seededSectors = seedSectores();
        catalogo.cargarCatalogoBase();
        ensureAdministrator(seededRoles.get("ADMINISTRADOR"));

        if (demoData) {
            seedDemo(seededRoles, seededSectors);
        }

        for (Servicio servicio : servicios.findAll()) {
            PerfilOrganizacional perfil = ensureProfile(servicio);
            ensureSoa(servicio, perfil);
        }
    }

    private Map<String, Rol> seedRoles() {
        Map<String, Rol> result = new HashMap<>();

        // Conserva los usuarios asociados al rol legado y convierte el rol en el
        // perfil de organización que sí tiene una experiencia propia en frontend.
        if (roles.findByNombreIgnoreCase("USUARIO_EMPRESA").isEmpty()) {
            roles.findByNombreIgnoreCase("USUARIO").ifPresent(legacy -> {
                legacy.setNombre("USUARIO_EMPRESA");
                roles.save(legacy);
            });
        }

        role(result, "ADMINISTRADOR",
                "Gobierno de plataforma, usuarios, empresas, catálogo y configuración",
                "{\"dashboard\":true,\"usuarios\":true,\"roles\":true,\"empresas\":true,\"reportes\":true,\"configuracion\":true}");
        role(result, "IMPLEMENTADOR",
                "Construcción de la SoA, riesgos, controles y evidencias",
                "{\"soa\":true,\"riesgos\":true,\"evidencias\":true,\"rpm\":true}");
        role(result, "AUDITOR",
                "Validación de evidencias, hallazgos, firmas y resultados RPM",
                "{\"auditoria\":true,\"evidencias\":true,\"rpm\":true}");
        role(result, "CAPACITADOR",
                "Gestión de formación, participantes, evaluaciones y recomendaciones RPM",
                "{\"capacitaciones\":true,\"rpm\":true}");
        role(result, "USUARIO_EMPRESA",
                "Portal ejecutivo de progreso, decisiones, riesgos y reportes",
                "{\"portalEmpresa\":true,\"rpm\":true,\"reportes\":true}");
        return result;
    }

    private void role(Map<String, Rol> result, String name, String description, String permissions) {
        Rol role = roles.findByNombreIgnoreCase(name).orElseGet(() -> {
            Rol created = new Rol();
            created.setNombre(name);
            return created;
        });
        role.setDescripcion(description);
        role.setPermisos(permissions);
        role.setActivo(true);
        result.put(name, roles.save(role));
    }

    private Map<String, Sector> seedSectores() {
        Map<String, Sector> result = new HashMap<>();
        for (String name : List.of(
                "Salud", "Educación", "Financiero", "Tecnología",
                "Manufactura", "Servicios", "Sector público")) {
            Sector sector = sectores.findByNombre(name);
            if (sector == null) {
                sector = new Sector();
                sector.setNombre(name);
                sector = sectores.save(sector);
            }
            result.put(name, sector);
        }
        return result;
    }

    private void ensureAdministrator(Rol adminRole) {
        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (usuarios.findByEmail(normalizedEmail).isPresent()) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre("Administrador Global ISO");
        admin.setEmail(normalizedEmail);
        admin.setPassword(encoder.encode(adminPassword));
        admin.setRol(adminRole);
        admin.setEmpresa(null);
        usuarios.save(admin);
    }

    private void seedDemo(Map<String, Rol> seededRoles, Map<String, Sector> seededSectors) {
        Empresa company = empresas.findByNombre(DEMO_COMPANY);
        if (company == null) {
            company = new Empresa();
            company.setNombre(DEMO_COMPANY);
            company = empresas.save(company);
        }

        final Empresa demoCompany = company;
        Sector technology = seededSectors.get("Tecnología");
        Servicio service = servicios.findFirstByEmpresaIdOrderByFechaCreacionDesc(demoCompany.getId())
                .orElseGet(() -> {
                    Servicio created = new Servicio();
                    created.setEmpresa(demoCompany);
                    created.setSector(technology);
                    created.setEstado("EN_PROCESO");
                    created.setFechaCreacion(LocalDateTime.now());
                    return servicios.save(created);
                });

        PerfilOrganizacional profile = perfiles.findByEmpresaId(demoCompany.getId()).orElseGet(PerfilOrganizacional::new);
        profile.setEmpresa(demoCompany);
        profile.setSector(service.getSector());
        profile.setTamano("MEDIANA");
        profile.setManejaDatosSensibles(true);
        profile.setUsaServiciosNube(true);
        profile.setPermiteTrabajoRemoto(true);
        profile.setDependeProveedores(true);
        profile.setInfraestructuraPropia(false);
        profile.setProcesaPagos(false);
        profile.setServicioCritico24x7(false);
        profile.setManejaMenores(false);
        profile.setOperaOtIot(false);
        profile.setAlcanceSgsi("Servicios web, infraestructura en la nube y procesos de soporte de la organización demo.");
        profile.setResponsableSgsi("Responsable SGSI Demo");
        profile.setActualizadoEn(LocalDateTime.now());
        perfiles.save(profile);

        user("implementador@demo.com", "Implementador Demo", seededRoles.get("IMPLEMENTADOR"), demoCompany);
        user("auditor@demo.com", "Auditor Demo", seededRoles.get("AUDITOR"), demoCompany);
        user("capacitador@demo.com", "Capacitador Demo", seededRoles.get("CAPACITADOR"), demoCompany);
        user("empresa@demo.com", "Responsable Empresa Demo", seededRoles.get("USUARIO_EMPRESA"), demoCompany);
    }

    private void user(String email, String name, Rol role, Empresa company) {
        if (usuarios.findByEmail(email).isPresent()) {
            return;
        }
        Usuario user = new Usuario();
        user.setEmail(email);
        user.setNombre(name);
        user.setPassword(encoder.encode(DEMO_PASSWORD));
        user.setRol(role);
        user.setEmpresa(company);
        usuarios.save(user);
    }

    private PerfilOrganizacional ensureProfile(Servicio service) {
        return perfiles.findByEmpresaId(service.getEmpresa().getId()).orElseGet(() -> {
            PerfilOrganizacional profile = new PerfilOrganizacional();
            profile.setEmpresa(service.getEmpresa());
            profile.setSector(service.getSector());
            profile.setActualizadoEn(LocalDateTime.now());
            return perfiles.save(profile);
        });
    }

    private void ensureSoa(Servicio service, PerfilOrganizacional profile) {
        for (ControlCatalogo control : controles.findByActivoTrueOrderByCodigoAsc()) {
            ControlRelevanciaService.Relevancia recommendation = relevancia.evaluar(control, service, profile);
            SoaControl item = soa.findByServicioIdAndControlId(service.getId(), control.getId())
                    .orElseGet(() -> {
                        SoaControl created = new SoaControl();
                        created.setServicio(service);
                        created.setControl(control);
                        return created;
                    });
            item.setPuntajeRelevancia(recommendation.puntaje());
            item.setRecomendacionContextual(recommendation.motivo());
            item.setActualizadoEn(LocalDateTime.now());
            soa.save(item);
        }
    }
}
