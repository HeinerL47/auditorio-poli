package co.edu.poligran.auditorio.service;

import co.edu.poligran.auditorio.model.Rol;
import co.edu.poligran.auditorio.model.TipoOperativo;
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

    private String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase();
    }

    /**
     * Registro público: solo para agentes externos.
     * Los docentes y administrativos ingresan con correo institucional (cuenta creada por admin).
     * Los externos deben usar un dominio diferente a @poligran.edu.co.
     */
    public Usuario registrarExterno(String nombre, String documento, String correo,
                                    String telefono, String organizacion, String password) {
        if (correo != null && correo.toLowerCase().endsWith("@poligran.edu.co"))
            throw new IllegalArgumentException(
                "El registro público es solo para agentes externos. " +
                "Si eres docente o administrativo, ingresa con tu correo institucional " +
                "(la cuenta la crea el administrador del auditorio).");
        correo = normalizarCorreo(correo);
        if (repo.existsByCorreoIgnoreCase(correo)) throw new IllegalArgumentException("Correo ya registrado");
        if (repo.existsByDocumento(documento)) throw new IllegalArgumentException("Documento ya registrado");
        validarPassword(password);
        return repo.save(Usuario.builder()
                .nombre(nombre).documento(documento).correo(correo)
                .telefono(telefono).organizacion(organizacion)
                .password(pe.encode(password.trim()))
                .rol(Rol.SOLICITANTE).tipoSolicitante(TipoSolicitante.EXTERNO)
                .activo(true).build());
    }

    /**
     * Creación por administrador/asistente.
     * Reglas de correo:
     *   - ADMIN_AUDITORIO, OPERATIVO, DOCENTE, ADMINISTRATIVO → @poligran.edu.co obligatorio
     *   - EXTERNO → cualquier dominio excepto @poligran.edu.co
     */
    public Usuario crearPorAdmin(String nombre, String documento, String correo,
                                 String telefono, String organizacion, String password,
                                 Rol rol, TipoSolicitante tipoSolicitante, TipoOperativo tipoOperativo) {
        if (rol == null) throw new IllegalArgumentException("Rol requerido");

        // Validar tipo según rol
        if (rol == Rol.SOLICITANTE && tipoSolicitante == null)
            throw new IllegalArgumentException(
                "Para SOLICITANTE debes elegir el tipo: Docente / Administrativo / Agente Externo");
        if (rol == Rol.OPERATIVO && tipoOperativo == null)
            throw new IllegalArgumentException(
                "Para OPERATIVO debes elegir el sub-rol: Asistente / Tecnología / Audiovisual / Infraestructura / Operaciones");

        // Limpiar tipos que no aplican
        if (rol != Rol.SOLICITANTE) tipoSolicitante = null;
        if (rol != Rol.OPERATIVO) tipoOperativo = null;

        // Validar dominio de correo
        boolean esInterno = (rol == Rol.ADMIN_AUDITORIO)
                || (rol == Rol.OPERATIVO)
                || (rol == Rol.SOLICITANTE && tipoSolicitante != TipoSolicitante.EXTERNO);
        boolean esExterno  = rol == Rol.SOLICITANTE && tipoSolicitante == TipoSolicitante.EXTERNO;

        if (esInterno) {
            if (correo == null || !correo.toLowerCase().endsWith("@poligran.edu.co"))
                throw new IllegalArgumentException("Los usuarios internos deben tener correo @poligran.edu.co");
        }
        if (esExterno) {
            if (correo != null && correo.toLowerCase().endsWith("@poligran.edu.co"))
                throw new IllegalArgumentException("Un agente externo no puede tener correo @poligran.edu.co");
        }

        correo = normalizarCorreo(correo);
        if (repo.existsByCorreoIgnoreCase(correo)) throw new IllegalArgumentException("Correo ya registrado");
        if (repo.existsByDocumento(documento)) throw new IllegalArgumentException("Documento ya registrado");
        validarPassword(password);

        return repo.save(Usuario.builder()
                .nombre(nombre).documento(documento).correo(correo)
                .telefono(telefono).organizacion(organizacion)
                .password(pe.encode(password.trim()))
                .rol(rol).tipoSolicitante(tipoSolicitante).tipoOperativo(tipoOperativo)
                .activo(true).build());
    }

    public Usuario actualizar(Long id, String nombre, String telefono, String organizacion,
                              Rol rol, TipoSolicitante tipoSolicitante, TipoOperativo tipoOperativo) {
        Usuario u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no existe"));
        u.setNombre(nombre);
        u.setTelefono(telefono);
        u.setOrganizacion(organizacion);
        if (rol != null) u.setRol(rol);

        if (u.getRol() == Rol.SOLICITANTE) {
            if (tipoSolicitante == null)
                throw new IllegalArgumentException("Tipo de solicitante requerido");
            u.setTipoSolicitante(tipoSolicitante);
            u.setTipoOperativo(null);
        } else if (u.getRol() == Rol.OPERATIVO) {
            if (tipoOperativo == null)
                throw new IllegalArgumentException("Sub-rol operativo requerido");
            u.setTipoOperativo(tipoOperativo);
            u.setTipoSolicitante(null);
        } else {
            u.setTipoSolicitante(null);
            u.setTipoOperativo(null);
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
                "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
    }

    public List<Usuario> listarTodos() { return repo.findAll(); }

    public ResultadoImportacion importarCsv(java.io.InputStream csv) throws java.io.IOException {
        ResultadoImportacion res = new ResultadoImportacion();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(csv, java.nio.charset.StandardCharsets.UTF_8))) {
            String linea;
            int num = 0;
            while ((linea = br.readLine()) != null) {
                num++;
                if (linea.isBlank()) continue;
                if (num == 1 && linea.toLowerCase().contains("nombre") && linea.toLowerCase().contains("documento")) continue;
                String[] c = linea.split(",", -1);
                if (c.length < 9) {
                    res.errores.add("Línea " + num + ": se esperan 9 columnas " +
                        "(nombre,documento,correo,telefono,organizacion,password,rol,tipoSolicitante,tipoOperativo)");
                    continue;
                }
                try {
                    Rol rol = Rol.valueOf(c[6].trim().toUpperCase());
                    TipoSolicitante tipo = c[7].trim().isEmpty() ? null
                            : TipoSolicitante.valueOf(c[7].trim().toUpperCase());
                    TipoOperativo tipoOp = c[8].trim().isEmpty() ? null
                            : TipoOperativo.valueOf(c[8].trim().toUpperCase());
                    crearPorAdmin(c[0].trim(), c[1].trim(), c[2].trim(), c[3].trim(), c[4].trim(),
                            c[5].trim(), rol, tipo, tipoOp);
                    res.exitos++;
                } catch (IllegalArgumentException e) {
                    res.errores.add("Línea " + num + " (" + (c.length > 2 ? c[2] : "?") + "): " + e.getMessage());
                }
            }
        }
        return res;
    }

    public static class ResultadoImportacion {
        public int exitos = 0;
        public java.util.List<String> errores = new java.util.ArrayList<>();
    }

    public Usuario porId(Long id) { return repo.findById(id).orElseThrow(); }
    public Usuario porCorreo(String correo) {
        return repo.findByCorreoIgnoreCase(correo.trim()).orElseThrow();
    }
}
