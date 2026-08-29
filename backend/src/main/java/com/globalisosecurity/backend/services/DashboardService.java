package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.DashboardResumenDTO;
import com.globalisosecurity.backend.repositories.ChecklistRepository;
import com.globalisosecurity.backend.repositories.EmpresaRepository;
import com.globalisosecurity.backend.repositories.EvaluacionRepository;
import com.globalisosecurity.backend.repositories.FirmaRepository;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import com.globalisosecurity.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ServicioRepository servicioRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final FirmaRepository firmaRepository;
    private final ChecklistRepository checklistRepository;
    private final AccesoEmpresaService accesoEmpresaService;

    public DashboardService(UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository,
            ServicioRepository servicioRepository,
            EvaluacionRepository evaluacionRepository,
            FirmaRepository firmaRepository,
            ChecklistRepository checklistRepository,
            AccesoEmpresaService accesoEmpresaService) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.servicioRepository = servicioRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.firmaRepository = firmaRepository;
        this.checklistRepository = checklistRepository;
        this.accesoEmpresaService = accesoEmpresaService;
    }

    public DashboardResumenDTO obtenerResumen() {
        DashboardResumenDTO resumen = new DashboardResumenDTO();
        resumen.setTotalUsuarios(usuarioRepository.count());
        resumen.setTotalEmpresas(empresaRepository.count());
        resumen.setTotalServicios(servicioRepository.count());
        resumen.setServiciosEnProceso(servicioRepository.findByEstado("EN_PROCESO").size());
        resumen.setServiciosFirmados(servicioRepository.findByEstado("FIRMADO").size());
        resumen.setServiciosCerrados(servicioRepository.findByEstado("CERRADO").size());
        resumen.setTotalEvaluaciones(evaluacionRepository.count());
        resumen.setTotalFirmas(firmaRepository.count());
        resumen.setTotalChecklists(checklistRepository.count());
        return resumen;
    }

    public DashboardResumenDTO obtenerResumenPorEmpresa(Long empresaId) {
        accesoEmpresaService.validarEmpresa(empresaId);
        DashboardResumenDTO resumen = new DashboardResumenDTO();
        resumen.setTotalUsuarios(usuarioRepository.findByEmpresaId(empresaId).size());
        resumen.setTotalEmpresas(1);
        resumen.setTotalServicios(servicioRepository.findByEmpresaId(empresaId).size());
        resumen.setServiciosEnProceso(servicioRepository.findByEmpresaIdAndEstado(empresaId, "EN_PROCESO").size());
        resumen.setServiciosFirmados(servicioRepository.findByEmpresaIdAndEstado(empresaId, "FIRMADO").size());
        resumen.setServiciosCerrados(servicioRepository.findByEmpresaIdAndEstado(empresaId, "CERRADO").size());
        resumen.setTotalEvaluaciones(evaluacionRepository.findByServicioEmpresaId(empresaId).size());
        resumen.setTotalFirmas(firmaRepository.findByServicioEmpresaId(empresaId).size());
        resumen.setTotalChecklists(checklistRepository.findByServicioEmpresaId(empresaId).size());
        return resumen;
    }
}
