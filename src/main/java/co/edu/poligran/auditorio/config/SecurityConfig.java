package co.edu.poligran.auditorio.config;

import co.edu.poligran.auditorio.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository repo) {
        return correo -> repo.findByCorreo(correo)
                .map(u -> User.withUsername(u.getCorreo())
                        .password(u.getPassword())
                        .roles(u.getRol().name())
                        .disabled(!u.isActivo())
                        .build())
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
            .headers(h -> h.frameOptions(f -> f.disable()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/registro", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/api/disponibilidad").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN_AUDITORIO")
                .requestMatchers("/reservas/nueva", "/reservas/cotizar").hasRole("SOLICITANTE")
                .requestMatchers("/reservas/*/cancelar").hasAnyRole("SOLICITANTE","ADMIN_AUDITORIO")
                .requestMatchers("/reservas/*/observacion").hasAnyRole("SOLICITANTE","ADMIN_AUDITORIO","OPERATIVO")
                .requestMatchers("/reportes/**").hasAnyRole("ADMIN_AUDITORIO","OPERATIVO")
                .anyRequest().authenticated()
            )
            .formLogin(f -> f
                .loginPage("/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(l -> l.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }
}
