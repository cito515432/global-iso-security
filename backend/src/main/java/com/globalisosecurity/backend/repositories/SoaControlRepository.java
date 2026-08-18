package com.globalisosecurity.backend.repositories;

import com.globalisosecurity.backend.models.SoaControl;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SoaControlRepository extends JpaRepository<SoaControl, Long> {
    List<SoaControl> findByServicioIdOrderByControlCodigoAsc(Long servicioId);
    List<SoaControl> findByServicioIdAndControlActivoTrueOrderByControlCodigoAsc(Long servicioId);
    Optional<SoaControl> findByServicioIdAndControlId(Long servicioId, Long controlId);
    long countByServicioId(Long servicioId);
    long countByServicioIdAndControlActivoTrue(Long servicioId);
    long countByServicioIdAndAplicabilidad(Long servicioId, String aplicabilidad);
    long countByServicioIdAndEstadoImplementacion(Long servicioId, String estado);
}
