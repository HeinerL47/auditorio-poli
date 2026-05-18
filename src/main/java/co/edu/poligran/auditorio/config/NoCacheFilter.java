package co.edu.poligran.auditorio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Evita que el navegador sirva paginas autenticadas desde cache
 * (p. ej. al usar "Atras" despues de cerrar sesion).
 */
@Component
public class NoCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();
        boolean recursoEstatico = path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/");

        if (auth != null && auth.isAuthenticated() && !recursoEstatico) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        chain.doFilter(request, response);
    }
}
