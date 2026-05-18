package co.edu.poligran.auditorio.service;

import co.edu.poligran.auditorio.model.EstadoReserva;
import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.model.Seccion;
import co.edu.poligran.auditorio.model.TipoSolicitante;
import co.edu.poligran.auditorio.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    public static final String PERIODO_SEMANA = "semana";
    public static final String PERIODO_MES = "mes";
    public static final String PERIODO_ANIO = "anio";

    private final ReservaRepository repo;

    public ReporteService(ReservaRepository r) { this.repo = r; }

    public record FiltroReporte(Seccion seccion, String periodo, LocalDate fechaRef) {}

    public RangoFechas calcularRango(FiltroReporte filtro) {
        LocalDate ref = filtro.fechaRef() != null ? filtro.fechaRef() : LocalDate.now();
        String periodo = filtro.periodo() != null && !filtro.periodo().isBlank()
                ? filtro.periodo() : PERIODO_MES;

        LocalDate inicio;
        LocalDate finExclusivo;

        switch (periodo) {
            case PERIODO_SEMANA -> {
                inicio = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                finExclusivo = inicio.plusWeeks(1);
            }
            case PERIODO_ANIO -> {
                inicio = ref.withDayOfYear(1);
                finExclusivo = inicio.plusYears(1);
            }
            default -> {
                inicio = ref.withDayOfMonth(1);
                finExclusivo = inicio.plusMonths(1);
            }
        }

        return new RangoFechas(
                inicio.atStartOfDay(),
                finExclusivo.atStartOfDay(),
                periodo,
                inicio,
                finExclusivo.minusDays(1));
    }

    public record RangoFechas(LocalDateTime desde, LocalDateTime hastaExclusivo,
                              String periodo, LocalDate inicio, LocalDate fin) {}

    private List<Reserva> baseFiltrada(FiltroReporte filtro) {
        RangoFechas rango = calcularRango(filtro);
        List<Reserva> lista;
        if (filtro.seccion() != null) {
            lista = repo.findConInicioEnRangoPorSeccion(
                    rango.desde(), rango.hastaExclusivo(), filtro.seccion());
        } else {
            lista = repo.findConInicioEnRango(rango.desde(), rango.hastaExclusivo());
        }
        return lista;
    }

    private List<Reserva> noRechazadas(List<Reserva> lista) {
        return lista.stream()
                .filter(r -> r.getEstado() != EstadoReserva.RECHAZADA)
                .collect(Collectors.toList());
    }

    public Map<String, Long> resumenEstados(FiltroReporte filtro) {
        Map<String, Long> m = new LinkedHashMap<>();
        List<Reserva> todas = baseFiltrada(filtro);
        m.put("TOTAL", (long) todas.size());
        for (EstadoReserva e : EstadoReserva.values()) {
            m.put(e.name(), todas.stream().filter(r -> r.getEstado() == e).count());
        }
        return m;
    }

    public Map<Integer, Long> horarioMasSolicitado(FiltroReporte filtro) {
        Map<Integer, Long> raw = noRechazadas(baseFiltrada(filtro)).stream()
                .collect(Collectors.groupingBy(r -> r.getInicio().getHour(), Collectors.counting()));
        return new TreeMap<>(raw);
    }

    public Map<String, Long> diaSemanaMasSolicitado(FiltroReporte filtro) {
        Map<String, Long> raw = noRechazadas(baseFiltrada(filtro)).stream()
                .collect(Collectors.groupingBy(r -> r.getInicio().getDayOfWeek().name(), Collectors.counting()));
        Map<String, Long> ordenado = new LinkedHashMap<>();
        for (String d : List.of("MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY")) {
            if (raw.containsKey(d)) ordenado.put(d, raw.get(d));
        }
        return ordenado;
    }

    public Map<String, Long> mesMasSolicitado(FiltroReporte filtro) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> raw = noRechazadas(baseFiltrada(filtro)).stream()
                .collect(Collectors.groupingBy(r -> r.getInicio().format(f), Collectors.counting()));
        return new TreeMap<>(raw);
    }

    public Map<String, Long> porSeccion(FiltroReporte filtro) {
        Map<String, Long> raw = noRechazadas(baseFiltrada(filtro)).stream()
                .collect(Collectors.groupingBy(r -> r.getSeccion().name(), Collectors.counting()));
        return new TreeMap<>(raw);
    }

    public Map<String, Long> porTipoSolicitante(FiltroReporte filtro) {
        Map<String, Long> m = new LinkedHashMap<>();
        List<Reserva> lista = noRechazadas(baseFiltrada(filtro));
        for (TipoSolicitante t : TipoSolicitante.values()) {
            m.put(t.name(), lista.stream()
                    .filter(r -> r.getSolicitante() != null
                            && t == r.getSolicitante().getTipoSolicitante())
                    .count());
        }
        return m;
    }

    public Map<String, Long> internosVsExternos(FiltroReporte filtro) {
        Map<String, Long> m = new LinkedHashMap<>();
        List<Reserva> lista = noRechazadas(baseFiltrada(filtro));
        long ext = lista.stream()
                .filter(r -> r.getSolicitante() != null
                        && r.getSolicitante().getTipoSolicitante() == TipoSolicitante.EXTERNO)
                .count();
        m.put("EXTERNOS", ext);
        m.put("INTERNOS", lista.size() - ext);
        return m;
    }

    public BigDecimal ingresosTotales(FiltroReporte filtro) {
        return baseFiltrada(filtro).stream()
                .filter(r -> r.getEstado() == EstadoReserva.APROBADA && r.getCosto() != null)
                .map(Reserva::getCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> ingresosPorSeccion(FiltroReporte filtro) {
        Map<String, BigDecimal> m = new TreeMap<>();
        baseFiltrada(filtro).stream()
                .filter(r -> r.getEstado() == EstadoReserva.APROBADA && r.getCosto() != null)
                .forEach(r -> m.merge(r.getSeccion().name(), r.getCosto(), BigDecimal::add));
        return m;
    }

    public List<Reserva> historial(FiltroReporte filtro) {
        return baseFiltrada(filtro).stream()
                .sorted(Comparator.comparing(Reserva::getInicio).reversed())
                .collect(Collectors.toList());
    }
}
