package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.ModuloCapacitacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ModuloCapacitacionRepository extends JpaRepository<ModuloCapacitacion,Long>{ List<ModuloCapacitacion> findByCapacitacionIdOrderByOrdenAsc(Long capacitacionId); }
