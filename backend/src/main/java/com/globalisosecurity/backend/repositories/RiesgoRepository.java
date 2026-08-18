package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.Riesgo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RiesgoRepository extends JpaRepository<Riesgo,Long>{ List<Riesgo> findByServicioIdOrderByNivelInherenteDesc(Long servicioId); Optional<Riesgo> findByServicioIdAndCodigo(Long servicioId,String codigo); List<Riesgo> findByServicioEmpresaId(Long empresaId); long countByServicioIdAndEstado(Long servicioId,String estado); }
