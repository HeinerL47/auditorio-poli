package co.edu.poligran.auditorio.repository;

import co.edu.poligran.auditorio.model.EstadoReserva;
import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.model.Seccion;
import co.edu.poligran.auditorio.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findBySolicitante(Usuario u);
    List<Reserva> findByEstado(EstadoReserva estado);

    @Query("SELECT r FROM Reserva r WHERE r.estado IN ('PENDIENTE','APROBADA') " +
            "AND r.inicio < :fin AND r.fin > :inicio")
    List<Reserva> findSolapadas(@Param("inicio") LocalDateTime inicio,
                                @Param("fin") LocalDateTime fin);

    @Query("SELECT r FROM Reserva r WHERE r.estado = 'APROBADA' " +
            "AND r.inicio BETWEEN :desde AND :hasta")
    List<Reserva> findAprobadasEnRango(@Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);

    List<Reserva> findByEstadoAndInicioBetween(EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);
}
