package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.UsuarioCreateRequest;
import com.globalisosecurity.backend.dto.UsuarioMeResponse;
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
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public UsuarioService(UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            EmpresaRepository empresaRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Usuario crearUsuario(UsuarioCreateRequest request) {
        validarRequest(request, true);
        passwordPolicy.validate(request.getRawPassword());

        String emailNormalizado = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.findByEmail(emailNormalizado).isPresent()) {
            throw new BadRequestException("Ya existe un usuario con ese email");
        }

        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        if (Boolean.FALSE.equals(rol.getActivo())) {
            throw new BadRequestException("No se puede asignar un rol inactivo");
        }

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
        usuario.setEmpresa(empresa);
        return usuarioRepository.save(usuario);
    }

    public Usuario actualizarUsuario(Long id, UsuarioCreateRequest request) {
        validarRequest(request, false);
        String emailNormalizado = request.getEmail().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Optional<Usuario> existente = usuarioRepository.findByEmail(emailNormalizado);
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new BadRequestException("Ya existe un usuario con ese email");
        }

        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        if (Boolean.FALSE.equals(rol.getActivo())) {
            throw new BadRequestException("No se puede asignar un rol inactivo");
        }

        Empresa empresa = null;
        if (request.getEmpresaId() != null) {
            empresa = empresaRepository.findById(request.getEmpresaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        }

        usuario.setNombre(request.getNombre().trim());
        usuario.setEmail(emailNormalizado);
        usuario.setRol(rol);
        usuario.setEmpresa(empresa);

        if (request.getRawPassword() != null && !request.getRawPassword().isEmpty()) {
            passwordPolicy.validate(request.getRawPassword());
            usuario.setPassword(passwordEncoder.encode(request.getRawPassword()));
        }

        return usuarioRepository.save(usuario);
    }

    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        usuarioRepository.delete(usuario);
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
        if (request.getRolId() == null) {
            throw new BadRequestException("El rolId es obligatorio");
        }
        // La empresa es opcional para permitir usuarios administrativos o roles globales.
    }
}
