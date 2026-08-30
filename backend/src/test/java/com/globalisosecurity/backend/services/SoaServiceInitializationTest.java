package com.globalisosecurity.backend.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.globalisosecurity.backend.models.ControlCatalogo;
import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.repositories.ControlCatalogoRepository;
import com.globalisosecurity.backend.repositories.EvidenciaRepository;
import com.globalisosecurity.backend.repositories.HallazgoAuditoriaRepository;
import com.globalisosecurity.backend.repositories.PerfilOrganizacionalRepository;
import com.globalisosecurity.backend.repositories.RiesgoControlRepository;
import com.globalisosecurity.backend.repositories.SoaControlRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SoaServiceInitializationTest {

    @Mock private SoaControlRepository soaRepository;
    @Mock private ControlCatalogoRepository controlRepository;
    @Mock private PerfilOrganizacionalRepository perfilRepository;
    @Mock private EvidenciaRepository evidenciaRepository;
    @Mock private RiesgoControlRepository riesgoControlRepository;
    @Mock private HallazgoAuditoriaRepository hallazgoRepository;
    @Mock private ControlRelevanciaService relevanciaService;
    @Mock private AccesoEmpresaService acceso;
    @Mock private LogAuditoriaService logs;

    private SoaService service;
    private Servicio servicio;

    @BeforeEach
    void setUp() {
        service = new SoaService(soaRepository, controlRepository, perfilRepository,
                evidenciaRepository, riesgoControlRepository, hallazgoRepository,
                relevanciaService, acceso, logs);
        Empresa empresa = new Empresa();
        empresa.setId(7L);
        servicio = new Servicio();
        servicio.setId(42L);
        servicio.setEmpresa(empresa);
        when(acceso.servicioAutorizado(42L)).thenReturn(servicio);
        when(perfilRepository.findByEmpresaId(7L)).thenReturn(Optional.empty());
        when(soaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(relevanciaService.evaluar(any(), eq(servicio), isNull()))
                .thenReturn(new ControlRelevanciaService.Relevancia(10, "BASE", "Prueba"));
    }

    @Test
    void inicializarCreaUnaSoaPorControlActivo() {
        List<ControlCatalogo> controles = controles(93);
        when(controlRepository.findByActivoTrueOrderByCodigoAsc()).thenReturn(controles);
        when(soaRepository.findByServicioIdAndControlId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(soaRepository.countByServicioId(42L)).thenReturn(93L);

        Map<String, Object> resultado = service.inicializar(42L);

        assertThat(resultado).containsEntry("creados", 93).containsEntry("actualizados", 0)
                .containsEntry("total", 93L);
        verify(soaRepository, times(93)).findByServicioIdAndControlId(42L, anyLong());
        verify(soaRepository, times(93)).save(any());
    }

    @Test
    void listarReparaUnaSoaIncompletaSinDataInitializer() {
        when(controlRepository.findByActivoTrueOrderByCodigoAsc()).thenReturn(controles(93));
        when(soaRepository.countByServicioIdAndControlActivoTrue(42L)).thenReturn(92L);
        when(soaRepository.findByServicioIdAndControlId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(soaRepository.countByServicioId(42L)).thenReturn(93L);
        when(soaRepository.findByServicioIdAndControlActivoTrueOrderByControlCodigoAsc(42L))
                .thenReturn(List.of());

        service.listar(42L);

        verify(soaRepository).countByServicioIdAndControlActivoTrue(42L);
        verify(soaRepository, times(93)).save(any());
    }

    private List<ControlCatalogo> controles(int cantidad) {
        return java.util.stream.IntStream.rangeClosed(1, cantidad).mapToObj(i -> {
            ControlCatalogo control = new ControlCatalogo();
            control.setId((long) i);
            control.setCodigo(String.format("A.%02d", i));
            control.setActivo(true);
            return control;
        }).toList();
    }
}
