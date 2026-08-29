package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.EvidenciaValidacionRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Evidencia;
import com.globalisosecurity.backend.models.SoaControl;
import com.globalisosecurity.backend.repositories.EvidenciaRepository;
import com.globalisosecurity.backend.repositories.SoaControlRepository;
import com.globalisosecurity.backend.utils.SecurityUtils;
import java.io.BufferedInputStream;
import java.io.DigestInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EvidenciaService {

    private static final Logger log = LoggerFactory.getLogger(EvidenciaService.class);
    private static final Set<String> ESTADOS = Set.of("PENDIENTE", "VALIDADA", "RECHAZADA");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "txt", "csv", "docx", "xlsx"
    );
    private static final Map<String, String> CANONICAL_MIME = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "txt", "text/plain",
            "csv", "text/csv",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final EvidenciaRepository repository;
    private final SoaControlRepository soaRepository;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;
    private final Path storage;

    public record Descarga(Evidencia evidencia, Resource recurso) {}

    public EvidenciaService(EvidenciaRepository repository, SoaControlRepository soaRepository,
            AccesoEmpresaService acceso, LogAuditoriaService logs,
            @Value("${app.storage.evidencias:./storage/evidencias}") String storageDir) {
        this.repository = repository;
        this.soaRepository = soaRepository;
        this.acceso = acceso;
        this.logs = logs;
        this.storage = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storage);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear el directorio de evidencias", e);
        }
    }

    public List<Evidencia> listarPorServicio(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        return repository.findByServicioIdOrderByFechaCargaDesc(servicioId);
    }

    public List<Evidencia> listarPorControl(Long soaId) {
        SoaControl s = soaRepository.findById(soaId)
                .orElseThrow(() -> new ResourceNotFoundException("Control SoA no encontrado"));
        acceso.servicioAutorizado(s.getServicio().getId());
        return repository.findBySoaControlIdOrderByFechaCargaDesc(soaId);
    }

    @Transactional
    public Evidencia cargar(Long soaId, MultipartFile archivo, String descripcion, String tipo, LocalDate vencimiento) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("Debe seleccionar un archivo");
        }
        if (archivo.getSize() > 25L * 1024 * 1024) {
            throw new BadRequestException("El archivo supera el límite de 25 MB");
        }

        SoaControl s = soaRepository.findById(soaId)
                .orElseThrow(() -> new ResourceNotFoundException("Control SoA no encontrado"));
        acceso.servicioAutorizado(s.getServicio().getId());

        String original = archivo.getOriginalFilename() == null
                ? "evidencia"
                : Paths.get(archivo.getOriginalFilename()).getFileName().toString();
        String extension = extension(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Tipo de archivo no permitido. Use PDF, PNG, JPG, TXT, CSV, DOCX o XLSX");
        }
        validateDeclaredMime(archivo.getContentType(), extension);

        String stored = UUID.randomUUID() + "." + extension;
        Path target = storage.resolve(stored).normalize();
        if (!target.startsWith(storage)) {
            throw new BadRequestException("Nombre de archivo no válido");
        }

        try (BufferedInputStream buffered = new BufferedInputStream(archivo.getInputStream())) {
            buffered.mark(8192);
            byte[] header = buffered.readNBytes(8192);
            buffered.reset();
            validateSignature(extension, header);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInput = new DigestInputStream(buffered, digest)) {
                Files.copy(digestInput, target);
            }
            validateOfficeContainer(extension, target);

            Evidencia e = new Evidencia();
            e.setServicio(s.getServicio());
            e.setSoaControl(s);
            e.setNombreOriginal(original);
            e.setNombreAlmacenado(stored);
            e.setRutaArchivo(target.toString());
            e.setTipoMime(CANONICAL_MIME.get(extension));
            e.setHashSha256(HexFormat.of().formatHex(digest.digest()));
            e.setDescripcion(trim(descripcion));
            e.setTipoEvidencia(tipo == null || tipo.isBlank() ? "DOCUMENTO" : tipo.trim().toUpperCase());
            e.setFechaVencimiento(vencimiento);
            e.setCargadaPor(SecurityUtils.getUsuarioActual());
            e.setEstado("PENDIENTE");
            Evidencia guardada = repository.save(e);
            logs.registrarLog("CARGAR", "EVIDENCIAS", "Se cargó evidencia para " + s.getControl().getCodigo());
            return guardada;
        } catch (BadRequestException ex) {
            deleteQuietly(target);
            throw ex;
        } catch (Exception ex) {
            deleteQuietly(target);
            log.warn("No fue posible almacenar una evidencia para SoA {}: {}", soaId, ex.getMessage());
            throw new BadRequestException("No fue posible almacenar la evidencia");
        }
    }

    @Transactional
    public Evidencia validar(Long id, EvidenciaValidacionRequest req) {
        Evidencia e = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidencia no encontrada"));
        acceso.servicioAutorizado(e.getServicio().getId());
        if (req == null || req.estado() == null) throw new BadRequestException("El estado es obligatorio");
        String estado = req.estado().trim().toUpperCase();
        if (!ESTADOS.contains(estado) || "PENDIENTE".equals(estado)) {
            throw new BadRequestException("Use VALIDADA o RECHAZADA");
        }
        if ("RECHAZADA".equals(estado) && (req.observacion() == null || req.observacion().isBlank())) {
            throw new BadRequestException("La observación es obligatoria al rechazar");
        }
        e.setEstado(estado);
        e.setValidadaPor(SecurityUtils.getUsuarioActual());
        e.setFechaValidacion(LocalDateTime.now());
        e.setObservacionValidacion(trim(req.observacion()));
        repository.save(e);
        logs.registrarLog("VALIDAR", "EVIDENCIAS", "Evidencia " + id + " marcada como " + estado);
        return e;
    }

    public Descarga descargar(Long id) {
        Evidencia e = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidencia no encontrada"));
        acceso.servicioAutorizado(e.getServicio().getId());
        Path p = Paths.get(e.getRutaArchivo()).toAbsolutePath().normalize();
        if (!p.startsWith(storage)) throw new BadRequestException("La ruta de la evidencia no es válida");
        if (!Files.exists(p) || !Files.isRegularFile(p)) {
            throw new ResourceNotFoundException("El archivo físico no está disponible");
        }
        return new Descarga(e, new FileSystemResource(p));
    }

    @Transactional
    public void eliminar(Long id) {
        Evidencia e = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidencia no encontrada"));
        acceso.servicioAutorizado(e.getServicio().getId());
        repository.delete(e);
        try {
            Path p = Paths.get(e.getRutaArchivo()).toAbsolutePath().normalize();
            if (p.startsWith(storage)) Files.deleteIfExists(p);
        } catch (Exception ignored) {
            // La eliminación lógica de la referencia no debe exponer información interna.
        }
    }

    private void validateDeclaredMime(String contentType, String extension) {
        if (contentType == null || contentType.isBlank()) return;
        String mime = contentType.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
        if ("application/octet-stream".equals(mime)) return;
        String expected = CANONICAL_MIME.get(extension);
        boolean csvAlias = "csv".equals(extension)
                && ("application/vnd.ms-excel".equals(mime) || "text/plain".equals(mime));
        if (!expected.equals(mime) && !csvAlias) {
            throw new BadRequestException("El tipo declarado del archivo no coincide con su extensión");
        }
    }

    private void validateSignature(String extension, byte[] header) {
        boolean valid = switch (extension) {
            case "pdf" -> startsWith(header, new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}); // %PDF-
            case "png" -> startsWith(header, new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "jpg", "jpeg" -> startsWith(header, new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF});
            case "docx", "xlsx" -> startsWith(header, new byte[]{0x50, 0x4B});
            case "txt", "csv" -> !containsNullByte(header);
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException("El contenido del archivo no coincide con un formato permitido");
        }
    }

    private void validateOfficeContainer(String extension, Path file) {
        if (!"docx".equals(extension) && !"xlsx".equals(extension)) return;
        try (ZipFile zip = new ZipFile(file.toFile())) {
            String requiredEntry = "docx".equals(extension) ? "word/document.xml" : "xl/workbook.xml";
            if (zip.getEntry(requiredEntry) == null || zip.getEntry("[Content_Types].xml") == null) {
                throw new BadRequestException("El documento Office no tiene una estructura válida");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("El documento Office no tiene una estructura válida");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean containsNullByte(byte[] value) {
        for (byte b : value) if (b == 0) return true;
        return false;
    }

    private String extension(String name) {
        int pos = name.lastIndexOf('.');
        if (pos < 0 || pos == name.length() - 1) return "";
        return name.substring(pos + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (Exception ignored) {
        }
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
