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

    @Query("SELECT r FROM Reserva r WHERE r.estado IN ('PENDIENTE','APROBADA') " +
            "AND r.inicio < :fin AND r.fin > :inicio AND r.id <> :excludeId")
    List<Reserva> findSolapadasExcluyendo(@Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin,
                                          @Param("excludeId") Long excludeId);

    @Query("SELECT r FROM Reserva r WHERE r.estado IN ('APROBADA','PENDIENTE') " +
            "AND r.inicio < :hasta AND r.fin > :desde")
    List<Reserva> findActivasEnRango(@Param("desde") LocalDateTime desde,
                                     @Param("hasta") LocalDateTime hasta);

    @Query("SELECT r FROM Reserva r WHERE r.estado IN ('APROBADA','PENDIENTE') " +
            "AND r.inicio < :hasta AND r.fin > :desde " +
            "AND (:seccion IS NULL OR r.seccion = :seccion)")
    List<Reserva> findActivasEnRangoPorSeccion(@Param("desde") LocalDateTime desde,
                                               @Param("hasta") LocalDateTime hasta,
                                               @Param("seccion") Seccion seccion);

    @Query("SELECT r FROM Reserva r WHERE r.estado IN ('APROBADA','PENDIENTE') " +
            "AND r.inicio < :hasta AND r.fin > :desde " +
            "AND r.solicitante = :solicitante")
    List<Reserva> findActivasEnRangoPorSolicitante(@Param("desde") LocalDateTime desde,
                                                   @Param("hasta") LocalDateTime hasta,
                                                   @Param("solicitante") Usuario solicitante);

    List<Reserva> findByEstadoAndInicioBetween(EstadoReserva estado, LocalDateTime desde, LocalDateTime hasta);

    @Query("SELECT r FROM Reserva r WHERE r.inicio >= :desde AND r.inicio < :hasta")
    List<Reserva> findConInicioEnRango(@Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);

    @Query("SELECT r FROM Reserva r WHERE r.inicio >= :desde AND r.inicio < :hasta " +
            "AND (:seccion IS NULL OR r.seccion = :seccion)")
    List<Reserva> findConInicioEnRangoPorSeccion(@Param("desde") LocalDateTime desde,
                                                  @Param("hasta") LocalDateTime hasta,
                                                  @Param("seccion") Seccion seccion);
}
