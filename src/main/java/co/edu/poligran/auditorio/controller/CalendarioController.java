package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.Bloqueo;
import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.repository.BloqueoRepository;
import co.edu.poligran.auditorio.service.ReservaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return "calendario";
    }

    @GetMapping("/api/disponibilidad")
    @ResponseBody
    public List<Map<String, Object>> disponibilidad(@RequestParam String start,
                                                    @RequestParam String end) {
        LocalDateTime d = LocalDateTime.parse(start.substring(0, 19));
        LocalDateTime h = LocalDateTime.parse(end.substring(0, 19));

        List<Map<String, Object>> eventos = new ArrayList<>();

        reservas.aprobadasEnRango(d, h).stream()
                .map(this::toEvento)
                .forEach(eventos::add);

        for (Bloqueo b : bloqueos.findAll()) {
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

    private Map<String, Object> toEvento(Reserva r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", "r-" + r.getId());
        m.put("title", r.getSeccion() + " \u2014 " + r.getTipoEvento());
        m.put("start", r.getInicio().toString());
        m.put("end", r.getFin().toString());
        m.put("color", colorPorSeccion(r.getSeccion().name()));
        m.put("tipo", "reserva");
        return m;
    }

    private Map<String, Object> toBloqueoEvento(Bloqueo b, LocalDateTime inicio, LocalDateTime fin) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", "b-" + b.getId() + "-" + inicio.toLocalDate());
        m.put("title", "\u26D4 " + b.getMotivo()
                + (b.getSeccion() != null ? " [" + b.getSeccion() + "]" : ""));
        m.put("start", inicio.toString());
        m.put("end", fin.toString());
        m.put("color", "#6b2737");
        m.put("textColor", "#ffffff");
        m.put("display", "block");
        m.put("tipo", "bloqueo");
        return m;
    }

    private String colorPorSeccion(String s) {
        return switch (s) {
            case "B1" -> "#1f6feb";
            case "B2" -> "#2da44e";
            case "B3" -> "#bf8700";
            default -> "#cf222e";
        };
    }
}
