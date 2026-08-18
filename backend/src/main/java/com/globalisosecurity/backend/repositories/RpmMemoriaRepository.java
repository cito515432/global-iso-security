package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.RpmMemoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RpmMemoriaRepository extends JpaRepository<RpmMemoria,Long>{ List<RpmMemoria> findTop10ByHuellaOrderByCreadoEnDesc(String huella); List<RpmMemoria> findByAnalisisServicioIdOrderByCreadoEnDesc(Long servicioId); long countByAnalisisServicioId(Long servicioId); }
