package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.model.Rol;
import co.edu.poligran.auditorio.model.Usuario;
import co.edu.poligran.auditorio.service.ReservaService;
import co.edu.poligran.auditorio.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Sección "Mis Reservas": disponible para todos los roles.
 *
 * - Solicitantes ven solo sus propias reservas.
 * - Admin, Asistente y demás Operativos ven todas las reservas del sistema.
 */
@Controller
@RequestMapping("/mis-reservas")
public class MisReservasController {

    private final UsuarioService usuarios;
    private final ReservaService reservas;

    public MisReservasController(UsuarioService u, ReservaService r) {
        this.usuarios = u; this.reservas = r;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UserDetails ud, Model m) {
        Usuario u = usuarios.porCorreo(ud.getUsername());
        m.addAttribute("usuario", u);

        List<Reserva> lista;
        if (u.getRol() == Rol.ADMIN_AUDITORIO || u.esAsistente() || u.getRol() == Rol.OPERATIVO) {
            lista = reservas.listarTodas();
            m.addAttribute("titulo", "Todas las reservas");
        } else {
            lista = reservas.listarDe(u);
            m.addAttribute("titulo", "Mis reservas");
        }
        m.addAttribute("reservas", lista);
        return "mis-reservas";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails ud,
                          Model m) {
        Usuario u = usuarios.porCorreo(ud.getUsername());
        Reserva r = reservas.porId(id);

        // Solicitante solo puede ver sus propias reservas
        boolean esAdminOOperativo = u.getRol() == Rol.ADMIN_AUDITORIO
                || u.esAsistente()
                || u.getRol() == Rol.OPERATIVO;
        if (!esAdminOOperativo && !r.getSolicitante().getId().equals(u.getId())) {
            return "redirect:/mis-reservas";
        }

        m.addAttribute("usuario", u);
        m.addAttribute("reserva", r);
        return "mis-reservas-detalle";
    }
}
