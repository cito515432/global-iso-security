package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.RpmDecision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RpmDecisionRepository extends JpaRepository<RpmDecision,Long>{ List<RpmDecision> findByAnalisisId(Long analisisId); List<RpmDecision> findByAnalisisServicioIdAndEstadoOrderByIdDesc(Long servicioId,String estado); long countByAnalisisServicioIdAndEstado(Long servicioId,String estado); }
