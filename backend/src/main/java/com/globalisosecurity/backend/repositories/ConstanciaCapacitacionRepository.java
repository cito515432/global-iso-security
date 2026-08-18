package com.globalisosecurity.backend.repositories;

import com.globalisosecurity.backend.models.ConstanciaCapacitacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConstanciaCapacitacionRepository extends JpaRepository<ConstanciaCapacitacion, Long> {
    List<ConstanciaCapacitacion> findByServicioId(Long servicioId);
    List<ConstanciaCapacitacion> findByCapacitacionId(Long capacitacionId);
    List<ConstanciaCapacitacion> findByDocumento(String documento);
    Optional<ConstanciaCapacitacion> findByParticipanteId(Long participanteId);
    Optional<ConstanciaCapacitacion> findByCodigoVerificacion(String codigoVerificacion);
    boolean existsByParticipanteId(Long participanteId);
}
