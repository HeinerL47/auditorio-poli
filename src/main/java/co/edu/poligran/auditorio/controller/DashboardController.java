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

import java.util.List;

@Controller
public class DashboardController {
    private final UsuarioService usuarios;
    private final ReservaService reservas;

    public DashboardController(UsuarioService u, ReservaService r) {
        this.usuarios = u; this.reservas = r;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model m) {
        Usuario u = usuarios.porCorreo(ud.getUsername());
        m.addAttribute("usuario", u);
        List<Reserva> mias;
        if (u.getRol() == Rol.ADMIN_AUDITORIO || u.getRol() == Rol.OPERATIVO) {
            mias = reservas.listarTodas();
        } else {
            mias = reservas.listarDe(u);
        }
        m.addAttribute("reservas", mias);
        m.addAttribute("pendientes", reservas.listarPendientes().size());
        return "dashboard";
    }
}
