package co.edu.poligran.auditorio.service;

import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.model.Rol;
import co.edu.poligran.auditorio.model.Usuario;
import co.edu.poligran.auditorio.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificacionService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ===== Identidad visual Politecnico Grancolombiano =====
    private static final String AZUL       = "#0F385A";
    private static final String AZUL_OSC   = "#08243d";
    private static final String CIAN       = "#1FB2DE";
    private static final String AMARILLO   = "#FBAF17";
    private static final String VERDE      = "#A6CE38";
    private static final String ROSA       = "#EC0677";
    private static final String GRIS_BG    = "#f4f6f9";
    private static final String GRIS_BORDE = "#e1e6ec";
    private static final String TEXTO      = "#1c1c1c";
    private static final String TEXTO_SUAVE= "#5c6772";

    private final JavaMailSender mailSender;
    private final UsuarioRepository usuarios;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Value("${spring.mail.username:}")
    private String mailUser;

    @Value("${spring.mail.password:}")
    private String mailPass;

    @Value("${app.public.url:}")
    private String publicUrl;

    public NotificacionService(JavaMailSender ms, UsuarioRepository u) {
        this.mailSender = ms; this.usuarios = u;
    }

    @PostConstruct
    public void verificarConfig() {
        if (!enabled) {
            log.warn("====================================================================");
            log.warn("  CORREO DESACTIVADO (app.mail.enabled=false o MAIL_ENABLED=false).");
            log.warn("  Las notificaciones solo se mostraran en consola.");
            log.warn("====================================================================");
            return;
        }
        if (mailUser == null || mailUser.isBlank()) {
            log.error("====================================================================");
            log.error("  MAIL_USERNAME no esta configurado.");
            log.error("  Define la variable de entorno MAIL_USERNAME=tu.correo@gmail.com");
            log.error("  Los correos NO se enviaran hasta que este configurado.");
            log.error("====================================================================");
            return;
        }
        if (mailPass == null || mailPass.isBlank()) {
            log.error("====================================================================");
            log.error("  MAIL_PASSWORD no esta configurado.");
            log.error("  Debe ser un App Password de Google (16 caracteres sin espacios).");
            log.error("  Generalo en: Cuenta Google > Seguridad > Contrasenas de aplicaciones");
            log.error("  Los correos NO se enviaran hasta que este configurado.");
            log.error("====================================================================");
            return;
        }
        log.info("====================================================================");
        log.info("  Correo SMTP configurado. Remitente: {}", from);
        log.info("  Cuenta Gmail: {}", mailUser);
        log.info("  Enviando correo de prueba de conexion...");
        log.info("====================================================================");
        probarConexion();
    }

    private void probarConexion() {
        try {
            mailSender.createMimeMessage();
            log.info("Conexion SMTP con Gmail establecida correctamente.");
        } catch (Exception e) {
            log.error("====================================================================");
            log.error("  ERROR al conectar con Gmail SMTP: {}", e.getMessage());
            log.error("  Posibles causas:");
            log.error("  1) MAIL_PASSWORD no es un App Password (usa el de 16 caracteres)");
            log.error("  2) La cuenta Gmail no tiene verificacion en 2 pasos activa");
            log.error("  3) Problema de red o firewall bloqueando el puerto 587");
            log.error("====================================================================");
        }
    }

    // ============ API publica ============

    public void notificarCreacion(Reserva r) {
        String asunto = "Reserva #" + r.getId() + " creada - PENDIENTE de aprobacion";
        String mensaje = "Hemos recibido tu solicitud de reserva. Esta <b>pendiente de aprobacion</b> por el equipo del Auditorio Institucional.";
        enviarSolicitante(r, asunto, mensaje, "PENDIENTE");
        notificarRoles(asunto,
            "Se ha creado una nueva reserva que requiere revision.",
            "PENDIENTE", r, List.of(Rol.ADMIN_AUDITORIO));
    }

    public void notificarDecision(Reserva r) {
        String estado = r.getEstado().name();
        String asunto = "Reserva #" + r.getId() + " - " + estado;
        String mensaje;
        if ("APROBADA".equals(estado)) {
            mensaje = "Tu reserva ha sido <b>aprobada</b>. Recibiras un recordatorio una hora antes del evento.";
        } else if ("RECHAZADA".equals(estado)) {
            mensaje = "Tu reserva ha sido <b>rechazada</b>. Si lo consideras necesario, comunicate con el equipo de Auditorio.";
        } else {
            mensaje = "El estado de tu reserva fue actualizado a <b>" + estado + "</b>.";
        }
        enviarSolicitante(r, asunto, mensaje, estado);
        if ("APROBADA".equals(estado)) {
            notificarRoles(asunto, "Hay un nuevo evento aprobado para alistamiento operativo.", estado, r, List.of(Rol.OPERATIVO));
        }
    }

    public void notificarCancelacion(Reserva r) {
        String asunto = "Reserva #" + r.getId() + " CANCELADA";
        String mensaje = "La reserva ha sido <b>cancelada</b>.";
        enviarSolicitante(r, asunto, mensaje, "CANCELADA");
        notificarRoles(asunto, "Una reserva ha sido cancelada.", "CANCELADA", r,
                List.of(Rol.ADMIN_AUDITORIO, Rol.OPERATIVO));
    }

    public void notificarRecordatorio(Reserva r) {
        String asunto = "Recordatorio: tu evento inicia en 1 hora - Reserva #" + r.getId();
        String mensaje = "Tu evento en el Auditorio Institucional inicia en aproximadamente <b>una hora</b>. Te esperamos.";
        enviarSolicitante(r, asunto, mensaje, "RECORDATORIO");
        notificarRoles(asunto, "Evento aprobado inicia en aproximadamente una hora.", "RECORDATORIO", r,
                List.of(Rol.OPERATIVO));
    }

    // ============ Envio ============

    private void enviarSolicitante(Reserva r, String asunto, String intro, String estadoVisual) {
        Usuario s = r.getSolicitante();
        String etiqueta = s.getTipoSolicitante() != null ? s.getTipoSolicitante().name() : "SOLICITANTE";
        String html = construirHtml(
                "Hola " + s.getNombre() + ",",
                intro,
                r,
                estadoVisual);
        enviar(s.getCorreo(), "[" + etiqueta + "] " + asunto, html);
    }

    private void notificarRoles(String asunto, String intro, String estadoVisual,
                                Reserva r, List<Rol> roles) {
        for (Usuario u : usuarios.findByRolIn(roles)) {
            if (!u.isActivo()) continue;
            String html = construirHtml(
                    "Hola " + u.getNombre() + ",",
                    intro,
                    r,
                    estadoVisual);
            enviar(u.getCorreo(), "[" + u.getRol().name() + "] " + asunto, html);
        }
    }

    private void enviar(String para, String asunto, String html) {
        if (!enabled) {
            log.info("[MAIL OFF - desactivado] Para: {} | Asunto: {}", para, asunto);
            return;
        }
        if (mailUser == null || mailUser.isBlank() || mailPass == null || mailPass.isBlank()) {
            log.warn("[MAIL OFF - sin credenciales] Para: {} | Asunto: {}", para, asunto);
            return;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(from, "Auditorio Institucional - Politecnico Grancolombiano");
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(html, true);
            ClassPathResource logo = new ClassPathResource("static/images/poli-logo.jpg");
            if (logo.exists()) {
                helper.addInline("poli-logo", logo, "image/jpeg");
            }
            mailSender.send(mime);
            log.info("[MAIL OK] Enviado a: {} | Asunto: {}", para, asunto);
        } catch (Exception e) {
            log.error("[MAIL ERROR] No se pudo enviar a: {} | Asunto: {}", para, asunto);
            log.error("[MAIL ERROR] Causa: {}", e.getMessage());
            log.error("[MAIL ERROR] Revisa: 1) App Password correcto  2) 2FA activo en Gmail  3) Puerto 587 no bloqueado");
            log.error("[MAIL ERROR] Detalle completo:", e);
        }
    }

    // ============ Plantilla HTML con identidad Poli ============

    private String construirHtml(String saludo, String intro, Reserva r, String estadoVisual) {
        String badgeColor = colorEstado(estadoVisual);
        String solicitante = escape(r.getSolicitante().getNombre());
        String inicio = r.getInicio().format(F);
        String fin = r.getFin().format(F);
        String seccion = r.getSeccion().name();
        String tipo = escape(r.getTipoEvento());
        String costo = r.getCosto() == null ? "Por definir" : ("$ " + r.getCosto());
        String idR = String.valueOf(r.getId());
        String enlace = (publicUrl != null && !publicUrl.isBlank()) ? publicUrl + "/dashboard" : "";

        StringBuilder sb = new StringBuilder(2048);
        sb.append("<!DOCTYPE html>")
          .append("<html lang=\"es\"><head><meta charset=\"UTF-8\"/>")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>")
          .append("<title>").append(escape(estadoVisual)).append(" - Reserva #").append(idR).append("</title>")
          .append("</head>")
          .append("<body style=\"margin:0;padding:0;background:").append(GRIS_BG)
          .append(";font-family:'Open Sans','Segoe UI',Roboto,Arial,sans-serif;color:").append(TEXTO).append(";\">")
          // Tabla wrapper
          .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"background:").append(GRIS_BG).append(";padding:24px 0;\">")
          .append("<tr><td align=\"center\">")
          .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"600\" style=\"max-width:600px;width:100%;background:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 2px 10px rgba(15,56,90,.07);\">")
          // Header con logo institucional embebido (cid:poli-logo)
          .append("<tr><td style=\"background:linear-gradient(90deg,").append(AZUL).append(" 0%,").append(AZUL_OSC).append(" 100%);padding:16px 24px;border-bottom:4px solid ").append(CIAN).append(";\">")
          .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\"><tr>")
          .append("<td valign=\"middle\" width=\"68\" style=\"padding-right:14px;\">")
          .append("<div style=\"background:#ffffff;padding:6px 8px;border-radius:6px;display:inline-block;line-height:0;\">")
          .append("<img src=\"cid:poli-logo\" alt=\"Politecnico Grancolombiano\" width=\"56\" height=\"38\" style=\"display:block;width:56px;height:auto;\"/>")
          .append("</div></td>")
          .append("<td valign=\"middle\" style=\"color:#ffffff;\">")
          .append("<div style=\"font-size:18px;font-weight:700;letter-spacing:.3px;line-height:1.1;\">Auditorio Institucional</div>")
          .append("<div style=\"font-size:10px;font-weight:400;letter-spacing:1.5px;text-transform:uppercase;opacity:.85;margin-top:3px;\">Politecnico Grancolombiano</div>")
          .append("</td></tr></table></td></tr>")
          // Cuerpo
          .append("<tr><td style=\"padding:28px 32px 8px 32px;\">")
          .append("<h2 style=\"color:").append(AZUL).append(";margin:0 0 14px 0;font-size:20px;border-left:5px solid ").append(CIAN).append(";padding-left:10px;line-height:1.2;\">").append(escape(saludo)).append("</h2>")
          .append("<p style=\"font-size:15px;line-height:1.55;margin:0 0 18px 0;color:").append(TEXTO).append(";\">").append(intro).append("</p>")
          // Badge estado
          .append("<p style=\"margin:0 0 18px 0;\"><span style=\"display:inline-block;background:").append(badgeColor).append(";color:#ffffff;font-weight:700;font-size:12px;letter-spacing:1px;padding:6px 14px;border-radius:20px;text-transform:uppercase;\">")
          .append(escape(estadoVisual)).append("</span></p>")
          // Tabla detalle
          .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"border:1px solid ").append(GRIS_BORDE).append(";border-radius:8px;overflow:hidden;margin:6px 0 22px 0;\">")
          .append(fila("Reserva", "#" + idR))
          .append(fila("Solicitante", solicitante))
          .append(fila("Tipo de evento", tipo))
          .append(fila("Seccion", seccion))
          .append(fila("Inicio", inicio))
          .append(fila("Fin", fin))
          .append(fila("Costo", costo))
          .append("</table>");

        if (!enlace.isBlank()) {
            sb.append("<p style=\"text-align:center;margin:0 0 18px 0;\">")
              .append("<a href=\"").append(escape(enlace)).append("\" style=\"display:inline-block;background:").append(AZUL).append(";color:#ffffff;text-decoration:none;font-weight:700;font-size:14px;padding:12px 26px;border-radius:6px;\">Ir al Auditorio</a>")
              .append("</p>");
        }

        sb.append("<p style=\"font-size:12px;color:").append(TEXTO_SUAVE).append(";margin:18px 0 0 0;line-height:1.5;\">Este es un correo automatico, por favor no respondas a este mensaje. Si necesitas ayuda escribe a <a href=\"mailto:auditorio@poligran.edu.co\" style=\"color:").append(CIAN).append(";text-decoration:none;\">auditorio@poligran.edu.co</a>.</p>")
          .append("</td></tr>")
          // Footer
          .append("<tr><td style=\"background:").append(AZUL_OSC).append(";color:#ffffff;padding:14px 24px;text-align:center;font-size:12px;letter-spacing:.5px;\">")
          .append("&copy; Politecnico Grancolombiano &nbsp;&middot;&nbsp; <span style=\"color:").append(CIAN).append(";font-weight:700;\">Somos diferentes, somos Poli.</span>")
          .append("</td></tr>")
          .append("</table>")
          .append("</td></tr></table>")
          .append("</body></html>");
        return sb.toString();
    }

    private String fila(String etiqueta, String valor) {
        return "<tr>" +
               "<td style=\"padding:10px 14px;background:" + GRIS_BG + ";color:" + AZUL + ";font-weight:700;font-size:13px;width:38%;border-bottom:1px solid " + GRIS_BORDE + ";\">" + escape(etiqueta) + "</td>" +
               "<td style=\"padding:10px 14px;color:" + TEXTO + ";font-size:14px;border-bottom:1px solid " + GRIS_BORDE + ";\">" + escape(valor) + "</td>" +
               "</tr>";
    }

    private String colorEstado(String estado) {
        if (estado == null) return AZUL;
        switch (estado) {
            case "APROBADA":    return VERDE;
            case "RECHAZADA":   return ROSA;
            case "CANCELADA":   return ROSA;
            case "PENDIENTE":   return AMARILLO;
            case "RECORDATORIO":return CIAN;
            default:            return AZUL;
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
