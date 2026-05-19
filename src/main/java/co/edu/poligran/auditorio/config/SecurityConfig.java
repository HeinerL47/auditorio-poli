package co.edu.poligran.auditorio.config;

import co.edu.poligran.auditorio.model.Rol;
import co.edu.poligran.auditorio.model.TipoOperativo;
import co.edu.poligran.auditorio.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
     * Carga el usuario por correo (case-insensitive) y construye sus authorities.
     *
     * Authorities asignadas:
     *   - ROLE_<ROL>  para todos (ej: ROLE_OPERATIVO, ROLE_ADMIN_AUDITORIO, ROLE_SOLICITANTE)
     *   - ROLE_<TIPO_OPERATIVO>  adicionalmente cuando el usuario es OPERATIVO
     *     (ej: ROLE_ASISTENTE, ROLE_TECNOLOGIA, ROLE_AUDIOVISUAL, etc.)
     *
     * Esto permite que en SecurityConfig se use hasAnyRole("ADMIN_AUDITORIO","ASISTENTE")
     * y el ASISTENTE tenga exactamente los mismos permisos que el administrador.
     */
    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository repo) {
        return correo -> {
            // Case-insensitive para evitar problemas con mayúsculas al escribir el correo
            var usuario = repo.findByCorreoIgnoreCase(correo.trim())
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Usuario no encontrado: " + correo));

            List<GrantedAuthority> auths = new ArrayList<>();
            // Authority base por rol principal
            auths.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));

            // Si es OPERATIVO, agregar también el sub-rol como authority
            if (usuario.getRol() == Rol.OPERATIVO && usuario.getTipoOperativo() != null) {
                auths.add(new SimpleGrantedAuthority("ROLE_" + usuario.getTipoOperativo().name()));
            }

            return new org.springframework.security.core.userdetails.User(
                    usuario.getCorreo(),
                    usuario.getPassword(),
                    usuario.isActivo(),   // enabled
                    true,                 // accountNonExpired
                    true,                 // credentialsNonExpired
                    true,                 // accountNonLocked
                    auths
            );
        };
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
                // Observaciones: todos los autenticados
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
