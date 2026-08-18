package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.PerfilOrganizacional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PerfilOrganizacionalRepository extends JpaRepository<PerfilOrganizacional,Long>{ Optional<PerfilOrganizacional> findByEmpresaId(Long empresaId); }
