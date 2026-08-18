package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.Evidencia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EvidenciaRepository extends JpaRepository<Evidencia,Long>{ List<Evidencia> findByServicioIdOrderByFechaCargaDesc(Long servicioId); List<Evidencia> findBySoaControlIdOrderByFechaCargaDesc(Long soaControlId); long countByServicioIdAndEstado(Long servicioId,String estado); }
