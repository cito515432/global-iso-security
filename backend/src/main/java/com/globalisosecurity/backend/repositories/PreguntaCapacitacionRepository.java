package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.PreguntaCapacitacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PreguntaCapacitacionRepository extends JpaRepository<PreguntaCapacitacion,Long>{
    List<PreguntaCapacitacion> findByCapacitacionIdOrderByOrdenAsc(Long capacitacionId);
    List<PreguntaCapacitacion> findByCapacitacionIdAndActivaTrueOrderByOrdenAsc(Long capacitacionId);
}
