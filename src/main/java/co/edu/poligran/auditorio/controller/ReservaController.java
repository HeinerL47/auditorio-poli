package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.*;
import co.edu.poligran.auditorio.service.ReservaService;
import co.edu.poligran.auditorio.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/reservas")
public class ReservaController {
    private final ReservaService reservas;
    private final UsuarioService usuarios;

    public ReservaController(ReservaService r, UsuarioService u) {
        this.reservas = r; this.usuarios = u;
    }

    @GetMapping("/nueva")
    public String form(Model m, @AuthenticationPrincipal UserDetails ud) {
        Usuario u = usuarios.porCorreo(ud.getUsername());
        m.addAttribute("usuario", u);
        m.addAttribute("secciones", Seccion.values());
        return "reserva-form";
    }

    @PostMapping("/cotizar")
    @ResponseBody
    public CotizacionDTO cotizar(@RequestParam Seccion seccion,
                                 @RequestParam String inicio,
                                 @RequestParam String fin) {
        BigDecimal c = reservas.calcularCosto(seccion,
                LocalDateTime.parse(inicio), LocalDateTime.parse(fin));
        return new CotizacionDTO(c.toPlainString());
    }

    public record CotizacionDTO(String costo) {}

    @PostMapping("/nueva")
    public String crear(@AuthenticationPrincipal UserDetails ud,
                        @RequestParam String inicio,
                        @RequestParam String fin,
                        @RequestParam Seccion seccion,
                        @RequestParam String tipoEvento,
                        @RequestParam(required = false) String especificaciones,
                        Model m) {
        Usuario u = usuarios.porCorreo(ud.getUsername());
        try {
            reservas.crear(u, LocalDateTime.parse(inicio), LocalDateTime.parse(fin),
                    seccion, tipoEvento, especificaciones);
            return "redirect:/dashboard?ok";
        } catch (Exception e) {
            m.addAttribute("usuario", u);
            m.addAttribute("secciones", Seccion.values());
            m.addAttribute("error", e.getMessage());
            m.addAttribute("inicio", inicio);
            m.addAttribute("fin", fin);
            m.addAttribute("seccion", seccion);
            m.addAttribute("tipoEvento", tipoEvento);
            m.addAttribute("especificaciones", especificaciones);
            return "reserva-form";
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id,
                           @RequestParam(required = false) String motivo,
                           @AuthenticationPrincipal UserDetails ud,
                           RedirectAttributes ra) {
        Usuario u = usuarios.porCorreo(ud.getUsername());
        try {
            reservas.cancelar(id, u, motivo);
            ra.addFlashAttribute("ok", "Reserva cancelada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/{id}/observacion")
    public String observar(@PathVariable Long id, @RequestParam String texto,
                           @AuthenticationPrincipal UserDetails ud) {
        Usuario u = usuarios.porCorreo(ud.getUsername());
        reservas.agregarObservacion(id, u, texto);
        return "redirect:/dashboard";
    }
}
