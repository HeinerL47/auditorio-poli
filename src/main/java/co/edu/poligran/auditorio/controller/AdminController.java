package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.*;
import co.edu.poligran.auditorio.repository.BloqueoRepository;
import co.edu.poligran.auditorio.repository.TarifaRepository;
import co.edu.poligran.auditorio.service.ReservaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ReservaService reservas;
    private final TarifaRepository tarifas;
    private final BloqueoRepository bloqueos;

    public AdminController(ReservaService r, TarifaRepository t, BloqueoRepository b) {
        this.reservas = r; this.tarifas = t; this.bloqueos = b;
    }

    @GetMapping("/aprobaciones")
    public String aprobaciones(Model m) {
        m.addAttribute("pendientes", reservas.listarPendientes());
        return "admin/aprobaciones";
    }

    @PostMapping("/aprobar/{id}")
    public String aprobar(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        reservas.aprobar(id, ud.getUsername());
        return "redirect:/admin/aprobaciones";
    }

    @PostMapping("/rechazar/{id}")
    public String rechazar(@PathVariable Long id,
                           @RequestParam(required = false) String motivo,
                           @AuthenticationPrincipal UserDetails ud) {
        reservas.rechazar(id, ud.getUsername(), motivo);
        return "redirect:/admin/aprobaciones";
    }

    @GetMapping("/tarifas")
    public String tarifasView(Model m) {
        m.addAttribute("tarifas", tarifas.findAll());
        return "admin/tarifas";
    }

    @PostMapping("/tarifas/{id}")
    public String actualizarTarifa(@PathVariable Long id, @RequestParam BigDecimal valorHora) {
        Tarifa t = tarifas.findById(id).orElseThrow();
        t.setValorHora(valorHora);
        tarifas.save(t);
        return "redirect:/admin/tarifas";
    }

    @GetMapping("/bloqueos")
    public String bloqueosView(Model m) {
        m.addAttribute("bloqueos", bloqueos.findAll());
        m.addAttribute("secciones", Seccion.values());
        m.addAttribute("dias", DayOfWeek.values());
        return "admin/bloqueos";
    }

    @PostMapping("/bloqueos")
    public String crearBloqueoRecurrente(@RequestParam String motivo,
                                          @RequestParam(required = false) Seccion seccion,
                                          @RequestParam DayOfWeek diaSemana,
                                          @RequestParam String horaInicio,
                                          @RequestParam String horaFin) {
        bloqueos.save(Bloqueo.builder()
                .motivo(motivo)
                .seccion(seccion)
                .diaSemana(diaSemana)
                .horaInicio(LocalTime.parse(horaInicio))
                .horaFin(LocalTime.parse(horaFin))
                .recurrente(true).build());
        return "redirect:/admin/bloqueos";
    }

    @PostMapping("/bloqueos/fecha")
    public String crearBloqueoPorFecha(@RequestParam String motivo,
                                        @RequestParam(required = false) Seccion seccion,
                                        @RequestParam String inicio,
                                        @RequestParam String fin) {
        bloqueos.save(Bloqueo.builder()
                .motivo(motivo)
                .seccion(seccion)
                .inicio(LocalDateTime.parse(inicio))
                .fin(LocalDateTime.parse(fin))
                .recurrente(false).build());
        return "redirect:/admin/bloqueos";
    }

    @PostMapping("/bloqueos/{id}/eliminar")
    public String eliminarBloqueo(@PathVariable Long id) {
        bloqueos.deleteById(id);
        return "redirect:/admin/bloqueos";
    }
}
