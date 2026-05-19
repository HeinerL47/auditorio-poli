package co.edu.poligran.auditorio.config;

import co.edu.poligran.auditorio.model.*;
import co.edu.poligran.auditorio.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Siembra los usuarios de prueba verificando cada correo individualmente.
 * De esta forma, si ya hay otros usuarios en la BD, los usuarios nuevos
 * (asistente, tecnología, etc.) se crean igualmente si no existen aún.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final TarifaRepository  tarifas;
    private final BloqueoRepository bloqueos;
    private final PasswordEncoder pe;

    public DataSeeder(UsuarioRepository u, TarifaRepository t, BloqueoRepository b, PasswordEncoder pe) {
        this.usuarios = u; this.tarifas = t; this.bloqueos = b; this.pe = pe;
    }

    @Override
    public void run(String... args) {
        // ── Administrador ──────────────────────────────────────────────────
        seedIfAbsent("admin@poligran.edu.co",
                "Admin Auditorio", "1000000001", "admin123",
                Rol.ADMIN_AUDITORIO, null, null);

        // ── Roles Operativos ───────────────────────────────────────────────
        seedIfAbsent("asistente@poligran.edu.co",
                "Asistente Demo", "1000000010", "asistente123",
                Rol.OPERATIVO, null, TipoOperativo.ASISTENTE);

        seedIfAbsent("tecnologia@poligran.edu.co",
                "Tecnologia Demo", "1000000011", "tec123",
                Rol.OPERATIVO, null, TipoOperativo.TECNOLOGIA);

        seedIfAbsent("audiovisual@poligran.edu.co",
                "Audiovisual Demo", "1000000012", "av123",
                Rol.OPERATIVO, null, TipoOperativo.AUDIOVISUAL);

        seedIfAbsent("infraestructura@poligran.edu.co",
                "Infraestructura Demo", "1000000013", "infra123",
                Rol.OPERATIVO, null, TipoOperativo.INFRAESTRUCTURA);

        seedIfAbsent("operaciones@poligran.edu.co",
                "Operaciones Demo", "1000000014", "ops123",
                Rol.OPERATIVO, null, TipoOperativo.OPERACIONES);

        // ── Solicitantes ───────────────────────────────────────────────────
        seedIfAbsent("docente@poligran.edu.co",
                "Docente Demo", "1000000003", "docente123",
                Rol.SOLICITANTE, TipoSolicitante.DOCENTE, null);

        seedIfAbsent("admin.staff@poligran.edu.co",
                "Administrativo Demo", "1000000004", "staff123",
                Rol.SOLICITANTE, TipoSolicitante.ADMINISTRATIVO, null);

        seedIfAbsent("externo@empresa.com",
                "Agente Externo Demo", "1000000005", "externo123",
                Rol.SOLICITANTE, TipoSolicitante.EXTERNO, null);

        // ── Tarifas (solo si no existen) ───────────────────────────────────
        if (tarifas.count() == 0) {
            tarifas.save(Tarifa.builder().seccion(Seccion.B1).valorHora(new BigDecimal("80000")).build());
            tarifas.save(Tarifa.builder().seccion(Seccion.B2).valorHora(new BigDecimal("80000")).build());
            tarifas.save(Tarifa.builder().seccion(Seccion.B3).valorHora(new BigDecimal("280000")).build());
        }

        // ── Bloqueos recurrentes (solo si no existen) ──────────────────────
        if (bloqueos.count() == 0) {
            bloqueos.save(Bloqueo.builder().motivo("Clase de Bienestar Universitario")
                    .seccion(Seccion.B3).diaSemana(DayOfWeek.MONDAY)
                    .horaInicio(LocalTime.of(10,0)).horaFin(LocalTime.of(12,0)).recurrente(true).build());
            bloqueos.save(Bloqueo.builder().motivo("Clase de Bienestar Universitario")
                    .seccion(Seccion.B3).diaSemana(DayOfWeek.WEDNESDAY)
                    .horaInicio(LocalTime.of(10,0)).horaFin(LocalTime.of(12,0)).recurrente(true).build());
            bloqueos.save(Bloqueo.builder().motivo("Mantenimiento general")
                    .seccion(Seccion.B3).diaSemana(DayOfWeek.SUNDAY)
                    .horaInicio(LocalTime.of(8,0)).horaFin(LocalTime.of(21,0)).recurrente(true).build());
        }
    }

    /**
     * Crea el usuario solo si su correo aún no existe en la base de datos.
     * Así funciona aunque ya haya otros usuarios registrados.
     */
    private void seedIfAbsent(String correo, String nombre, String doc, String pass,
                               Rol rol, TipoSolicitante tipoSol, TipoOperativo tipoOp) {
        if (!usuarios.existsByCorreoIgnoreCase(correo)) {
            String docFinal = doc;
            if (usuarios.existsByDocumento(doc)) {
                docFinal = doc + "_seed";
            }
            usuarios.save(Usuario.builder()
                    .nombre(nombre).documento(docFinal).correo(correo)
                    .password(pe.encode(pass)).rol(rol)
                    .tipoSolicitante(tipoSol).tipoOperativo(tipoOp)
                    .activo(true).build());
        }
    }
}
