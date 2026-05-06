package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.service.ReporteService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ReporteService reportes;

    public ReporteController(ReporteService r) { this.reportes = r; }

    @GetMapping
    public String reportes(Model m) {
        m.addAttribute("resumen",         reportes.resumenEstados());
        m.addAttribute("horario",         reportes.horarioMasSolicitado());
        m.addAttribute("dia",             reportes.diaSemanaMasSolicitado());
        m.addAttribute("mes",             reportes.mesMasSolicitado());
        m.addAttribute("seccion",         reportes.porSeccion());
        m.addAttribute("tipoSolicitante", reportes.porTipoSolicitante());
        m.addAttribute("intExt",          reportes.internosVsExternos());
        m.addAttribute("ingresos",        reportes.ingresosTotales());
        m.addAttribute("ingresosSec",     reportes.ingresosPorSeccion());
        m.addAttribute("historial",       reportes.historial());
        return "reportes";
    }

    @GetMapping("/historial.csv")
    public void exportar(HttpServletResponse resp) throws Exception {
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition",
                "attachment; filename=reporte-auditorio-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv");

        PrintWriter w = resp.getWriter();
        w.print('\uFEFF');
        w.println("ID,Solicitante,Documento,Correo,Rol,Tipo Solicitante,Seccion," +
                  "Fecha Inicio,Fecha Fin,Duracion (h),Tipo Evento,Estado,Costo (COP),Observaciones");

        for (Reserva r : reportes.historial()) {
            long mins = ChronoUnit.MINUTES.between(r.getInicio(), r.getFin());
            double horas = Math.round(mins / 60.0 * 100) / 100.0;
            w.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%.2f,%s,%s,%s,%s%n",
                r.getId(),
                q(r.getSolicitante().getNombre()),
                q(r.getSolicitante().getDocumento()),
                q(r.getSolicitante().getCorreo()),
                r.getSolicitante().getRol(),
                r.getSolicitante().getTipoSolicitante() != null
                    ? r.getSolicitante().getTipoSolicitante() : "",
                r.getSeccion(),
                r.getInicio().format(F),
                r.getFin().format(F),
                horas,
                q(r.getTipoEvento()),
                r.getEstado(),
                r.getCosto() != null ? r.getCosto().toPlainString() : "0",
                q(r.getObservaciones()));
        }
        w.flush();
    }

    private String q(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
