package co.edu.poligran.auditorio.config;

import co.edu.poligran.auditorio.model.*;
import co.edu.poligran.auditorio.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

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
        if (usuarios.count() == 0) {
            // ── Administrador ──────────────────────────────────────────────
            crear("Admin Auditorio",     "1000000001", "admin@poligran.edu.co",
                  "admin123",    Rol.ADMIN_AUDITORIO, null, null);

            // ── Roles Operativos ───────────────────────────────────────────
            crear("Asistente Demo",      "1000000010", "asistente@poligran.edu.co",
                  "asistente123", Rol.OPERATIVO, null, TipoOperativo.ASISTENTE);
            crear("Tecnologia Demo",     "1000000011", "tecnologia@poligran.edu.co",
                  "tec123",       Rol.OPERATIVO, null, TipoOperativo.TECNOLOGIA);
            crear("Audiovisual Demo",    "1000000012", "audiovisual@poligran.edu.co",
                  "av123",        Rol.OPERATIVO, null, TipoOperativo.AUDIOVISUAL);
            crear("Infraestructura Demo","1000000013", "infraestructura@poligran.edu.co",
                  "infra123",     Rol.OPERATIVO, null, TipoOperativo.INFRAESTRUCTURA);
            crear("Operaciones Demo",    "1000000014", "operaciones@poligran.edu.co",
                  "ops123",       Rol.OPERATIVO, null, TipoOperativo.OPERACIONES);

            // ── Solicitantes ───────────────────────────────────────────────
            crear("Docente Demo",        "1000000003", "docente@poligran.edu.co",
                  "docente123",   Rol.SOLICITANTE, TipoSolicitante.DOCENTE, null);
            crear("Administrativo Demo", "1000000004", "admin.staff@poligran.edu.co",
                  "staff123",     Rol.SOLICITANTE, TipoSolicitante.ADMINISTRATIVO, null);
            crear("Agente Externo Demo", "1000000005", "externo@empresa.com",
                  "externo123",   Rol.SOLICITANTE, TipoSolicitante.EXTERNO, null);
        }

        if (tarifas.count() == 0) {
            tarifas.save(Tarifa.builder().seccion(Seccion.B1).valorHora(new BigDecimal("80000")).build());
            tarifas.save(Tarifa.builder().seccion(Seccion.B2).valorHora(new BigDecimal("80000")).build());
            tarifas.save(Tarifa.builder().seccion(Seccion.B3).valorHora(new BigDecimal("150000")).build());
            tarifas.save(Tarifa.builder().seccion(Seccion.COMPLETO).valorHora(new BigDecimal("280000")).build());
        }

        if (bloqueos.count() == 0) {
            bloqueos.save(Bloqueo.builder().motivo("Clase de Bienestar Universitario")
                    .seccion(Seccion.B3).diaSemana(DayOfWeek.MONDAY)
                    .horaInicio(LocalTime.of(10,0)).horaFin(LocalTime.of(12,0)).recurrente(true).build());
            bloqueos.save(Bloqueo.builder().motivo("Clase de Bienestar Universitario")
                    .seccion(Seccion.B3).diaSemana(DayOfWeek.WEDNESDAY)
                    .horaInicio(LocalTime.of(10,0)).horaFin(LocalTime.of(12,0)).recurrente(true).build());
            bloqueos.save(Bloqueo.builder().motivo("Mantenimiento general")
                    .seccion(Seccion.COMPLETO).diaSemana(DayOfWeek.SUNDAY)
                    .horaInicio(LocalTime.of(8,0)).horaFin(LocalTime.of(21,0)).recurrente(true).build());
        }
    }

    private void crear(String nombre, String doc, String correo, String pass,
                       Rol rol, TipoSolicitante tipoSol, TipoOperativo tipoOp) {
        usuarios.save(Usuario.builder()
                .nombre(nombre).documento(doc).correo(correo)
                .password(pe.encode(pass)).rol(rol)
                .tipoSolicitante(tipoSol).tipoOperativo(tipoOp)
                .activo(true).build());
    }
}
