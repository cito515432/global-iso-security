package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.RolRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.repositories.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RolService {

    @Autowired
    private RolRepository rolRepository;

    public List<Rol> obtenerTodos() {
        return rolRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public Rol obtenerPorId(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    public Rol crearRol(RolRequest request) {
        validarRequest(request);
        String nombreNormalizado = normalizarNombre(request.getNombre());

        if (rolRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new BadRequestException("Ya existe un rol con ese nombre");
        }

        Rol rol = new Rol();
        rol.setNombre(nombreNormalizado);
        rol.setDescripcion(normalizarTextoOpcional(request.getDescripcion()));
        rol.setActivo(request.getActivo() == null ? true : request.getActivo());
        rol.setPermisos(normalizarTextoOpcional(request.getPermisos()));

        return rolRepository.save(rol);
    }

    public Rol actualizarRol(Long id, RolRequest request) {
        validarRequest(request);
        String nombreNormalizado = normalizarNombre(request.getNombre());

        Rol rol = obtenerPorId(id);
        Optional<Rol> existente = rolRepository.findByNombreIgnoreCase(nombreNormalizado);
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new BadRequestException("Ya existe otro rol con ese nombre");
        }

        rol.setNombre(nombreNormalizado);
        rol.setDescripcion(normalizarTextoOpcional(request.getDescripcion()));
        rol.setActivo(request.getActivo() == null ? true : request.getActivo());
        rol.setPermisos(normalizarTextoOpcional(request.getPermisos()));

        return rolRepository.save(rol);
    }

    public Rol cambiarEstado(Long id, Boolean activo) {
        if (activo == null) {
            throw new BadRequestException("El estado activo es obligatorio");
        }

        Rol rol = obtenerPorId(id);
        rol.setActivo(activo);
        return rolRepository.save(rol);
    }

    public void eliminarRol(Long id) {
        Rol rol = obtenerPorId(id);
        rolRepository.delete(rol);
    }

    private void validarRequest(RolRequest request) {
        if (request == null) {
            throw new BadRequestException("El body de la solicitud es obligatorio");
        }

        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new BadRequestException("El nombre del rol es obligatorio");
        }
    }

    private String normalizarNombre(String nombre) {
        return nombre.trim().toUpperCase();
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) return null;
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
