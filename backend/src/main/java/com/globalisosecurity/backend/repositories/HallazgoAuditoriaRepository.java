package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.HallazgoAuditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HallazgoAuditoriaRepository extends JpaRepository<HallazgoAuditoria,Long>{ List<HallazgoAuditoria> findByServicioIdOrderByFechaDeteccionDesc(Long servicioId); List<HallazgoAuditoria> findBySoaControlIdAndEstadoNot(Long soaControlId,String estado); long countByServicioIdAndEstado(Long servicioId,String estado); long countBySoaControlIdAndRecurrenteTrueAndEstadoNot(Long soaControlId,String estado); }
