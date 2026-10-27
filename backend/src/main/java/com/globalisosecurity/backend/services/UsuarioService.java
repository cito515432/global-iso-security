package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.UsuarioCreateRequest;
import com.globalisosecurity.backend.dto.UsuarioMeResponse;
import com.globalisosecurity.backend.dto.UsuarioProfileUpdateRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.EmpresaRepository;
import com.globalisosecurity.backend.repositories.RolRepository;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.utils.SecurityUtils;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final LogAuditoriaService logs;
    private final MfaService mfaService;
    private final LoginAttemptService loginAttempts;

    public UsuarioService(UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            LogAuditoriaService logs,
            MfaService mfaService,
            LoginAttemptService loginAttempts) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.logs = logs;
        this.mfaService = mfaService;
        this.loginAttempts = loginAttempts;
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Transactional
    public Usuario crearUsuario(UsuarioCreateRequest request) {
        validarRequest(request, true);
        passwordPolicy.validate(request.getRawPassword());

        String emailNormalizado = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.findByEmail(emailNormalizado).isPresent()) {
            throw new BadRequestException("Ya existe un usuario con ese email");
        }

        List<Rol> roles = resolverRoles(request);
        Rol rol = roles.get(0);

        Empresa empresa = null;
        if (request.getEmpresaId() != null) {
            empresa = empresaRepository.findById(request.getEmpresaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre().trim());
        usuario.setEmail(emailNormalizado);
        usuario.setPassword(passwordEncoder.encode(request.getRawPassword()));
        usuario.setRol(rol);
        usuario.setRoles(new LinkedHashSet<>(roles));
        usuario.setEmpresa(empresa);
        Usuario saved = usuarioRepository.save(usuario);
        logs.registrarLog("CREAR_USUARIO", "USUARIOS", "Se creó el usuario " + emailNormalizado
                + " con rol " + rol.getNombre());
        return saved;
    }

    @Transactional
    public Usuario actualizarUsuario(Long id, UsuarioCreateRequest request) {
        validarRequest(request, false);
        String emailNormalizado = request.getEmail().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Optional<Usuario> existente = usuarioRepository.findByEmail(emailNormalizado);
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new BadRequestException("Ya existe un usuario con ese email");
        }

        List<Rol> roles = resolverRoles(request);
        Rol rol = roles.get(0);

        Empresa empresa = null;
        if (request.getEmpresaId() != null) {
            empresa = empresaRepository.findById(request.getEmpresaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        }

        usuario.setNombre(request.getNombre().trim());
        usuario.setEmail(emailNormalizado);
        usuario.setRol(rol);
        usuario.setRoles(new LinkedHashSet<>(roles));
        usuario.setEmpresa(empresa);

        if (request.getRawPassword() != null && !request.getRawPassword().isEmpty()) {
            passwordPolicy.validate(request.getRawPassword());
            usuario.setPassword(passwordEncoder.encode(request.getRawPassword()));
        }

        Usuario saved = usuarioRepository.save(usuario);
        mfaService.revokeSessions(usuario.getId());
        logs.registrarLog("ACTUALIZAR_USUARIO", "USUARIOS", "Se actualizó el usuario " + emailNormalizado
                + " y su asignación de rol/empresa");
        return saved;
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        usuarioRepository.delete(usuario);
        logs.registrarLog("ELIMINAR_USUARIO", "USUARIOS", "Se eliminó el usuario " + usuario.getEmail());
    }

    public UsuarioMeResponse obtenerUsuarioAutenticado() {
        String email = SecurityUtils.getUsuarioActual();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        UsuarioMeResponse response = new UsuarioMeResponse();
        response.setId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setEmail(usuario.getEmail());
        if (usuario.getRol() != null) {
            response.setRol(usuario.getRol().getNombre());
            response.setPerfilBase(RolPerfilUtil.effectiveProfile(usuario.getRol()));
            response.setPermisos(usuario.getRol().getPermisos());
        }

        if (usuario.getEmpresa() != null) {
            response.setEmpresa(new UsuarioMeResponse.EmpresaResumen(
                    usuario.getEmpresa().getId(),
                    usuario.getEmpresa().getNombre()
            ));
        } else {
            response.setEmpresa(null);
        }
        return response;
    }

    @Transactional
    public UsuarioMeResponse actualizarMiPerfil(UsuarioProfileUpdateRequest request) {
        if (request == null || request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new BadRequestException("Ingrese su contraseña actual para confirmar el cambio");
        }
        String currentEmail = SecurityUtils.getUsuarioActual().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
        String confirmationKey = loginAttempts.keyFor("profile-update:" + currentEmail);
        if (loginAttempts.isBlocked(confirmationKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "La confirmación está temporalmente limitada. Intente más tarde.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), usuario.getPassword())) {
            loginAttempts.recordFailure(confirmationKey);
            throw new BadRequestException("La contraseña actual no es correcta");
        }
        loginAttempts.recordSuccess(confirmationKey);

        boolean changed = false;
        if (request.getNombre() != null) {
            String nombre = request.getNombre().trim();
            if (nombre.isEmpty() || nombre.length() > 255) throw new BadRequestException("El nombre no es válido");
            usuario.setNombre(nombre);
            changed = true;
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
            if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new BadRequestException("El correo no tiene un formato válido");
            }
            Optional<Usuario> existing = usuarioRepository.findByEmail(email);
            if (existing.isPresent() && !existing.get().getId().equals(usuario.getId())) {
                throw new BadRequestException("Ya existe un usuario con ese email");
            }
            if (!email.equals(currentEmail)) {
                usuario.setEmail(email);
                changed = true;
            }
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            if (!request.getNewPassword().equals(request.getPasswordConfirmation())) {
                throw new BadRequestException("La confirmación de contraseña no coincide");
            }
            if (passwordEncoder.matches(request.getNewPassword(), usuario.getPassword())) {
                throw new BadRequestException("La nueva contraseña debe ser diferente");
            }
            passwordPolicy.validate(request.getNewPassword());
            usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
            changed = true;
        }
        if (!changed) throw new BadRequestException("Indique un nuevo nombre, correo o contraseña");

        usuarioRepository.save(usuario);
        mfaService.revokeSessions(usuario.getId());
        logs.registrarEvento(currentEmail, "ACTUALIZAR_PERFIL", "USUARIOS",
                "El usuario actualizó su perfil; se revocaron las sesiones activas");
        return obtenerUsuarioAutenticadoPorId(usuario.getId());
    }

    private UsuarioMeResponse obtenerUsuarioAutenticadoPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
        UsuarioMeResponse response = new UsuarioMeResponse();
        response.setId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setEmail(usuario.getEmail());
        if (usuario.getRol() != null) {
            response.setRol(usuario.getRol().getNombre());
            response.setPerfilBase(RolPerfilUtil.effectiveProfile(usuario.getRol()));
            response.setPermisos(usuario.getRol().getPermisos());
        }
        if (usuario.getEmpresa() != null) response.setEmpresa(new UsuarioMeResponse.EmpresaResumen(
                usuario.getEmpresa().getId(), usuario.getEmpresa().getNombre()));
        return response;
    }

    private List<Rol> resolverRoles(UsuarioCreateRequest request) {
        List<Long> ids = request.getRolIds() == null || request.getRolIds().isEmpty()
                ? List.of(request.getRolId()) : request.getRolIds();
        if (ids.size() > 10 || ids.stream().anyMatch(id -> id == null || id <= 0)
                || new LinkedHashSet<>(ids).size() != ids.size()) {
            throw new BadRequestException("Seleccione entre 1 y 10 roles distintos");
        }
        if (request.getRolId() != null && !ids.contains(request.getRolId())) {
            throw new BadRequestException("El rol principal debe estar entre los roles asignados");
        }
        List<Rol> result = new ArrayList<>();
        for (Long id : ids) {
            Rol role = rolRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
            if (Boolean.FALSE.equals(role.getActivo())) throw new BadRequestException("No se puede asignar un rol inactivo");
            result.add(role);
        }
        if (request.getRolId() != null && !result.get(0).getId().equals(request.getRolId())) {
            Rol primary = result.stream().filter(role -> role.getId().equals(request.getRolId())).findFirst().orElseThrow();
            result.remove(primary);
            result.add(0, primary);
        }
        return result;
    }

    private void validarRequest(UsuarioCreateRequest request, boolean passwordObligatoria) {
        if (request == null) {
            throw new BadRequestException("El body de la solicitud es obligatorio");
        }
        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new BadRequestException("El nombre es obligatorio");
        }
        if (request.getNombre().trim().length() > 255) {
            throw new BadRequestException("El nombre supera la longitud permitida");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("El email es obligatorio");
        }
        if (request.getEmail().trim().length() > 254 || !request.getEmail().contains("@")) {
            throw new BadRequestException("El email no tiene un formato válido");
        }
        if (passwordObligatoria && (request.getRawPassword() == null || request.getRawPassword().isEmpty())) {
            throw new BadRequestException("La contraseña es obligatoria");
        }
        if ((request.getRolId() == null && (request.getRolIds() == null || request.getRolIds().isEmpty()))) {
            throw new BadRequestException("El rolId es obligatorio");
        }
        // La empresa es opcional para permitir usuarios administrativos o roles globales.
    }
}
