package com.globalisosecurity.backend.repositories;
import com.globalisosecurity.backend.models.RpmSenal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RpmSenalRepository extends JpaRepository<RpmSenal,Long>{ List<RpmSenal> findByAnalisisIdOrderByPesoDesc(Long analisisId); void deleteByAnalisisId(Long analisisId); }
