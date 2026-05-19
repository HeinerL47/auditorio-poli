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

    public static final LocalTime APERTURA  = LocalTime.of(8, 0);
    public static final LocalTime CIERRE    = LocalTime.of(21, 0);
    public static final int MAX_HORAS       = 4;
    public static final int MIN_MINUTOS     = 60;
    public static final int MIN_DIAS_ANTELACION = 7;
    public static final int MAX_DIAS_ANTELACION = 31;

    private final ReservaRepository reservas;
    private final BloqueoRepository bloqueos;
    private final TarifaRepository  tarifas;
    private final NotificacionService notif;

    public ReservaService(ReservaRepository r, BloqueoRepository b, TarifaRepository t, NotificacionService n) {
        this.reservas = r; this.bloqueos = b; this.tarifas = t; this.notif = n;
    }

    @Transactional
    public Reserva crear(Usuario solicitante, LocalDateTime inicio, LocalDateTime fin,
                         Seccion seccion, String tipoEvento, String especificaciones) {
        validarReglas(solicitante, inicio, fin, seccion, null);
        BigDecimal costo = solicitante.esExterno() ? calcularCosto(seccion, inicio, fin) : BigDecimal.ZERO;
        Reserva r = Reserva.builder()
                .solicitante(solicitante).inicio(inicio).fin(fin)
                .seccion(seccion).tipoEvento(tipoEvento).especificaciones(especificaciones)
                .estado(EstadoReserva.PENDIENTE).costo(costo).creadaEn(LocalDateTime.now()).build();
        r = reservas.save(r);
        notif.notificarCreacion(r);
        return r;
    }

    @Transactional
    public Reserva actualizar(Long id, LocalDateTime inicio, LocalDateTime fin,
                              Seccion seccion, String tipoEvento, String especificaciones) {
        Reserva r = reservas.findById(id).orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        if (r.getEstado() != EstadoReserva.PENDIENTE && r.getEstado() != EstadoReserva.APROBADA)
            throw new IllegalArgumentException("Solo se pueden editar reservas pendientes o aprobadas");
        validarReglas(r.getSolicitante(), inicio, fin, seccion, id);
        r.setInicio(inicio); r.setFin(fin); r.setSeccion(seccion);
        r.setTipoEvento(tipoEvento); r.setEspecificaciones(especificaciones);
        if (r.getSolicitante().esExterno()) r.setCosto(calcularCosto(seccion, inicio, fin));
        return reservas.save(r);
    }

    public BigDecimal calcularCosto(Seccion seccion, LocalDateTime inicio, LocalDateTime fin) {
        Tarifa t = tarifas.findBySeccion(seccion)
                .orElseThrow(() -> new IllegalStateException("Sin tarifa para " + seccion));
        long minutos = ChronoUnit.MINUTES.between(inicio, fin);
        BigDecimal horas = BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return t.getValorHora().multiply(horas).setScale(0, RoundingMode.HALF_UP);
    }

    public void validarReglas(Usuario solicitante, LocalDateTime inicio, LocalDateTime fin,
                              Seccion seccion, Long excludeId) {
        if (inicio == null || fin == null)
            throw new IllegalArgumentException("Fecha y hora son obligatorias");
        if (inicio.isAfter(fin) || inicio.equals(fin))
            throw new IllegalArgumentException("Rango de fechas inválido");
        if (!inicio.toLocalDate().equals(fin.toLocalDate()))
            throw new IllegalArgumentException("La reserva debe ser en un mismo día");
        if (inicio.toLocalTime().isBefore(APERTURA) || fin.toLocalTime().isAfter(CIERRE))
            throw new IllegalArgumentException("Horario fuera de 8:00 AM - 9:00 PM");

        long minutos = ChronoUnit.MINUTES.between(inicio, fin);
        if (minutos < MIN_MINUTOS)
            throw new IllegalArgumentException("La duración mínima es de " + (MIN_MINUTOS / 60) + " hora");
        if (minutos > MAX_HORAS * 60L)
            throw new IllegalArgumentException("Máximo " + MAX_HORAS + " horas por reserva");

        LocalDateTime ahora = LocalDateTime.now();
        long diasAnticipacion = ChronoUnit.DAYS.between(ahora.toLocalDate(), inicio.toLocalDate());
        if (diasAnticipacion < MIN_DIAS_ANTELACION)
            throw new IllegalArgumentException("Se requiere mínimo 1 semana de anticipación");
        if (diasAnticipacion > MAX_DIAS_ANTELACION)
            throw new IllegalArgumentException("Máximo 1 mes de anticipación para reservas");

        if (inicio.getYear() != ahora.getYear())
            throw new IllegalArgumentException("Solo se permiten reservas para el año actual");

        for (Bloqueo b : bloqueos.findAll()) {
            if (chocaConBloqueo(b, inicio, fin, seccion))
                throw new IllegalArgumentException("Horario bloqueado por administrador: " + b.getMotivo());
        }

        List<Reserva> sol = excludeId == null
                ? reservas.findSolapadas(inicio, fin)
                : reservas.findSolapadasExcluyendo(inicio, fin, excludeId);
        for (Reserva otra : sol) {
            if (seccionesIncompatibles(otra.getSeccion(), seccion))
                throw new IllegalArgumentException("Conflicto con reserva #" + otra.getId()
                        + " (" + otra.getSeccion().getLabel() + ", " + otra.getEstado() + ")");
        }
    }

    private boolean chocaConBloqueo(Bloqueo b, LocalDateTime inicio, LocalDateTime fin, Seccion seccion) {
        if (b.getSeccion() != null && !seccionesIncompatibles(b.getSeccion(), seccion)) return false;
        if (b.isRecurrente()) {
            if (b.getDiaSemana() == null || b.getHoraInicio() == null || b.getHoraFin() == null) return false;
            if (b.getDiaSemana() != inicio.getDayOfWeek()) return false;
            return inicio.toLocalTime().isBefore(b.getHoraFin()) && fin.toLocalTime().isAfter(b.getHoraInicio());
        }
        if (b.getInicio() == null || b.getFin() == null) return false;
        return inicio.isBefore(b.getFin()) && fin.isAfter(b.getInicio());
    }

    /**
     * Dos secciones son incompatibles si:
     * - Son la misma sección (no pueden haber dos reservas de B1 al mismo tiempo)
     * - Una de ellas es B3 (Completo), que abarca todo el auditorio y es incompatible con B1 y B2
     */
    private boolean seccionesIncompatibles(Seccion a, Seccion b) {
        return a == b || a == Seccion.B3 || b == Seccion.B3;
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
        if (motivo == null || motivo.trim().isEmpty())
            throw new IllegalArgumentException("Debe indicar el motivo del rechazo");
        r.setEstado(EstadoReserva.RECHAZADA);
        r.setDecididaEn(LocalDateTime.now());
        r.setDecididaPor(adminCorreo);
        r.setMotivoRechazo(motivo.trim());
        notif.notificarDecision(r);
        return r;
    }

    @Transactional
    public Reserva cancelar(Long id, Usuario solicitante, String motivo) {
        Reserva r = reservas.findById(id).orElseThrow();
        boolean esAdminOAsistente = solicitante.getRol() == Rol.ADMIN_AUDITORIO || solicitante.esAsistente();
        boolean esPropietario     = r.getSolicitante().getId().equals(solicitante.getId());

        if (!esPropietario && !esAdminOAsistente)
            throw new IllegalArgumentException("No autorizado para cancelar esta reserva");

        if (!esAdminOAsistente) {
            long horas = ChronoUnit.HOURS.between(LocalDateTime.now(), r.getInicio());
            if (horas < 4)
                throw new IllegalArgumentException("Solo se puede cancelar hasta 4 horas antes del evento");
        }
        r.setEstado(EstadoReserva.CANCELADA);
        if (motivo != null && !motivo.trim().isEmpty()) r.setMotivoCancelacion(motivo.trim());
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

    public List<Reserva> listarTodas()             { return reservas.findAll(); }
    public List<Reserva> listarDe(Usuario u)       { return reservas.findBySolicitante(u); }
    public List<Reserva> listarPendientes()        { return reservas.findByEstado(EstadoReserva.PENDIENTE); }
    public List<Reserva> activasEnRango(LocalDateTime d, LocalDateTime h) { return reservas.findActivasEnRango(d, h); }
    public Reserva porId(Long id)                  { return reservas.findById(id).orElseThrow(); }
}
