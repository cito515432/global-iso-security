package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.models.ControlCatalogo;
import com.globalisosecurity.backend.repositories.ControlCatalogoRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoControlesService {
    private final ControlCatalogoRepository repository;
    public CatalogoControlesService(ControlCatalogoRepository repository) { this.repository = repository; }

    public List<ControlCatalogo> listar(String dominio) {
        if (dominio == null || dominio.isBlank()) return repository.findByActivoTrueOrderByCodigoAsc();
        return repository.findByDominioAndActivoTrueOrderByCodigoAsc(dominio.trim());
    }

    public ControlCatalogo obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new BadRequestException("Control no encontrado"));
    }

    @Transactional
    public int cargarCatalogoBase() {
        int creados = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/iso27001_controls.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 6) continue;
                if (repository.findByCodigo(p[0]).isPresent()) continue;
                ControlCatalogo c = new ControlCatalogo();
                c.setCodigo(p[0]); c.setDominio(p[1]); c.setTitulo(p[2]); c.setDescripcion(p[3]);
                c.setPreguntaEvaluacion(p[4]); c.setEtiquetas(p[5]); c.setActivo(true);
                repository.save(c); creados++;
            }
            return creados;
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible cargar el catálogo base de controles", ex);
        }
    }
}
