package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.ControlCatalogo;
import com.globalisosecurity.backend.models.PerfilOrganizacional;
import com.globalisosecurity.backend.models.Servicio;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ControlRelevanciaService {

    public record Relevancia(int puntaje, String nivel, String motivo) {}

    public Relevancia evaluar(ControlCatalogo control, Servicio servicio, PerfilOrganizacional perfil) {
        Set<String> etiquetas = new HashSet<>();
        if (control.getEtiquetas() != null) {
            Arrays.stream(control.getEtiquetas().split(","))
                    .map(String::trim).map(String::toLowerCase)
                    .filter(s -> !s.isBlank()).forEach(etiquetas::add);
        }

        int puntaje = 10;
        List<String> razones = new ArrayList<>();
        String sectorNombre = perfil != null && perfil.getSector() != null && perfil.getSector().getNombre() != null
                ? perfil.getSector().getNombre()
                : servicio != null && servicio.getSector() != null ? servicio.getSector().getNombre() : "";
        String sector = sectorNombre == null ? "" : sectorNombre.toUpperCase(Locale.ROOT);

        Map<String,Integer> pesosSector = new HashMap<>();
        if (sector.contains("SALUD")) {
            pesosSector.put("datos_sensibles", 25); pesosSector.put("privacidad", 25);
            pesosSector.put("continuidad", 15); pesosSector.put("servicio_critico", 20);
            pesosSector.put("proveedores", 10); pesosSector.put("fisico", 10); pesosSector.put("endpoint", 10);
        } else if (sector.contains("EDUC")) {
            pesosSector.put("datos_sensibles", 20); pesosSector.put("privacidad", 20);
            pesosSector.put("menores", 30); pesosSector.put("nube", 15);
            pesosSector.put("trabajo_remoto", 10); pesosSector.put("personas", 10); pesosSector.put("acceso", 10);
        } else if (sector.contains("FINAN")) {
            pesosSector.put("fraude", 30); pesosSector.put("continuidad", 20);
            pesosSector.put("criptografia", 20); pesosSector.put("logs", 15);
            pesosSector.put("acceso", 15); pesosSector.put("proveedores", 10); pesosSector.put("registros", 10);
        } else if (sector.contains("TECNO")) {
            pesosSector.put("desarrollo", 25); pesosSector.put("nube", 20);
            pesosSector.put("vulnerabilidades", 25); pesosSector.put("configuracion", 20);
            pesosSector.put("logs", 15); pesosSector.put("redes", 15); pesosSector.put("proveedores", 10);
        } else if (sector.contains("MANUFACT")) {
            pesosSector.put("ot", 30); pesosSector.put("continuidad", 20);
            pesosSector.put("fisico", 15); pesosSector.put("mantenimiento", 20);
            pesosSector.put("proveedores", 10); pesosSector.put("infraestructura", 15);
        }

        int sectorExtra = 0;
        for (String etiqueta : etiquetas) sectorExtra = Math.max(sectorExtra, pesosSector.getOrDefault(etiqueta, 0));
        if (sectorExtra > 0) {
            puntaje += sectorExtra;
            razones.add("relevancia para el sector " + (sectorNombre == null || sectorNombre.isBlank() ? "definido" : sectorNombre));
        }

        if (perfil != null) {
            puntaje += sumarContexto(etiquetas, perfil, razones);
        }

        puntaje = Math.min(100, puntaje);
        String nivel = puntaje >= 60 ? "ALTA" : puntaje >= 35 ? "MEDIA" : "BASE";
        String motivo = razones.isEmpty()
                ? "Control de referencia que debe analizarse según riesgos y contexto; no se excluye automáticamente."
                : "Recomendado por " + String.join(", ", razones) + ". La aplicabilidad final requiere justificación del responsable.";
        return new Relevancia(puntaje, nivel, motivo);
    }

    private int sumarContexto(Set<String> tags, PerfilOrganizacional p, List<String> razones) {
        int extra = 0;
        if (p.isManejaDatosSensibles() && intersects(tags, "datos_sensibles", "privacidad", "criptografia")) { extra += 20; razones.add("tratamiento de datos sensibles"); }
        if (p.isUsaServiciosNube() && intersects(tags, "nube", "proveedores", "redes")) { extra += 15; razones.add("uso de servicios en la nube"); }
        if (p.isPermiteTrabajoRemoto() && intersects(tags, "trabajo_remoto", "endpoint", "acceso")) { extra += 12; razones.add("trabajo remoto"); }
        if (p.isProcesaPagos() && intersects(tags, "fraude", "criptografia", "logs", "acceso")) { extra += 18; razones.add("procesamiento de pagos"); }
        if (p.isInfraestructuraPropia() && intersects(tags, "fisico", "infraestructura", "redes", "continuidad")) { extra += 10; razones.add("infraestructura propia"); }
        if (p.isDependeProveedores() && intersects(tags, "proveedores", "terceros", "nube")) { extra += 12; razones.add("dependencia de terceros"); }
        if (p.isServicioCritico24x7() && intersects(tags, "continuidad", "servicio_critico", "backup", "monitoreo")) { extra += 20; razones.add("operación crítica 24/7"); }
        if (p.isManejaMenores() && intersects(tags, "menores", "privacidad", "datos_sensibles")) { extra += 22; razones.add("tratamiento de datos de menores"); }
        if (p.isOperaOtIot() && intersects(tags, "ot", "iot", "redes", "mantenimiento", "endpoint")) { extra += 20; razones.add("uso de OT/IoT"); }
        return extra;
    }

    private boolean intersects(Set<String> tags, String... candidates) {
        for (String c : candidates) if (tags.contains(c)) return true;
        return false;
    }
}
