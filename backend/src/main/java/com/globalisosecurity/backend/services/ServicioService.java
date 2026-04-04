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
import com.globalisosecurity.backend.dto.ServicioResponseDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.globalisosecurity.backend.models.Checklist;
import com.globalisosecurity.backend.models.Usuario;
import com.globalisosecurity.backend.repositories.ChecklistRepository;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import com.globalisosecurity.backend.utils.SecurityUtils;
import com.globalisosecurity.backend.models.ItemChecklist;
import com.globalisosecurity.backend.repositories.ItemChecklistRepository;
import java.util.HashMap;
import java.util.Map;
import com.globalisosecurity.backend.models.Firma;
import com.globalisosecurity.backend.repositories.FirmaRepository;


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
private UsuarioRepository usuarioRepository;
@Autowired
private ItemChecklistRepository itemChecklistRepository;
@Autowired
private ChecklistRepository checklistRepository;
@Autowired
private FirmaRepository firmaRepository;
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
public List<ServicioResponseDTO> listarServiciosDTO(Long empresaId) {
    List<Servicio> servicios;

    if (empresaId != null) {
        servicios = servicioRepository.findByEmpresaId(empresaId);
    } else {
        servicios = servicioRepository.findAll();
    }

    return servicios.stream()
            .map(this::mapServicio)
            .toList();
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
private ServicioResponseDTO mapServicio(Servicio servicio) {
    ServicioResponseDTO dto = new ServicioResponseDTO();
    dto.setId(servicio.getId());
    dto.setEstado(servicio.getEstado());
    dto.setFechaCreacion(servicio.getFechaCreacion());

    if (servicio.getEmpresa() != null) {
        dto.setEmpresa(new ServicioResponseDTO.SimpleRef(
                servicio.getEmpresa().getId(),
                servicio.getEmpresa().getNombre()
        ));
    } else {
        dto.setEmpresa(null);
    }

    if (servicio.getSector() != null) {
        dto.setSector(new ServicioResponseDTO.SimpleRef(
                servicio.getSector().getId(),
                servicio.getSector().getNombre()
        ));
    } else {
        dto.setSector(null);
    }

    return dto;
}
public Map<String, Object> obtenerMiServicio() {
    String email = SecurityUtils.getUsuarioActual();

    Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

    if (usuario.getEmpresa() == null) {
        throw new BadRequestException("El usuario no tiene empresa asignada");
    }

    List<Servicio> servicios = servicioRepository.findByEmpresaId(usuario.getEmpresa().getId());
    if (servicios.isEmpty()) {
        throw new ResourceNotFoundException("No se encontró servicio para la empresa del usuario");
    }

    Servicio servicio = servicios.get(0);

    List<Checklist> checklists = checklistRepository.findByServicioId(servicio.getId());
    if (checklists.isEmpty()) {
        throw new ResourceNotFoundException("No se encontró checklist para el servicio");
    }

    Checklist checklist = checklists.get(0);

    Map<String, Object> response = new HashMap<>();
    response.put("servicioId", servicio.getId());
    response.put("estado", servicio.getEstado());
    response.put("empresaNombre", servicio.getEmpresa() != null ? servicio.getEmpresa().getNombre() : null);
    response.put("sectorNombre", servicio.getSector() != null ? servicio.getSector().getNombre() : null);
    response.put("checklistId", checklist.getId());

    return response;
}
public Map<String, Object> obtenerResumen(Long servicioId) {
    Servicio servicio = servicioRepository.findById(servicioId)
            .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

    List<Checklist> checklists = checklistRepository.findByServicioId(servicio.getId());
    if (checklists.isEmpty()) {
        throw new ResourceNotFoundException("No se encontró checklist para el servicio");
    }

    Checklist checklist = checklists.get(0);

    List<ItemChecklist> items = itemChecklistRepository.findByChecklistId(checklist.getId());

    int total = items.size();
    int cumple = 0;
    int noCumple = 0;
    int enProceso = 0;
    int pendiente = 0;

    for (ItemChecklist item : items) {
        String estado = item.getEstado() != null ? item.getEstado().trim().toUpperCase() : "PENDIENTE";

        switch (estado) {
            case "CUMPLE" -> cumple++;
            case "NO_CUMPLE" -> noCumple++;
            case "EN_PROCESO" -> enProceso++;
            default -> pendiente++;
        }
    }

    int porcentajeCumplimiento = total == 0 ? 0 : Math.round((cumple * 100.0f) / total);

    Map<String, Object> response = new HashMap<>();
    response.put("total", total);
    response.put("cumple", cumple);
    response.put("noCumple", noCumple);
    response.put("enProceso", enProceso);
    response.put("pendiente", pendiente);
    response.put("porcentajeCumplimiento", porcentajeCumplimiento);

    return response;
}

public Map<String, Object> obtenerEstadoCompleto(Long servicioId) {
    Servicio servicio = servicioRepository.findById(servicioId)
            .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

    List<Firma> firmas = firmaRepository.findByServicioId(servicioId);

    List<Map<String, Object>> firmasResponse = firmas.stream().map(firma -> {
        Map<String, Object> firmaMap = new HashMap<>();
        firmaMap.put("cargo", firma.getCargo());
        firmaMap.put("estado", firma.getEstado());
        firmaMap.put("fechaFirma", firma.getFechaFirma());
        return firmaMap;
    }).toList();

    Map<String, Object> response = new HashMap<>();
    response.put("servicioId", servicio.getId());
    response.put("estado", servicio.getEstado());
    response.put("firmas", firmasResponse);

    return response;
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