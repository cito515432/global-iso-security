package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.ParticipanteCapacitacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ParticipanteCapacitacionRepository extends JpaRepository<ParticipanteCapacitacion,Long>{ List<ParticipanteCapacitacion> findByCapacitacionIdOrderByNombreAsc(Long capacitacionId); List<ParticipanteCapacitacion> findByCapacitacionServicioId(Long servicioId); Optional<ParticipanteCapacitacion> findByCapacitacionIdAndEmail(Long capacitacionId,String email); }
