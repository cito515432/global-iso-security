/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Evaluacion;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.repositories.EvaluacionRepository;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EvaluacionService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE",
            "EN_REVISION",
            "APROBADA",
            "RECHAZADA"
    );

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    public List<Evaluacion> obtenerTodas() {
        return evaluacionRepository.findAll();
    }

    public Optional<Evaluacion> obtenerPorId(Long id) {
        return evaluacionRepository.findById(id);
    }

    public List<Evaluacion> obtenerPorEstado(String estado) {
        return evaluacionRepository.findByEstado(normalizarEstado(estado));
    }

    public List<Evaluacion> obtenerPorServicio(Long servicioId) {
        return evaluacionRepository.findByServicioId(servicioId);
    }

    public Evaluacion crearEvaluacion(Evaluacion evaluacion) {
        validarEvaluacion(evaluacion);

        if (evaluacion.getFechaEvaluacion() == null) {
            evaluacion.setFechaEvaluacion(LocalDateTime.now());
        }

        String estado = evaluacion.getEstado();
        if (estado == null || estado.trim().isEmpty()) {
            estado = "PENDIENTE";
        }

        estado = normalizarEstado(estado);
        validarEstado(estado);

        Servicio servicio = servicioRepository.findById(evaluacion.getServicio().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        evaluacion.setResultadoGeneral(evaluacion.getResultadoGeneral().trim());
        evaluacion.setComentarios(
                evaluacion.getComentarios() != null ? evaluacion.getComentarios().trim() : null
        );
        evaluacion.setEstado(estado);
        evaluacion.setServicio(servicio);

        return evaluacionRepository.save(evaluacion);
    }

    public Evaluacion actualizarEvaluacion(Long id, Evaluacion evaluacionActualizada) {
        Evaluacion evaluacionExistente = evaluacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada"));

        validarEvaluacion(evaluacionActualizada);

        String estado = evaluacionActualizada.getEstado();
        if (estado == null || estado.trim().isEmpty()) {
            throw new BadRequestException("El estado de la evaluación es obligatorio");
        }

        estado = normalizarEstado(estado);
        validarEstado(estado);

        Servicio servicio = servicioRepository.findById(evaluacionActualizada.getServicio().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        evaluacionExistente.setResultadoGeneral(evaluacionActualizada.getResultadoGeneral().trim());
        evaluacionExistente.setComentarios(
                evaluacionActualizada.getComentarios() != null
                        ? evaluacionActualizada.getComentarios().trim()
                        : null
        );
        evaluacionExistente.setEstado(estado);
        evaluacionExistente.setServicio(servicio);

        return evaluacionRepository.save(evaluacionExistente);
    }

    public void eliminarEvaluacion(Long id) {
        if (!evaluacionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Evaluación no encontrada");
        }

        evaluacionRepository.deleteById(id);
    }

    private void validarEvaluacion(Evaluacion evaluacion) {
        if (evaluacion.getResultadoGeneral() == null || evaluacion.getResultadoGeneral().trim().isEmpty()) {
            throw new BadRequestException("El resultado general es obligatorio");
        }

        if (evaluacion.getServicio() == null || evaluacion.getServicio().getId() == null) {
            throw new BadRequestException("El servicio es obligatorio");
        }
    }

    private void validarEstado(String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new BadRequestException(
                    "Estado no válido. Use: PENDIENTE, EN_REVISION, APROBADA o RECHAZADA"
            );
        }
    }

    private String normalizarEstado(String estado) {
        return estado.trim().toUpperCase();
    }
}
