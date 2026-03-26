/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Checklist;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.repositories.ChecklistRepository;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ChecklistService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE",
            "EN_PROCESO",
            "COMPLETADO"
    );

    @Autowired
    private ChecklistRepository checklistRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    public List<Checklist> obtenerTodos() {
        return checklistRepository.findAll();
    }

    public Optional<Checklist> obtenerPorId(Long id) {
        return checklistRepository.findById(id);
    }

    public List<Checklist> obtenerPorEstado(String estado) {
        return checklistRepository.findByEstado(normalizarEstado(estado));
    }

    public List<Checklist> obtenerPorServicio(Long servicioId) {
        return checklistRepository.findByServicioId(servicioId);
    }

    public Checklist crearChecklist(Checklist checklist) {
        validarChecklist(checklist);

        String estado = checklist.getEstado();
        if (estado == null || estado.trim().isEmpty()) {
            estado = "PENDIENTE";
        }

        estado = normalizarEstado(estado);
        validarEstado(estado);

        Servicio servicio = servicioRepository.findById(checklist.getServicio().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        checklist.setNombre(checklist.getNombre().trim());
        if (checklist.getDescripcion() != null) {
            checklist.setDescripcion(checklist.getDescripcion().trim());
        }
        checklist.setEstado(estado);
        checklist.setServicio(servicio);

        return checklistRepository.save(checklist);
    }

    public Checklist actualizarChecklist(Long id, Checklist checklistActualizado) {
        Checklist checklistExistente = checklistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist no encontrado"));

        validarChecklist(checklistActualizado);

        String estado = checklistActualizado.getEstado();
        if (estado == null || estado.trim().isEmpty()) {
            throw new BadRequestException("El estado del checklist es obligatorio");
        }

        estado = normalizarEstado(estado);
        validarEstado(estado);

        Servicio servicio = servicioRepository.findById(checklistActualizado.getServicio().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        checklistExistente.setNombre(checklistActualizado.getNombre().trim());
        checklistExistente.setDescripcion(
                checklistActualizado.getDescripcion() != null
                        ? checklistActualizado.getDescripcion().trim()
                        : null
        );
        checklistExistente.setEstado(estado);
        checklistExistente.setServicio(servicio);

        return checklistRepository.save(checklistExistente);
    }

    public void eliminarChecklist(Long id) {
        if (!checklistRepository.existsById(id)) {
            throw new ResourceNotFoundException("Checklist no encontrado");
        }

        checklistRepository.deleteById(id);
    }

    private void validarChecklist(Checklist checklist) {
        if (checklist.getNombre() == null || checklist.getNombre().trim().isEmpty()) {
            throw new BadRequestException("El nombre del checklist es obligatorio");
        }

        if (checklist.getServicio() == null || checklist.getServicio().getId() == null) {
            throw new BadRequestException("El servicio es obligatorio");
        }
    }

    private void validarEstado(String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new BadRequestException(
                    "Estado no válido. Use: PENDIENTE, EN_PROCESO o COMPLETADO"
            );
        }
    }

    private String normalizarEstado(String estado) {
        return estado.trim().toUpperCase();
    }
}
