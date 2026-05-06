package co.edu.poligran.auditorio.service;

import co.edu.poligran.auditorio.model.*;
import co.edu.poligran.auditorio.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReservaService {

    public static final LocalTime APERTURA = LocalTime.of(8, 0);
    public static final LocalTime CIERRE   = LocalTime.of(21, 0);
    public static final int MAX_HORAS = 4;

    private final ReservaRepository reservas;
    private final BloqueoRepository bloqueos;
    private final TarifaRepository tarifas;
    private final NotificacionService notif;

    public ReservaService(ReservaRepository r, BloqueoRepository b, TarifaRepository t, NotificacionService n) {
        this.reservas = r; this.bloqueos = b; this.tarifas = t; this.notif = n;
    }

    @Transactional
    public Reserva crear(Usuario solicitante, LocalDateTime inicio, LocalDateTime fin,
                         Seccion seccion, String tipoEvento, String especificaciones) {
        validarReglas(solicitante, inicio, fin, seccion);

        BigDecimal costo = BigDecimal.ZERO;
        if (solicitante.esExterno()) {
            costo = calcularCosto(seccion, inicio, fin);
        }

        Reserva r = Reserva.builder()
                .solicitante(solicitante)
                .inicio(inicio).fin(fin)
                .seccion(seccion)
                .tipoEvento(tipoEvento)
                .especificaciones(especificaciones)
                .estado(EstadoReserva.PENDIENTE)
                .costo(costo)
                .creadaEn(LocalDateTime.now())
                .build();
        r = reservas.save(r);
        notif.notificarCreacion(r);
        return r;
    }

    public BigDecimal calcularCosto(Seccion seccion, LocalDateTime inicio, LocalDateTime fin) {
        Tarifa t = tarifas.findBySeccion(seccion).orElseThrow(
                () -> new IllegalStateException("Sin tarifa para " + seccion));
        long minutos = ChronoUnit.MINUTES.between(inicio, fin);
        BigDecimal horas = BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return t.getValorHora().multiply(horas).setScale(0, RoundingMode.HALF_UP);
    }

    public void validarReglas(Usuario solicitante, LocalDateTime inicio, LocalDateTime fin, Seccion seccion) {
        if (inicio.isAfter(fin) || inicio.equals(fin))
            throw new IllegalArgumentException("Rango de fechas invalido");

        if (!inicio.toLocalDate().equals(fin.toLocalDate()))
            throw new IllegalArgumentException("La reserva debe ser en un mismo dia");

        if (inicio.toLocalTime().isBefore(APERTURA) || fin.toLocalTime().isAfter(CIERRE))
            throw new IllegalArgumentException("Horario fuera de 8:00 AM - 9:00 PM");

        long horas = ChronoUnit.MINUTES.between(inicio, fin);
        if (horas > MAX_HORAS * 60)
            throw new IllegalArgumentException("Maximo " + MAX_HORAS + " horas por reserva");

        LocalDateTime ahora = LocalDateTime.now();
        long diasAnticipacion = ChronoUnit.DAYS.between(ahora.toLocalDate(), inicio.toLocalDate());
        if (diasAnticipacion < 7)
            throw new IllegalArgumentException("Se requiere minimo 1 semana de anticipacion");

        if (diasAnticipacion > 31)
            throw new IllegalArgumentException("Maximo 1 mes de anticipacion para eventos grandes");

        if (inicio.getYear() != ahora.getYear())
            throw new IllegalArgumentException("Solo se permiten reservas para el ano actual");

        // Bloqueos
        for (Bloqueo b : bloqueos.findAll()) {
            if (chocaConBloqueo(b, inicio, fin, seccion))
                throw new IllegalArgumentException("Choca con bloqueo: " + b.getMotivo());
        }

        // Solapamiento con otras reservas
        List<Reserva> sol = reservas.findSolapadas(inicio, fin);
        for (Reserva otra : sol) {
            if (seccionesIncompatibles(otra.getSeccion(), seccion))
                throw new IllegalArgumentException("Conflicto con reserva existente #" + otra.getId());
        }
    }

    private boolean chocaConBloqueo(Bloqueo b, LocalDateTime inicio, LocalDateTime fin, Seccion seccion) {
        if (b.getSeccion() != null && seccionesIncompatibles(b.getSeccion(), seccion)) {
            // ok, podria afectar
        } else if (b.getSeccion() != null) {
            return false;
        }
        if (b.isRecurrente()) {
            if (b.getDiaSemana() != inicio.getDayOfWeek()) return false;
            LocalTime hi = inicio.toLocalTime();
            LocalTime hf = fin.toLocalTime();
            return hi.isBefore(b.getHoraFin()) && hf.isAfter(b.getHoraInicio());
        } else {
            if (b.getInicio() == null || b.getFin() == null) return false;
            return inicio.isBefore(b.getFin()) && fin.isAfter(b.getInicio());
        }
    }

    private boolean seccionesIncompatibles(Seccion a, Seccion b) {
        if (a == b) return true;
        if (a == Seccion.COMPLETO || b == Seccion.COMPLETO) return true;
        return false;
    }

    @Transactional
    public Reserva aprobar(Long id, String adminCorreo) {
        Reserva r = reservas.findById(id).orElseThrow();
        r.setEstado(EstadoReserva.APROBADA);
        r.setDecididaEn(LocalDateTime.now());
        r.setDecididaPor(adminCorreo);
        notif.notificarDecision(r);
        return r;
    }

    @Transactional
    public Reserva rechazar(Long id, String adminCorreo, String motivo) {
        Reserva r = reservas.findById(id).orElseThrow();
        r.setEstado(EstadoReserva.RECHAZADA);
        r.setDecididaEn(LocalDateTime.now());
        r.setDecididaPor(adminCorreo);
        r.setObservaciones(motivo);
        notif.notificarDecision(r);
        return r;
    }

    @Transactional
    public Reserva cancelar(Long id, Usuario solicitante) {
        Reserva r = reservas.findById(id).orElseThrow();
        if (!r.getSolicitante().getId().equals(solicitante.getId()) &&
            solicitante.getRol() != Rol.ADMIN_AUDITORIO)
            throw new IllegalArgumentException("No autorizado");
        long horas = ChronoUnit.HOURS.between(LocalDateTime.now(), r.getInicio());
        if (horas < 4 && solicitante.getRol() != Rol.ADMIN_AUDITORIO)
            throw new IllegalArgumentException("Solo se puede cancelar hasta 4 horas antes");
        r.setEstado(EstadoReserva.CANCELADA);
        notif.notificarCancelacion(r);
        return r;
    }

    @Transactional
    public Reserva agregarObservacion(Long id, Usuario u, String obs) {
        Reserva r = reservas.findById(id).orElseThrow();
        if (r.getInicio().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Las observaciones son post-evento");
        String prev = r.getObservaciones() == null ? "" : r.getObservaciones() + "\n";
        r.setObservaciones(prev + "[" + u.getNombre() + "] " + obs);
        return r;
    }

    public List<Reserva> listarTodas() { return reservas.findAll(); }
    public List<Reserva> listarDe(Usuario u) { return reservas.findBySolicitante(u); }
    public List<Reserva> listarPendientes() { return reservas.findByEstado(EstadoReserva.PENDIENTE); }
    public List<Reserva> aprobadasEnRango(LocalDateTime d, LocalDateTime h) {
        return reservas.findAprobadasEnRango(d, h);
    }
    public Reserva porId(Long id) { return reservas.findById(id).orElseThrow(); }
}
