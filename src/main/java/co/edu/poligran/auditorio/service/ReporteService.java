package co.edu.poligran.auditorio.service;

import co.edu.poligran.auditorio.model.EstadoReserva;
import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.model.TipoSolicitante;
import co.edu.poligran.auditorio.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final ReservaRepository repo;

    public ReporteService(ReservaRepository r) { this.repo = r; }

    private List<Reserva> noRechazadas() {
        return repo.findAll().stream()
                .filter(r -> r.getEstado() != EstadoReserva.RECHAZADA)
                .collect(Collectors.toList());
    }

    public Map<String, Long> resumenEstados() {
        Map<String, Long> m = new LinkedHashMap<>();
        List<Reserva> todas = repo.findAll();
        m.put("TOTAL", (long) todas.size());
        for (EstadoReserva e : EstadoReserva.values()) {
            m.put(e.name(), todas.stream().filter(r -> r.getEstado() == e).count());
        }
        return m;
    }

    public Map<Integer, Long> horarioMasSolicitado() {
        Map<Integer, Long> raw = noRechazadas().stream()
                .collect(Collectors.groupingBy(r -> r.getInicio().getHour(), Collectors.counting()));
        return new TreeMap<>(raw);
    }

    public Map<String, Long> diaSemanaMasSolicitado() {
        Map<String, Long> raw = noRechazadas().stream()
                .collect(Collectors.groupingBy(r -> r.getInicio().getDayOfWeek().name(), Collectors.counting()));
        Map<String, Long> ordenado = new LinkedHashMap<>();
        for (String d : List.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY")) {
            if (raw.containsKey(d)) ordenado.put(d, raw.get(d));
        }
        return ordenado;
    }

    public Map<String, Long> mesMasSolicitado() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> raw = noRechazadas().stream()
                .collect(Collectors.groupingBy(r -> r.getInicio().format(f), Collectors.counting()));
        return new TreeMap<>(raw);
    }

    public Map<String, Long> porSeccion() {
        Map<String, Long> raw = noRechazadas().stream()
                .collect(Collectors.groupingBy(r -> r.getSeccion().name(), Collectors.counting()));
        return new TreeMap<>(raw);
    }

    public Map<String, Long> porTipoSolicitante() {
        Map<String, Long> m = new LinkedHashMap<>();
        List<Reserva> lista = noRechazadas();
        for (TipoSolicitante t : TipoSolicitante.values()) {
            m.put(t.name(), lista.stream()
                    .filter(r -> t == r.getSolicitante().getTipoSolicitante())
                    .count());
        }
        return m;
    }

    public Map<String, Long> internosVsExternos() {
        Map<String, Long> m = new LinkedHashMap<>();
        List<Reserva> lista = noRechazadas();
        long ext = lista.stream()
                .filter(r -> r.getSolicitante().getTipoSolicitante() == TipoSolicitante.EXTERNO)
                .count();
        m.put("EXTERNOS", ext);
        m.put("INTERNOS", lista.size() - ext);
        return m;
    }

    public BigDecimal ingresosTotales() {
        return repo.findAll().stream()
                .filter(r -> r.getEstado() == EstadoReserva.APROBADA && r.getCosto() != null)
                .map(Reserva::getCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> ingresosPorSeccion() {
        Map<String, BigDecimal> m = new TreeMap<>();
        repo.findAll().stream()
                .filter(r -> r.getEstado() == EstadoReserva.APROBADA && r.getCosto() != null)
                .forEach(r -> m.merge(r.getSeccion().name(), r.getCosto(), BigDecimal::add));
        return m;
    }

    public List<Reserva> historial() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(Reserva::getInicio).reversed())
                .collect(Collectors.toList());
    }
}
