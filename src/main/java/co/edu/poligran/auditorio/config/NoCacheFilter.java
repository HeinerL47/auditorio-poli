package co.edu.poligran.auditorio.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Evita que el navegador cachee paginas de la aplicacion (incl. bfcache al usar Atras/Adelante).
 */
@Component
public class NoCacheFilter extends OncePerRequestFilter {

    private static final String CACHE_HEADERS =
            "no-cache, no-store, must-revalidate, max-age=0, private";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (esPaginaProtegida(path)) {
            response.setHeader("Cache-Control", CACHE_HEADERS);
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            response.setHeader("Vary", "Cookie");
        }

        chain.doFilter(request, response);

        if (esPaginaProtegida(path) && !response.isCommitted()) {
            response.setHeader("Cache-Control", CACHE_HEADERS);
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
    }

    private boolean esPaginaProtegida(String path) {
        if (path == null) return false;
        if (path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.equals("/favicon.ico")) {
            return false;
        }
        return !path.equals("/login") && !path.equals("/registro")
                && !path.startsWith("/error");
    }
}
