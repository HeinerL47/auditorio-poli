package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.Bloqueo;
import co.edu.poligran.auditorio.model.EstadoReserva;
import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.model.Seccion;
import co.edu.poligran.auditorio.repository.BloqueoRepository;
import co.edu.poligran.auditorio.service.ReservaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CalendarioController {

    private final ReservaService reservas;
    private final BloqueoRepository bloqueos;

    public CalendarioController(ReservaService r, BloqueoRepository b) {
        this.reservas = r;
        this.bloqueos = b;
    }

    @GetMapping("/calendario")
    public String calendario(Model m) {
        m.addAttribute("secciones", List.of(Seccion.B1, Seccion.B2, Seccion.B3, Seccion.COMPLETO));
        return "calendario";
    }

    @GetMapping("/api/disponibilidad")
    @ResponseBody
    public List<Map<String, Object>> disponibilidad(@RequestParam String start,
                                                    @RequestParam String end,
                                                    @RequestParam(required = false) Seccion seccion) {
        LocalDateTime d = parseFecha(start);
        LocalDateTime h = parseFecha(end);

        List<Map<String, Object>> eventos = new ArrayList<>();

        reservas.activasEnRango(d, h).stream()
                .filter(r -> aplicaFiltroSeccion(r.getSeccion(), seccion))
                .map(this::toEvento)
                .forEach(eventos::add);

        for (Bloqueo b : bloqueos.findAll()) {
            if (!aplicaFiltroSeccion(b.getSeccion(), seccion)) continue;
            if (b.isRecurrente()) {
                LocalDate cursor = d.toLocalDate();
                while (!cursor.isAfter(h.toLocalDate())) {
                    if (b.getDiaSemana() != null && cursor.getDayOfWeek() == b.getDiaSemana()) {
                        LocalDateTime bInicio = cursor.atTime(b.getHoraInicio());
                        LocalDateTime bFin    = cursor.atTime(b.getHoraFin());
                        if (bInicio.isBefore(h) && bFin.isAfter(d)) {
                            eventos.add(toBloqueoEvento(b, bInicio, bFin));
                        }
                    }
                    cursor = cursor.plusDays(1);
                }
            } else if (b.getInicio() != null && b.getFin() != null) {
                if (b.getInicio().isBefore(h) && b.getFin().isAfter(d)) {
                    eventos.add(toBloqueoEvento(b, b.getInicio(), b.getFin()));
                }
            }
        }

        return eventos;
    }

    /** Muestra eventos de la seccion filtrada y reservas/bloqueos de auditorio completo. */
    private boolean aplicaFiltroSeccion(Seccion evento, Seccion filtro) {
        if (filtro == null) return true;
        if (evento == null) return true;
        if (evento == Seccion.COMPLETO) return true;
        return evento == filtro;
    }

    private LocalDateTime parseFecha(String s) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException("Fecha requerida");
        try {
            if (s.contains("T") && (s.endsWith("Z") || s.contains("+") || s.lastIndexOf('-') > 10)) {
                return OffsetDateTime.parse(s).toLocalDateTime();
            }
            if (s.length() >= 19) {
                return LocalDateTime.parse(s.substring(0, 19));
            }
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(s.substring(0, 10)).atStartOfDay();
        }
    }

    private Map<String, Object> toEvento(Reserva r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", "r-" + r.getId());
        String prefijo = r.getEstado() == EstadoReserva.PENDIENTE ? "[Pendiente] " : "";
        m.put("title", prefijo + r.getSeccion() + " \u2014 " + r.getTipoEvento());
        m.put("start", r.getInicio().toString());
        m.put("end", r.getFin().toString());
        m.put("color", colorPorSeccion(r.getSeccion().name(), r.getEstado()));
        m.put("borderColor", r.getEstado() == EstadoReserva.PENDIENTE ? "#888888" : null);
        m.put("tipo", "reserva");
        m.put("seccion", r.getSeccion().name());
        m.put("estado", r.getEstado().name());
        return m;
    }

    private Map<String, Object> toBloqueoEvento(Bloqueo b, LocalDateTime inicio, LocalDateTime fin) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", "b-" + b.getId() + "-" + inicio.toLocalDate());
        m.put("title", "\u26D4 " + b.getMotivo()
                + (b.getSeccion() != null ? " [" + b.getSeccion() + "]" : " [Todo]"));
        m.put("start", inicio.toString());
        m.put("end", fin.toString());
        m.put("color", "#6b2737");
        m.put("textColor", "#ffffff");
        m.put("display", "block");
        m.put("tipo", "bloqueo");
        m.put("seccion", b.getSeccion() != null ? b.getSeccion().name() : "TODAS");
        return m;
    }

    private String colorPorSeccion(String s, EstadoReserva estado) {
        String base = switch (s) {
            case "B1" -> "#1f6feb";
            case "B2" -> "#2da44e";
            case "B3" -> "#bf8700";
            default -> "#cf222e";
        };
        if (estado == EstadoReserva.PENDIENTE) {
            return base + "99";
        }
        return base;
    }
}
