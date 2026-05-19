package co.edu.poligran.auditorio.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SessionApiController {

    @GetMapping("/session")
    public Map<String, Object> session(@AuthenticationPrincipal UserDetails user) {
        return Map.of(
                "authenticated", user != null,
                "correo", user != null ? user.getUsername() : ""
        );
    }
}
