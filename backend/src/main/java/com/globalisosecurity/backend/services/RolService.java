package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.RolRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.repositories.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class RolService {

    private static final Set<String> ROLES_SISTEMA = Set.of("ADMINISTRADOR", "IMPLEMENTADOR", "AUDITOR", "CAPACITADOR", "USUARIO_EMPRESA");

    private final RolRepository rolRepository;
    private final LogAuditoriaService audit;

    public RolService(RolRepository rolRepository, LogAuditoriaService audit) {
        this.rolRepository = rolRepository;
        this.audit = audit;
    }

    public List<Rol> obtenerTodos() {
        List<Rol> roles = rolRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (roles.isEmpty()) {
            crearRolesBase();
            roles = rolRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        }
        return roles;
    }

    public Rol obtenerPorId(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    @Transactional
    public Rol crearRol(RolRequest request) {
        validarRequest(request);
        String nombreNormalizado = normalizarNombre(request.getNombre());
        if (ROLES_SISTEMA.contains(nombreNormalizado)) {
            throw new BadRequestException("Los nombres de roles del sistema están reservados");
        }

        if (rolRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new BadRequestException("Ya existe un rol con ese nombre");
        }

        Rol rol = new Rol();
        rol.setNombre(nombreNormalizado);
        rol.setDescripcion(normalizarTextoOpcional(request.getDescripcion()));
        rol.setActivo(request.getActivo() == null ? true : request.getActivo());
        String perfilBase = validarPerfilBase(request.getPerfilBase());
        rol.setPermisos(permisosParaPerfil(perfilBase));

        Rol created = rolRepository.save(rol);
        audit.registrarLog("CREAR_ROL", "ROLES", "Se creó el rol " + nombreNormalizado
                + " con perfil base " + perfilBase);
        return created;
    }

    @Transactional
    public Rol actualizarRol(Long id, RolRequest request) {
        validarRequest(request);
        String nombreNormalizado = normalizarNombre(request.getNombre());

        Rol rol = obtenerPorId(id);
        if (ROLES_SISTEMA.contains(rol.getNombre().toUpperCase(Locale.ROOT))
                && !rol.getNombre().equalsIgnoreCase(nombreNormalizado)) {
            throw new BadRequestException("Los roles base no pueden cambiar de nombre porque protegen rutas del sistema");
        }
        Optional<Rol> existente = rolRepository.findByNombreIgnoreCase(nombreNormalizado);
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new BadRequestException("Ya existe otro rol con ese nombre");
        }
        if (!ROLES_SISTEMA.contains(rol.getNombre().toUpperCase(Locale.ROOT))
                && ROLES_SISTEMA.contains(nombreNormalizado)) {
            throw new BadRequestException("Los nombres de roles del sistema están reservados");
        }

        rol.setNombre(nombreNormalizado);
        rol.setDescripcion(normalizarTextoOpcional(request.getDescripcion()));
        rol.setActivo(request.getActivo() == null ? true : request.getActivo());
        if (!ROLES_SISTEMA.contains(nombreNormalizado)) {
            String base = request.getPerfilBase() == null || request.getPerfilBase().isBlank()
                    ? RolPerfilUtil.effectiveProfile(rol)
                    : validarPerfilBase(request.getPerfilBase());
            rol.setPermisos(permisosParaPerfil(base));
        }

        Rol updated = rolRepository.save(rol);
        audit.registrarLog("ACTUALIZAR_ROL", "ROLES", "Se actualizó el rol " + nombreNormalizado);
        return updated;
    }

    @Transactional
    public Rol cambiarEstado(Long id, Boolean activo) {
        if (activo == null) {
            throw new BadRequestException("El estado activo es obligatorio");
        }

        Rol rol = obtenerPorId(id);
        rol.setActivo(activo);
        Rol updated = rolRepository.save(rol);
        audit.registrarLog("CAMBIAR_ESTADO_ROL", "ROLES", "Se " + (activo ? "activó" : "desactivó")
                + " el rol " + rol.getNombre());
        return updated;
    }

    @Transactional
    public void eliminarRol(Long id) {
        Rol rol = obtenerPorId(id);
        if (ROLES_SISTEMA.contains(rol.getNombre().toUpperCase(Locale.ROOT))) {
            throw new BadRequestException("Los roles base no se eliminan; puede desactivarlos de forma controlada");
        }
        rolRepository.delete(rol);
        audit.registrarLog("ELIMINAR_ROL", "ROLES", "Se eliminó el rol " + rol.getNombre());
    }


    private void crearRolesBase() {
        crearRolBase("ADMINISTRADOR", "Acceso completo al panel administrativo.", "{\"dashboard\":true,\"usuarios\":true,\"roles\":true,\"empresas\":true,\"reportes\":true,\"configuracion\":true,\"crearEditar\":true}");
        crearRolBase("IMPLEMENTADOR", "Gestiona procesos de implementación y empresas asignadas.", "{\"dashboard\":true,\"usuarios\":false,\"roles\":false,\"empresas\":true,\"reportes\":true,\"configuracion\":false,\"crearEditar\":true}");
        crearRolBase("AUDITOR", "Revisa auditorías, evidencias y reportes.", "{\"dashboard\":true,\"usuarios\":false,\"roles\":false,\"empresas\":true,\"reportes\":true,\"configuracion\":false,\"crearEditar\":false}");
        crearRolBase("CAPACITADOR", "Gestiona actividades de capacitación.", "{\"dashboard\":true,\"usuarios\":false,\"roles\":false,\"empresas\":true,\"reportes\":true,\"configuracion\":false,\"crearEditar\":true}");
        crearRolBase("USUARIO_EMPRESA", "Portal ejecutivo de progreso, decisiones, riesgos y reportes.", "{\"dashboard\":true,\"usuarios\":false,\"roles\":false,\"empresas\":false,\"reportes\":false,\"configuracion\":false,\"crearEditar\":false}");
    }

    private void crearRolBase(String nombre, String descripcion, String permisos) {
        if (rolRepository.existsByNombreIgnoreCase(nombre)) return;

        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(descripcion);
        rol.setActivo(true);
        rol.setPermisos(permisos);
        rolRepository.save(rol);
    }

    private void validarRequest(RolRequest request) {
        if (request == null) {
            throw new BadRequestException("El body de la solicitud es obligatorio");
        }

        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new BadRequestException("El nombre del rol es obligatorio");
        }
        String name = normalizarNombre(request.getNombre());
        if (!name.matches("[A-Z][A-Z0-9_]{2,49}")) {
            throw new BadRequestException("Use entre 3 y 50 letras, números o guiones bajos para el nombre del rol");
        }
        if (request.getDescripcion() != null && request.getDescripcion().length() > 500) {
            throw new BadRequestException("La descripción no puede superar 500 caracteres");
        }
    }

    private String normalizarNombre(String nombre) {
        return nombre.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String validarPerfilBase(String value) {
        if (!RolPerfilUtil.isCustomProfile(value)) {
            throw new BadRequestException("El rol debe heredar un perfil válido y nunca puede heredar ADMINISTRADOR");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String permisosParaPerfil(String profile) {
        String capabilities = switch (profile) {
            case "IMPLEMENTADOR" -> "\"dashboard\":true,\"usuarios\":false,\"roles\":false,\"empresas\":true,\"reportes\":true,\"configuracion\":false,\"crearEditar\":true";
            case "AUDITOR" -> "\"dashboard\":true,\"usuarios\":false,\"roles\":false,\"empresas\":true,\"reportes\":true,\"configuracion\":false,\"crearEditar\":false";
            case "CAPACITADOR" -> "\"dashboard\":true,\"usuarios\":false,\"roles\":false,\"empresas\":true,\"reportes\":true,\"configuracion\":false,\"crearEditar\":true";
            default -> "\"dashboard\":true,\"usuarios\":false,\"roles\":false,\"empresas\":false,\"reportes\":false,\"configuracion\":false,\"crearEditar\":false";
        };
        return "{\"perfilBase\":\"" + profile + "\"," + capabilities + "}";
    }
}
