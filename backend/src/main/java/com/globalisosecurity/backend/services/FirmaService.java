package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.FirmaCreateRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Firma;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.FirmaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FirmaService {
    private static final Set<String> ESTADOS_VALIDOS = Set.of("PENDIENTE", "FIRMADA", "RECHAZADA");
    private final FirmaRepository repository;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;

    public FirmaService(FirmaRepository repository, AccesoEmpresaService acceso, LogAuditoriaService logs) {
        this.repository = repository;
        this.acceso = acceso;
        this.logs = logs;
    }

    public List<Firma> obtenerTodas() {
        Usuario u = acceso.usuarioActual();
        if (acceso.esAdministrador()) return repository.findAll();
        return u.getEmpresa() == null ? List.of() : repository.findByServicioEmpresaId(u.getEmpresa().getId());
    }

    public Optional<Firma> obtenerPorId(Long id) {
        Optional<Firma> f = repository.findById(id);
        f.ifPresent(x -> acceso.servicioAutorizado(x.getServicio().getId()));
        return f;
    }

    public List<Firma> obtenerPorEstado(String estado) {
        String e = normalizar(estado);
        return obtenerTodas().stream().filter(x -> e.equals(x.getEstado())).toList();
    }

    public List<Firma> obtenerPorServicio(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        return repository.findByServicioId(servicioId);
    }

    public List<Firma> obtenerPorEmpresa(Long empresaId) {
        acceso.validarEmpresa(empresaId);
        return repository.findByServicioEmpresaId(empresaId);
    }

    @Transactional
    public Firma crearFirma(FirmaCreateRequest q) {
        if (q == null || q.getServicioId() == null) throw new BadRequestException("El servicio es obligatorio");
        if (q.getCargo() == null || q.getCargo().isBlank()) throw new BadRequestException("El cargo es obligatorio");
        Servicio s = acceso.servicioAutorizado(q.getServicioId());
        Usuario actual = acceso.usuarioActual();
        String estado = normalizar(q.getEstado() == null ? "PENDIENTE" : q.getEstado());
        validar(estado);

        Firma f = new Firma();
        // El nombre de la firma proviene de la identidad autenticada, no del body.
        f.setNombreFirmante(actual.getNombre());
        f.setCargo(q.getCargo().trim());
        f.setEstado(estado);
        f.setFechaFirma(LocalDateTime.now());
        f.setServicio(s);
        f = repository.save(f);
        logs.registrarLog("FIRMAR", "FIRMAS",
                "Se registró firma " + f.getId() + " por " + actual.getEmail() + " como " + estado);
        return f;
    }

    @Transactional
    public Firma actualizarFirma(Long id, Firma q) {
        Firma f = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Firma no encontrada"));
        acceso.servicioAutorizado(f.getServicio().getId());
        if ("FIRMADA".equalsIgnoreCase(f.getEstado())) {
            throw new BadRequestException("Una firma confirmada es inmutable");
        }
        if (q == null) throw new BadRequestException("El cuerpo es obligatorio");
        String estado = normalizar(q.getEstado());
        validar(estado);
        // Nombre, fecha, cargo y servicio son parte de la evidencia original y no se reescriben.
        f.setEstado(estado);
        Firma updated = repository.save(f);
        logs.registrarLog("ACTUALIZAR", "FIRMAS", "Se actualizó el estado de firma " + id + " a " + estado);
        return updated;
    }

    @Transactional
    public void eliminarFirma(Long id) {
        Firma f = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Firma no encontrada"));
        acceso.servicioAutorizado(f.getServicio().getId());
        if (!"PENDIENTE".equalsIgnoreCase(f.getEstado())) {
            throw new BadRequestException("Solo se pueden eliminar firmas pendientes; las firmas validadas son evidencia permanente");
        }
        repository.delete(f);
        logs.registrarLog("ELIMINAR", "FIRMAS", "Se eliminó firma pendiente " + id);
    }

    private String normalizar(String e) {
        if (e == null || e.isBlank()) throw new BadRequestException("El estado es obligatorio");
        return e.trim().toUpperCase();
    }

    private void validar(String e) {
        if (!ESTADOS_VALIDOS.contains(e)) throw new BadRequestException("Use PENDIENTE, FIRMADA o RECHAZADA");
    }
}
