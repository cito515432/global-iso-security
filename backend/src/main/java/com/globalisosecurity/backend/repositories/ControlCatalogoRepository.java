package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.ControlCatalogo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ControlCatalogoRepository extends JpaRepository<ControlCatalogo,Long>{ Optional<ControlCatalogo> findByCodigo(String codigo); List<ControlCatalogo> findByActivoTrueOrderByCodigoAsc(); List<ControlCatalogo> findByDominioAndActivoTrueOrderByCodigoAsc(String dominio); }
