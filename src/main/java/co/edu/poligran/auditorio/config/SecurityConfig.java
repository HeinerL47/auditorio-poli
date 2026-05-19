package co.edu.poligran.auditorio.config;

import co.edu.poligran.auditorio.model.TipoOperativo;
import co.edu.poligran.auditorio.model.Rol;
import co.edu.poligran.auditorio.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Construye las authorities del usuario.
     * - Todos los roles obtienen ROLE_<ROL>.
     * - Si el usuario es OPERATIVO con tipoOperativo = ASISTENTE,
     *   se agrega también ROLE_ASISTENTE para que tenga los mismos
     *   permisos que ADMIN_AUDITORIO en todas las rutas de seguridad.
     */
    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository repo) {
        return correo -> repo.findByCorreo(correo)
                .map(u -> {
                    List<GrantedAuthority> auths = new ArrayList<>();
                    auths.add(new SimpleGrantedAuthority("ROLE_" + u.getRol().name()));
                    if (u.getRol() == Rol.OPERATIVO && u.getTipoOperativo() != null) {
                        // Sub-rol operativo como authority adicional
                        auths.add(new SimpleGrantedAuthority("ROLE_" + u.getTipoOperativo().name()));
                    }
                    return new org.springframework.security.core.userdetails.User(
                            u.getCorreo(), u.getPassword(),
                            u.isActivo(), true, true, true, auths);
                })
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));
    }

    @Bean
    public DaoAuthenticationProvider authProvider(UserDetailsService uds, PasswordEncoder pe) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(uds);
        p.setPasswordEncoder(pe);
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(h -> h
                .frameOptions(f -> f.disable())
                .cacheControl(c -> {})
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/registro", "/css/**", "/js/**",
                                 "/images/**", "/favicon.ico", "/api/disponibilidad").permitAll()
                // Rutas de administración: admin + asistente tienen los mismos permisos
                .requestMatchers("/admin/**").hasAnyRole("ADMIN_AUDITORIO", "ASISTENTE")
                // Nueva reserva y cotizar: solicitantes + admin + asistente
                .requestMatchers("/reservas/nueva", "/reservas/cotizar")
                    .hasAnyRole("SOLICITANTE", "ADMIN_AUDITORIO", "ASISTENTE")
                // Cancelar: solicitante dueño + admin + asistente
                .requestMatchers("/reservas/*/cancelar")
                    .hasAnyRole("SOLICITANTE", "ADMIN_AUDITORIO", "ASISTENTE")
                // Observaciones: todos los roles (post-evento)
                .requestMatchers("/reservas/*/observacion").authenticated()
                // Reportes: admin, asistente y todos los operativos
                .requestMatchers("/reportes/**")
                    .hasAnyRole("ADMIN_AUDITORIO", "ASISTENTE", "OPERATIVO")
                // Mis reservas: todos los autenticados
                .requestMatchers("/mis-reservas/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(f -> f
                .loginPage("/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(l -> l
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            );
        return http.build();
    }
}
