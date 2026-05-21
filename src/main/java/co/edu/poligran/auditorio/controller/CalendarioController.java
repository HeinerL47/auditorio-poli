package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.Bloqueo;
import co.edu.poligran.auditorio.model.EstadoReserva;
import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.model.Rol;
import co.edu.poligran.auditorio.model.Seccion;
import co.edu.poligran.auditorio.model.Usuario;
import co.edu.poligran.auditorio.repository.BloqueoRepository;
import co.edu.poligran.auditorio.service.ReservaService;
import co.edu.poligran.auditorio.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UsuarioService usuarios;

    public CalendarioController(ReservaService r, BloqueoRepository b, UsuarioService u) {
        this.reservas  = r;
        this.bloqueos  = b;
        this.usuarios  = u;
    }

    @GetMapping("/calendario")
    public String calendario(Model m) {
        m.addAttribute("secciones", List.of(Seccion.B1, Seccion.B2, Seccion.B3));
        return "calendario";
    }

    @GetMapping("/api/disponibilidad")
    @ResponseBody
    public List<Map<String, Object>> disponibilidad(@RequestParam String start,
                                                    @RequestParam String end,
                                                    @RequestParam(required = false) Seccion seccion,
                                                    @AuthenticationPrincipal UserDetails ud) {
        LocalDateTime d = parseFecha(start);
        LocalDateTime h = parseFecha(end);

        Usuario usuario = usuarios.porCorreo(ud.getUsername());
        boolean verTodo = puedeVerTodo(usuario);

        List<Map<String, Object>> eventos = new ArrayList<>();

        // Reservas: filtrar por usuario si es SOLICITANTE
        List<Reserva> listaReservas = verTodo
                ? reservas.activasEnRango(d, h)
                : reservas.activasEnRangoDe(d, h, usuario);

        listaReservas.stream()
                .filter(r -> aplicaFiltroSeccion(r.getSeccion(), seccion))
                .map(r -> toEvento(r, verTodo))
                .forEach(eventos::add);

        // Bloqueos: siempre visibles para todos (muestran indisponibilidad general)
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

    /**
     * Los ADMIN y todos los OPERATIVOS ven todas las reservas.
     * Los SOLICITANTES (docente, administrativo, externo) solo ven las suyas.
     */
    private boolean puedeVerTodo(Usuario u) {
        return u.getRol() == Rol.ADMIN_AUDITORIO || u.getRol() == Rol.OPERATIVO;
    }

    /**
     * Muestra eventos de la sección filtrada.
     * B3 (Completo) siempre se muestra porque afecta a todas las secciones.
     */
    private boolean aplicaFiltroSeccion(Seccion evento, Seccion filtro) {
        if (filtro == null) return true;
        if (evento == null) return true;
        if (evento == Seccion.B3) return true;
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

    private Map<String, Object> toEvento(Reserva r, boolean verTodo) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", "r-" + r.getId());
        String prefijo = r.getEstado() == EstadoReserva.PENDIENTE ? "[Pendiente] " : "";
        // Si es admin/operativo muestra el nombre del solicitante; si es el propio solicitante muestra "Mi reserva"
        String titulo = verTodo
                ? prefijo + r.getSeccion().getLabel() + " \u2014 " + r.getTipoEvento()
                  + " (" + r.getSolicitante().getNombre() + ")"
                : prefijo + r.getSeccion().getLabel() + " \u2014 " + r.getTipoEvento();
        m.put("title", titulo);
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
        String secLabel = b.getSeccion() != null ? b.getSeccion().getLabel() : "Todo el auditorio";
        m.put("title", "\u26D4 " + b.getMotivo() + " [" + secLabel + "]");
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
            case "B3" -> "#cf222e";
            default   -> "#8b5cf6";
        };
        return estado == EstadoReserva.PENDIENTE ? base + "99" : base;
    }
}
