package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.utils.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class AccesoEmpresaService {
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;

    public AccesoEmpresaService(UsuarioRepository usuarioRepository, ServicioRepository servicioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
    }

    public Usuario usuarioActual() {
        return usuarioRepository.findByEmail(SecurityUtils.getUsuarioActual())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    public String rolActual() {
        Usuario u = usuarioActual();
        return u.getRol() == null || u.getRol().getNombre() == null ? "" : u.getRol().getNombre().trim().toUpperCase();
    }

    public boolean esAdministrador() {
        return rolActual().contains("ADMIN");
    }

    /**
     * Compatibilidad con servicios existentes. Tras el hardening, solo el
     * administrador puede actuar sin una empresa asignada.
     */
    public boolean esRolInternoGlobal() {
        return esAdministrador();
    }

    public void validarEmpresa(Long empresaId) {
        if (empresaId == null) throw new BadRequestException("La empresa es obligatoria");
        Usuario u = usuarioActual();
        if (esAdministrador()) return;
        if (u.getEmpresa() == null) {
            throw new BadRequestException("El usuario no tiene empresa asignada");
        }
        if (!u.getEmpresa().getId().equals(empresaId)) {
            throw new BadRequestException("No tiene autorización para consultar esta empresa");
        }
    }

    public Servicio servicioAutorizado(Long servicioId) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
        validarEmpresa(servicio.getEmpresa().getId());
        return servicio;
    }

    public Long empresaActualObligatoria() {
        Usuario u = usuarioActual();
        if (u.getEmpresa() == null) throw new BadRequestException("El usuario no tiene empresa asignada");
        return u.getEmpresa().getId();
    }
}
