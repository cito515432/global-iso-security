package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.dto.DashboardResumenDTO;
import com.globalisosecurity.backend.services.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public DashboardResumenDTO obtenerResumen() {
        return dashboardService.obtenerResumen();
    }

    @GetMapping("/resumen/empresa/{empresaId}")
    public DashboardResumenDTO obtenerResumenPorEmpresa(@PathVariable Long empresaId) {
        return dashboardService.obtenerResumenPorEmpresa(empresaId);
    }
}
