package co.edu.poligran.auditorio.repository;

import co.edu.poligran.auditorio.model.Rol;
import co.edu.poligran.auditorio.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    Optional<Usuario> findByDocumento(String documento);
    boolean existsByCorreo(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByDocumento(String documento);
    List<Usuario> findByRol(Rol rol);
    List<Usuario> findByRolIn(List<Rol> roles);
}
