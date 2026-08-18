package com.globalisosecurity.backend.repositories;

import com.globalisosecurity.backend.models.IntentoCapacitacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentoCapacitacionRepository extends JpaRepository<IntentoCapacitacion, Long> {
    List<IntentoCapacitacion> findByParticipanteIdOrderByFechaIntentoDesc(Long participanteId);
}
