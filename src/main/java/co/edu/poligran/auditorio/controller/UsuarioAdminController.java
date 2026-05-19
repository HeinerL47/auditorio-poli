package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.Rol;
import co.edu.poligran.auditorio.model.TipoOperativo;
import co.edu.poligran.auditorio.model.TipoSolicitante;
import co.edu.poligran.auditorio.model.Usuario;
import co.edu.poligran.auditorio.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    private final UsuarioService usuarios;

    public UsuarioAdminController(UsuarioService u) { this.usuarios = u; }

    @GetMapping
    public String listar(Model m) {
        m.addAttribute("usuarios", usuarios.listarTodos());
        return "admin/usuarios";
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model m) {
        m.addAttribute("usuario", new Usuario());
        m.addAttribute("roles", Rol.values());
        m.addAttribute("tiposSolicitante", TipoSolicitante.values());
        m.addAttribute("tiposOperativo", TipoOperativo.values());
        m.addAttribute("modo", "crear");
        return "admin/usuario-form";
    }

    @PostMapping("/nuevo")
    public String crear(@RequestParam String nombre,
                        @RequestParam String documento,
                        @RequestParam String correo,
                        @RequestParam(required = false) String telefono,
                        @RequestParam(required = false) String organizacion,
                        @RequestParam String password,
                        @RequestParam Rol rol,
                        @RequestParam(required = false) TipoSolicitante tipoSolicitante,
                        @RequestParam(required = false) TipoOperativo tipoOperativo,
                        RedirectAttributes ra) {
        try {
            usuarios.crearPorAdmin(nombre, documento, correo, telefono, organizacion,
                    password, rol, tipoSolicitante, tipoOperativo);
            ra.addFlashAttribute("ok", "Usuario creado correctamente");
            return "redirect:/admin/usuarios";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/usuarios/nuevo";
        }
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Model m) {
        m.addAttribute("usuario", usuarios.porId(id));
        m.addAttribute("roles", Rol.values());
        m.addAttribute("tiposSolicitante", TipoSolicitante.values());
        m.addAttribute("tiposOperativo", TipoOperativo.values());
        m.addAttribute("modo", "editar");
        return "admin/usuario-form";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @RequestParam String nombre,
                         @RequestParam(required = false) String telefono,
                         @RequestParam(required = false) String organizacion,
                         @RequestParam Rol rol,
                         @RequestParam(required = false) TipoSolicitante tipoSolicitante,
                         @RequestParam(required = false) TipoOperativo tipoOperativo,
                         RedirectAttributes ra) {
        try {
            usuarios.actualizar(id, nombre, telefono, organizacion, rol, tipoSolicitante, tipoOperativo);
            ra.addFlashAttribute("ok", "Usuario actualizado");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam boolean activo,
                                @AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes ra) {
        Usuario actual = usuarios.porCorreo(ud.getUsername());
        if (actual.getId().equals(id)) {
            ra.addFlashAttribute("error", "No puedes desactivar tu propia cuenta");
            return "redirect:/admin/usuarios";
        }
        usuarios.cambiarEstado(id, activo);
        ra.addFlashAttribute("ok", activo ? "Usuario activado" : "Usuario desactivado");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/{id}/password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String nuevaPassword,
                                RedirectAttributes ra) {
        try {
            usuarios.resetPassword(id, nuevaPassword);
            ra.addFlashAttribute("ok", "Contraseña actualizada");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}
