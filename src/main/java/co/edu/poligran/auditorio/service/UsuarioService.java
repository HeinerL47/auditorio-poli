package co.edu.poligran.auditorio.service;

import co.edu.poligran.auditorio.model.Rol;
import co.edu.poligran.auditorio.model.TipoSolicitante;
import co.edu.poligran.auditorio.model.Usuario;
import co.edu.poligran.auditorio.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private final UsuarioRepository repo;
    private final PasswordEncoder pe;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder pe) {
        this.repo = repo; this.pe = pe;
    }

    public Usuario registrarExterno(String nombre, String documento, String correo,
                                    String telefono, String organizacion, String password) {
        if (correo != null && correo.toLowerCase().endsWith("@poligran.edu.co"))
            throw new IllegalArgumentException("El registro publico es solo para personas externas. Si eres docente o administrativo, ingresa con tu correo institucional (la cuenta la crea el admin del auditorio).");
        if (repo.existsByCorreo(correo)) throw new IllegalArgumentException("Correo ya registrado");
        if (repo.existsByDocumento(documento)) throw new IllegalArgumentException("Documento ya registrado");
        return repo.save(Usuario.builder()
                .nombre(nombre).documento(documento).correo(correo)
                .telefono(telefono).organizacion(organizacion)
                .password(pe.encode(password.trim()))
                .rol(Rol.SOLICITANTE).tipoSolicitante(TipoSolicitante.EXTERNO)
                .activo(true).build());
    }

    public Usuario crearPorAdmin(String nombre, String documento, String correo,
                                 String telefono, String organizacion, String password,
                                 Rol rol, TipoSolicitante tipo) {
        if (rol == null) throw new IllegalArgumentException("Rol requerido");
        if (rol == Rol.SOLICITANTE && tipo == null)
            throw new IllegalArgumentException("Para un SOLICITANTE debes elegir el tipo (Docente / Administrativo / Externo)");
        if (rol != Rol.SOLICITANTE) tipo = null;
        if (rol == Rol.ADMIN_AUDITORIO || rol == Rol.OPERATIVO ||
            (rol == Rol.SOLICITANTE && tipo != TipoSolicitante.EXTERNO)) {
            if (correo == null || !correo.toLowerCase().endsWith("@poligran.edu.co"))
                throw new IllegalArgumentException("Los usuarios internos deben tener correo @poligran.edu.co");
        }
        if (rol == Rol.SOLICITANTE && tipo == TipoSolicitante.EXTERNO) {
            if (correo != null && correo.toLowerCase().endsWith("@poligran.edu.co"))
                throw new IllegalArgumentException("Un externo no puede tener correo @poligran.edu.co");
        }
        if (repo.existsByCorreo(correo)) throw new IllegalArgumentException("Correo ya registrado");
        if (repo.existsByDocumento(documento)) throw new IllegalArgumentException("Documento ya registrado");
        validarPassword(password);
        return repo.save(Usuario.builder()
                .nombre(nombre).documento(documento).correo(correo)
                .telefono(telefono).organizacion(organizacion)
                .password(pe.encode(password.trim()))
                .rol(rol).tipoSolicitante(tipo)
                .activo(true).build());
    }

    public Usuario actualizar(Long id, String nombre, String telefono, String organizacion,
                              Rol rol, TipoSolicitante tipo) {
        Usuario u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));
        u.setNombre(nombre);
        u.setTelefono(telefono);
        u.setOrganizacion(organizacion);
        if (rol != null) u.setRol(rol);
        if (u.getRol() == Rol.SOLICITANTE) {
            if (tipo == null) throw new IllegalArgumentException("Tipo de solicitante requerido");
            u.setTipoSolicitante(tipo);
        } else {
            u.setTipoSolicitante(null);
        }
        return repo.save(u);
    }

    public void cambiarEstado(Long id, boolean activo) {
        Usuario u = repo.findById(id).orElseThrow();
        u.setActivo(activo);
        repo.save(u);
    }

    public void resetPassword(Long id, String nuevaPassword) {
        validarPassword(nuevaPassword);
        Usuario u = repo.findById(id).orElseThrow();
        u.setPassword(pe.encode(nuevaPassword.trim()));
        repo.save(u);
    }

    public void validarPassword(String password) {
        if (password == null || password.trim().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "La contrasena debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
    }

    public List<Usuario> listarTodos() {
        return repo.findAll();
    }

    public ResultadoImportacion importarCsv(java.io.InputStream csv) throws java.io.IOException {
        ResultadoImportacion res = new ResultadoImportacion();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(csv, java.nio.charset.StandardCharsets.UTF_8))) {
            String linea;
            int num = 0;
            while ((linea = br.readLine()) != null) {
                num++;
                if (linea.isBlank()) continue;
                if (num == 1 && linea.toLowerCase().contains("nombre") && linea.toLowerCase().contains("documento")) {
                    continue;
                }
                String[] c = linea.split(",", -1);
                if (c.length < 8) {
                    res.errores.add("Linea " + num + ": se esperan 8 columnas (nombre,documento,correo,telefono,organizacion,password,rol,tipoSolicitante)");
                    continue;
                }
                try {
                    String nombre = c[0].trim();
                    String documento = c[1].trim();
                    String correo = c[2].trim();
                    String telefono = c[3].trim();
                    String organizacion = c[4].trim();
                    String password = c[5].trim();
                    Rol rol = Rol.valueOf(c[6].trim().toUpperCase());
                    TipoSolicitante tipo = c[7].trim().isEmpty() ? null
                            : TipoSolicitante.valueOf(c[7].trim().toUpperCase());
                    crearPorAdmin(nombre, documento, correo, telefono, organizacion, password, rol, tipo);
                    res.exitos++;
                } catch (IllegalArgumentException e) {
                    res.errores.add("Linea " + num + " (" + (c.length > 2 ? c[2] : "?") + "): " + e.getMessage());
                }
            }
        }
        return res;
    }

    public static class ResultadoImportacion {
        public int exitos = 0;
        public java.util.List<String> errores = new java.util.ArrayList<>();
    }

    public Usuario porId(Long id) {
        return repo.findById(id).orElseThrow();
    }

    public Usuario porCorreo(String correo) {
        return repo.findByCorreo(correo).orElseThrow();
    }
}
