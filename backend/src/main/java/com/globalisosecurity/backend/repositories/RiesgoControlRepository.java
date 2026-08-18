package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.RiesgoControl;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RiesgoControlRepository extends JpaRepository<RiesgoControl,Long>{ List<RiesgoControl> findByRiesgoId(Long riesgoId); List<RiesgoControl> findByControlIdAndRiesgoServicioId(Long controlId,Long servicioId); Optional<RiesgoControl> findByRiesgoIdAndControlId(Long riesgoId,Long controlId); void deleteByRiesgoId(Long riesgoId); }
