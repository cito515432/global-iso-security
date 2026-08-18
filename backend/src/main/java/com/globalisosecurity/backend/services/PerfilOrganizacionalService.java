package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.PerfilOrganizacionalRequest;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.models.PerfilOrganizacional;
import com.globalisosecurity.backend.models.Sector;
import com.globalisosecurity.backend.repositories.EmpresaRepository;
import com.globalisosecurity.backend.repositories.PerfilOrganizacionalRepository;
import com.globalisosecurity.backend.repositories.SectorRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilOrganizacionalService {
    private final PerfilOrganizacionalRepository repository;
    private final EmpresaRepository empresaRepository;
    private final SectorRepository sectorRepository;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;

    public PerfilOrganizacionalService(PerfilOrganizacionalRepository repository, EmpresaRepository empresaRepository,
            SectorRepository sectorRepository, AccesoEmpresaService acceso, LogAuditoriaService logs) {
        this.repository = repository; this.empresaRepository = empresaRepository; this.sectorRepository = sectorRepository;
        this.acceso = acceso; this.logs = logs;
    }

    public PerfilOrganizacional obtener(Long empresaId) {
        acceso.validarEmpresa(empresaId);
        return repository.findByEmpresaId(empresaId).orElseGet(() -> crearBase(empresaId));
    }

    @Transactional
    public PerfilOrganizacional guardar(Long empresaId, PerfilOrganizacionalRequest r) {
        acceso.validarEmpresa(empresaId);
        PerfilOrganizacional p = repository.findByEmpresaId(empresaId).orElseGet(() -> crearBaseSinGuardar(empresaId));
        if (r != null) {
            if (r.sectorId() != null) {
                Sector s = sectorRepository.findById(r.sectorId()).orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado"));
                p.setSector(s);
            }
            if (r.tamano() != null && !r.tamano().isBlank()) p.setTamano(r.tamano().trim().toUpperCase());
            p.setManejaDatosSensibles(Boolean.TRUE.equals(r.manejaDatosSensibles()));
            p.setUsaServiciosNube(Boolean.TRUE.equals(r.usaServiciosNube()));
            p.setPermiteTrabajoRemoto(Boolean.TRUE.equals(r.permiteTrabajoRemoto()));
            p.setProcesaPagos(Boolean.TRUE.equals(r.procesaPagos()));
            p.setInfraestructuraPropia(Boolean.TRUE.equals(r.infraestructuraPropia()));
            p.setDependeProveedores(Boolean.TRUE.equals(r.dependeProveedores()));
            p.setServicioCritico24x7(Boolean.TRUE.equals(r.servicioCritico24x7()));
            p.setManejaMenores(Boolean.TRUE.equals(r.manejaMenores()));
            p.setOperaOtIot(Boolean.TRUE.equals(r.operaOtIot()));
            p.setAlcanceSgsi(trim(r.alcanceSgsi()));
            p.setResponsableSgsi(trim(r.responsableSgsi()));
            if (r.umbralAceptacion() != null) p.setUmbralAceptacion(Math.max(1, Math.min(25, r.umbralAceptacion())));
        }
        p.setActualizadoEn(LocalDateTime.now());
        PerfilOrganizacional saved = repository.save(p);
        logs.registrarLog("ACTUALIZAR", "CONTEXTO", "Se actualizó el contexto de la empresa " + empresaId);
        return saved;
    }

    private PerfilOrganizacional crearBase(Long empresaId) { return repository.save(crearBaseSinGuardar(empresaId)); }
    private PerfilOrganizacional crearBaseSinGuardar(Long empresaId) {
        Empresa e = empresaRepository.findById(empresaId).orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        PerfilOrganizacional p = new PerfilOrganizacional(); p.setEmpresa(e); return p;
    }
    private String trim(String s) { return s == null ? null : s.trim(); }
}
