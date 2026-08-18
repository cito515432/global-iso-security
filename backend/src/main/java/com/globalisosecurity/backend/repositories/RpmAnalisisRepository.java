package com.globalisosecurity.backend.repositories;

import com.globalisosecurity.backend.models.RpmAnalisis;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RpmAnalisisRepository extends JpaRepository<RpmAnalisis, Long> {
    List<RpmAnalisis> findByServicioIdOrderByPuntajeDescGeneradoEnDesc(Long servicioId);
    List<RpmAnalisis> findByServicioIdOrderByGeneradoEnDesc(Long servicioId);
    List<RpmAnalisis> findByServicioEmpresaIdOrderByPuntajeDescGeneradoEnDesc(Long empresaId);
    List<RpmAnalisis> findByServicioEmpresaIdOrderByGeneradoEnDesc(Long empresaId);
    Optional<RpmAnalisis> findFirstBySoaControlIdOrderByGeneradoEnDesc(Long soaControlId);
    long countByServicioIdAndPrioridadInAndEstadoNot(Long servicioId, List<String> prioridades, String estado);
}
