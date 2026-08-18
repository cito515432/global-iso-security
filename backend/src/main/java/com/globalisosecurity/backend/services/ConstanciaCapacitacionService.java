package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.repositories.*;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConstanciaCapacitacionService {
    private final ConstanciaCapacitacionRepository constanciaRepository;
    private final CapacitacionRepository capacitacionRepository;
    private final ServicioRepository servicioRepository;
    private final ParticipanteCapacitacionRepository participanteRepository;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;

    public ConstanciaCapacitacionService(ConstanciaCapacitacionRepository constanciaRepository,
            CapacitacionRepository capacitacionRepository, ServicioRepository servicioRepository,
            ParticipanteCapacitacionRepository participanteRepository, AccesoEmpresaService acceso,
            LogAuditoriaService logs) {
        this.constanciaRepository = constanciaRepository;
        this.capacitacionRepository = capacitacionRepository;
        this.servicioRepository = servicioRepository;
        this.participanteRepository = participanteRepository;
        this.acceso = acceso;
        this.logs = logs;
    }

    public List<ConstanciaCapacitacion> obtenerTodas() {
        Usuario u = acceso.usuarioActual();
        if (acceso.esAdministrador() || (u.getEmpresa() == null && acceso.esRolInternoGlobal())) {
            return constanciaRepository.findAll();
        }
        if (u.getEmpresa() == null) return List.of();
        List<Servicio> servicios = servicioRepository.findByEmpresaId(u.getEmpresa().getId());
        return servicios.stream().flatMap(s -> constanciaRepository.findByServicioId(s.getId()).stream()).toList();
    }

    public Optional<ConstanciaCapacitacion> obtenerPorId(Long id) {
        Optional<ConstanciaCapacitacion> c = constanciaRepository.findById(id);
        c.ifPresent(x -> acceso.servicioAutorizado(x.getServicio().getId()));
        return c;
    }

    public List<ConstanciaCapacitacion> obtenerPorServicio(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        return constanciaRepository.findByServicioId(servicioId);
    }

    public List<ConstanciaCapacitacion> obtenerPorCapacitacion(Long capacitacionId) {
        Capacitacion c = capacitacionRepository.findById(capacitacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Capacitación no encontrada"));
        acceso.servicioAutorizado(c.getServicio().getId());
        return constanciaRepository.findByCapacitacionId(capacitacionId);
    }

    public List<ConstanciaCapacitacion> obtenerPorDocumento(String documento) {
        if (documento == null || documento.isBlank()) throw new BadRequestException("El documento es obligatorio");
        Usuario u = acceso.usuarioActual();
        List<ConstanciaCapacitacion> resultados = constanciaRepository.findByDocumento(documento.trim());
        if (acceso.esAdministrador() || (u.getEmpresa() == null && acceso.esRolInternoGlobal())) return resultados;
        if (u.getEmpresa() == null) return List.of();
        return resultados.stream().filter(c -> u.getEmpresa().getId().equals(c.getServicio().getEmpresa().getId())).toList();
    }

    public ConstanciaCapacitacion verificar(String codigo) {
        ConstanciaCapacitacion c = buscarPorCodigo(codigo);
        acceso.servicioAutorizado(c.getServicio().getId());
        return c;
    }

    /**
     * Verificación pública con información mínima. No expone documento,
     * identificadores internos, relaciones completas ni datos de acceso.
     */
    public Map<String, Object> verificarPublica(String codigo) {
        ConstanciaCapacitacion c = buscarPorCodigo(codigo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("codigoVerificacion", c.getCodigoVerificacion());
        result.put("estado", c.getEstado());
        result.put("nombreCompleto", c.getNombreCompleto());
        result.put("capacitacion", c.getCapacitacion().getTitulo());
        result.put("empresa", c.getServicio().getEmpresa() == null ? "" : c.getServicio().getEmpresa().getNombre());
        result.put("fechaEmision", c.getFechaFirma());
        result.put("puntaje", c.getPuntaje());
        result.put("valida", "VIGENTE".equalsIgnoreCase(c.getEstado()));
        return result;
    }

    private ConstanciaCapacitacion buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new BadRequestException("El código de verificación es obligatorio");
        return constanciaRepository.findByCodigoVerificacion(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No existe una constancia con ese código"));
    }

    @Transactional
    public ConstanciaCapacitacion emitirParaParticipante(ParticipanteCapacitacion participante) {
        if (participante == null || participante.getId() == null) throw new BadRequestException("Participante inválido");
        ParticipanteCapacitacion p = participanteRepository.findById(participante.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));
        acceso.servicioAutorizado(p.getCapacitacion().getServicio().getId());
        return constanciaRepository.findByParticipanteId(p.getId()).orElseGet(() -> {
            if (p.getProgresoPorcentaje() == null || p.getProgresoPorcentaje() < 100) {
                throw new BadRequestException("El participante aún no ha completado la capacitación");
            }
            if (p.getPuntajeEvaluacion() == null || p.getPuntajeEvaluacion() < p.getCapacitacion().getPuntajeMinimo()) {
                throw new BadRequestException("El participante aún no ha aprobado la evaluación");
            }
            if (p.getDocumento() == null || p.getDocumento().isBlank()) {
                throw new BadRequestException("Debe registrar el documento del participante antes de emitir la constancia");
            }
            ConstanciaCapacitacion c = new ConstanciaCapacitacion();
            c.setParticipante(p);
            c.setNombreCompleto(p.getNombre());
            c.setDocumento(p.getDocumento());
            c.setCargo(p.getCargo() == null || p.getCargo().isBlank() ? "Participante" : p.getCargo());
            c.setCodigoInterno("CAP-" + p.getCapacitacion().getId() + "-" + p.getId());
            c.setCodigoVerificacion(nuevoCodigo());
            c.setPuntaje(p.getPuntajeEvaluacion());
            c.setEstado("VIGENTE");
            c.setFechaFirma(LocalDateTime.now());
            c.setCapacitacion(p.getCapacitacion());
            c.setServicio(p.getCapacitacion().getServicio());
            ConstanciaCapacitacion guardada = constanciaRepository.save(c);
            logs.registrarLog("EMITIR", "CONSTANCIAS", "Se emitió la constancia " + guardada.getId() + " para el participante " + p.getId());
            return guardada;
        });
    }

    @Transactional
    public ConstanciaCapacitacion crearConstancia(ConstanciaCapacitacion entrada) {
        validarConstancia(entrada);
        Capacitacion cap = capacitacionRepository.findById(entrada.getCapacitacion().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Capacitación no encontrada"));
        Servicio servicio = acceso.servicioAutorizado(entrada.getServicio().getId());
        if (!cap.getServicio().getId().equals(servicio.getId())) throw new BadRequestException("La capacitación no pertenece al servicio indicado");
        entrada.setCapacitacion(cap);
        entrada.setServicio(servicio);
        entrada.setNombreCompleto(entrada.getNombreCompleto().trim());
        entrada.setDocumento(entrada.getDocumento().trim());
        entrada.setCargo(entrada.getCargo().trim());
        entrada.setCodigoInterno(trim(entrada.getCodigoInterno()));
        entrada.setFechaFirma(entrada.getFechaFirma() == null ? LocalDateTime.now() : entrada.getFechaFirma());
        entrada.setCodigoVerificacion(entrada.getCodigoVerificacion() == null || entrada.getCodigoVerificacion().isBlank() ? nuevoCodigo() : entrada.getCodigoVerificacion().trim().toUpperCase());
        entrada.setEstado(entrada.getEstado() == null || entrada.getEstado().isBlank() ? "VIGENTE" : entrada.getEstado().trim().toUpperCase());
        ConstanciaCapacitacion guardada = constanciaRepository.save(entrada);
        logs.registrarLog("CREAR", "CONSTANCIAS", "Se creó manualmente la constancia " + guardada.getId());
        return guardada;
    }

    @Transactional
    public ConstanciaCapacitacion actualizarConstancia(Long id, ConstanciaCapacitacion entrada) {
        ConstanciaCapacitacion actual = constanciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Constancia de capacitación no encontrada"));
        acceso.servicioAutorizado(actual.getServicio().getId());
        validarConstancia(entrada);
        actual.setNombreCompleto(entrada.getNombreCompleto().trim());
        actual.setDocumento(entrada.getDocumento().trim());
        actual.setCargo(entrada.getCargo().trim());
        actual.setCodigoInterno(trim(entrada.getCodigoInterno()));
        actual.setPuntaje(entrada.getPuntaje());
        actual.setEstado(entrada.getEstado() == null || entrada.getEstado().isBlank() ? actual.getEstado() : entrada.getEstado().trim().toUpperCase());
        return constanciaRepository.save(actual);
    }

    @Transactional
    public void eliminarConstancia(Long id) {
        ConstanciaCapacitacion c = constanciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Constancia de capacitación no encontrada"));
        acceso.servicioAutorizado(c.getServicio().getId());
        constanciaRepository.delete(c);
        logs.registrarLog("ELIMINAR", "CONSTANCIAS", "Se eliminó la constancia " + id);
    }

    public byte[] generarPdf(Long id) {
        ConstanciaCapacitacion c = obtenerPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Constancia no encontrada"));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER.rotate(), 48, 48, 38, 38);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            PdfContentByte canvas = writer.getDirectContent();
            Rectangle page = doc.getPageSize();
            canvas.setLineWidth(2.2f);
            canvas.setColorStroke(new BaseColor(48, 96, 180));
            canvas.rectangle(24, 24, page.getWidth() - 48, page.getHeight() - 48);
            canvas.stroke();
            canvas.setLineWidth(0.7f);
            canvas.setColorStroke(new BaseColor(112, 155, 220));
            canvas.rectangle(31, 31, page.getWidth() - 62, page.getHeight() - 62);
            canvas.stroke();

            Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, new BaseColor(32, 74, 145));
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 27, BaseColor.DARK_GRAY);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 14, BaseColor.DARK_GRAY);
            Font name = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new BaseColor(32, 74, 145));
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);

            Paragraph pBrand = new Paragraph("GLOBAL ISO SECURITY", brand); pBrand.setAlignment(Element.ALIGN_CENTER); pBrand.setSpacingAfter(20); doc.add(pBrand);
            Paragraph pTitle = new Paragraph("CONSTANCIA DE CAPACITACIÓN", title); pTitle.setAlignment(Element.ALIGN_CENTER); pTitle.setSpacingAfter(24); doc.add(pTitle);
            Paragraph intro = new Paragraph("Hace constar que", body); intro.setAlignment(Element.ALIGN_CENTER); doc.add(intro);
            Paragraph pName = new Paragraph(c.getNombreCompleto(), name); pName.setAlignment(Element.ALIGN_CENTER); pName.setSpacingBefore(8); pName.setSpacingAfter(10); doc.add(pName);
            Paragraph detail = new Paragraph("identificado(a) con documento " + c.getDocumento() + ", completó y aprobó la capacitación", body); detail.setAlignment(Element.ALIGN_CENTER); doc.add(detail);
            Paragraph pCap = new Paragraph(c.getCapacitacion().getTitulo(), brand); pCap.setAlignment(Element.ALIGN_CENTER); pCap.setSpacingBefore(12); pCap.setSpacingAfter(10); doc.add(pCap);
            String company = c.getServicio().getEmpresa() == null ? "la organización" : c.getServicio().getEmpresa().getNombre();
            String score = c.getPuntaje() == null ? "" : String.format(Locale.US, " con un resultado de %.1f/100", c.getPuntaje());
            Paragraph end = new Paragraph("desarrollada para " + company + score + ".", body); end.setAlignment(Element.ALIGN_CENTER); end.setSpacingAfter(28); doc.add(end);

            PdfPTable signatures = new PdfPTable(2); signatures.setWidthPercentage(72); signatures.setSpacingBefore(15);
            PdfPCell left = cell("Fecha de emisión\n" + c.getFechaFirma().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), body);
            PdfPCell right = cell("Estado\n" + c.getEstado(), body);
            signatures.addCell(left); signatures.addCell(right); doc.add(signatures);

            Paragraph verify = new Paragraph("Código verificable: " + c.getCodigoVerificacion() + "\n" +
                    "Esta constancia fue generada por el módulo de formación y conserva trazabilidad con el servicio, la capacitación y el participante.", small);
            verify.setAlignment(Element.ALIGN_CENTER); verify.setSpacingBefore(24); doc.add(verify);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("No fue posible generar la constancia en PDF: " + e.getMessage());
        }
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(Element.ALIGN_CENTER); c.setBorder(Rectangle.TOP); c.setPaddingTop(9); c.setBorderColor(BaseColor.GRAY);
        return c;
    }

    private void validarConstancia(ConstanciaCapacitacion c) {
        if (c == null) throw new BadRequestException("La constancia es obligatoria");
        if (c.getNombreCompleto() == null || c.getNombreCompleto().isBlank()) throw new BadRequestException("El nombre completo es obligatorio");
        if (c.getDocumento() == null || c.getDocumento().isBlank()) throw new BadRequestException("El documento es obligatorio");
        if (c.getCargo() == null || c.getCargo().isBlank()) throw new BadRequestException("El cargo es obligatorio");
        if (c.getCapacitacion() == null || c.getCapacitacion().getId() == null) throw new BadRequestException("La capacitación es obligatoria");
        if (c.getServicio() == null || c.getServicio().getId() == null) throw new BadRequestException("El servicio es obligatorio");
    }

    private String nuevoCodigo() { return "GIS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase(); }
    private String trim(String v) { return v == null ? null : v.trim(); }
}
