/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.models.Sector;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.repositories.EmpresaRepository;
import com.globalisosecurity.backend.repositories.SectorRepository;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ServicioService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "BORRADOR",
            "EN_PROCESO",
            "FINALIZADO",
            "FIRMADO",
            "CERRADO"
    );

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    public List<Servicio> obtenerTodos() {
        return servicioRepository.findAll();
    }

    public Optional<Servicio> obtenerPorId(Long id) {
        return servicioRepository.findById(id);
    }

    public List<Servicio> obtenerPorEstado(String estado) {
        return servicioRepository.findByEstado(normalizarEstado(estado));
    }

    public List<Servicio> obtenerPorEmpresa(Long empresaId) {
        return servicioRepository.findByEmpresaId(empresaId);
    }

    public Servicio crearServicio(Servicio servicio) {
        validarEmpresaYSector(servicio);

        if (servicio.getFechaCreacion() == null) {
            servicio.setFechaCreacion(LocalDateTime.now());
        }

        String estado = servicio.getEstado();
        if (estado == null || estado.trim().isEmpty()) {
            estado = "BORRADOR";
        }

        estado = normalizarEstado(estado);
        validarEstado(estado);
        servicio.setEstado(estado);

        Empresa empresa = empresaRepository.findById(servicio.getEmpresa().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Sector sector = sectorRepository.findById(servicio.getSector().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado"));

        servicio.setEmpresa(empresa);
        servicio.setSector(sector);

        Servicio nuevo = servicioRepository.save(servicio);

        logAuditoriaService.registrarLog(
                "CREAR",
                "SERVICIOS",
                "Se creó el servicio con ID: " + nuevo.getId() + " en estado " + nuevo.getEstado()
        );

        return nuevo;
    }

    public Servicio actualizarServicio(Long id, Servicio servicioActualizado) {
        Servicio servicioExistente = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        if (estaBloqueado(servicioExistente.getEstado())) {
            throw new BadRequestException("No se puede modificar un servicio en estado " + servicioExistente.getEstado());
        }

        validarEmpresaYSector(servicioActualizado);

        String estado = servicioActualizado.getEstado();
        if (estado == null || estado.trim().isEmpty()) {
            throw new BadRequestException("El estado del servicio es obligatorio");
        }

        estado = normalizarEstado(estado);
        validarEstado(estado);

        Empresa empresa = empresaRepository.findById(servicioActualizado.getEmpresa().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        Sector sector = sectorRepository.findById(servicioActualizado.getSector().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado"));

        servicioExistente.setEmpresa(empresa);
        servicioExistente.setSector(sector);
        servicioExistente.setEstado(estado);

        Servicio actualizado = servicioRepository.save(servicioExistente);

        logAuditoriaService.registrarLog(
                "ACTUALIZAR",
                "SERVICIOS",
                "Se actualizó el servicio con ID: " + actualizado.getId() + " al estado " + actualizado.getEstado()
        );

        return actualizado;
    }

    public void eliminarServicio(Long id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        if (estaBloqueado(servicio.getEstado())) {
            throw new BadRequestException("No se puede eliminar un servicio en estado " + servicio.getEstado());
        }

        servicioRepository.delete(servicio);

        logAuditoriaService.registrarLog(
                "ELIMINAR",
                "SERVICIOS",
                "Se eliminó el servicio con ID: " + servicio.getId() + " en estado " + servicio.getEstado()
        );
    }

    private void validarEmpresaYSector(Servicio servicio) {
        if (servicio == null) {
            throw new BadRequestException("El body del servicio es obligatorio");
        }

        if (servicio.getEmpresa() == null || servicio.getEmpresa().getId() == null) {
            throw new BadRequestException("La empresa es obligatoria");
        }

        if (servicio.getSector() == null || servicio.getSector().getId() == null) {
            throw new BadRequestException("El sector es obligatorio");
        }
    }

    private void validarEstado(String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new BadRequestException(
                    "Estado no válido. Use: BORRADOR, EN_PROCESO, FINALIZADO, FIRMADO o CERRADO"
            );
        }
    }

    private String normalizarEstado(String estado) {
        return estado.trim().toUpperCase();
    }

    private boolean estaBloqueado(String estado) {
        String estadoNormalizado = normalizarEstado(estado);
        return estadoNormalizado.equals("FIRMADO") || estadoNormalizado.equals("CERRADO");
    }
}