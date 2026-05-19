package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UsuarioService usuarios;
    public AuthController(UsuarioService u) { this.usuarios = u; }

    @GetMapping("/")
    public String home() { return "redirect:/dashboard"; }

    @GetMapping("/login")
    public String login(HttpServletRequest request, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            return "redirect:/login?expirada";
        }
        return "login";
    }

    @GetMapping("/registro")
    public String registroForm() { return "registro"; }

    @PostMapping("/registro")
    public String registrar(@RequestParam String nombre,
                            @RequestParam String documento,
                            @RequestParam String correo,
                            @RequestParam(required = false) String telefono,
                            @RequestParam(required = false) String organizacion,
                            @RequestParam String password,
                            Model model) {
        try {
            usuarios.registrarExterno(nombre, documento, correo, telefono, organizacion, password);
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "registro";
        }
    }
}
