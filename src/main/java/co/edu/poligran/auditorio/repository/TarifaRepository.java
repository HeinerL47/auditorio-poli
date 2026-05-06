package co.edu.poligran.auditorio.repository;

import co.edu.poligran.auditorio.model.Seccion;
import co.edu.poligran.auditorio.model.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    Optional<Tarifa> findBySeccion(Seccion seccion);
}
