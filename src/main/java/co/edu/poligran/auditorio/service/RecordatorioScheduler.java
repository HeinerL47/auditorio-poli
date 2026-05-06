package co.edu.poligran.auditorio.service;

import co.edu.poligran.auditorio.model.EstadoReserva;
import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.repository.ReservaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class RecordatorioScheduler {
    private final ReservaRepository repo;
    private final NotificacionService notif;

    public RecordatorioScheduler(ReservaRepository r, NotificacionService n) {
        this.repo = r; this.notif = n;
    }

    // Cada 5 minutos: notificar eventos que arrancan en ~1 hora
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void recordatorios() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime desde = ahora.plusMinutes(55);
        LocalDateTime hasta = ahora.plusMinutes(65);
        for (Reserva r : repo.findByEstadoAndInicioBetween(EstadoReserva.APROBADA, desde, hasta)) {
            if (!r.isRecordatorioEnviado()) {
                notif.notificarRecordatorio(r);
                r.setRecordatorioEnviado(true);
            }
        }
    }
}
