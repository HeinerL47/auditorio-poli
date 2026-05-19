package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.model.Seccion;
import co.edu.poligran.auditorio.service.ReporteService;
import co.edu.poligran.auditorio.service.ReporteService.FiltroReporte;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.PrintWriter;
import java.time.LocalDate;
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
    public String reportes(@RequestParam(required = false) Seccion seccion,
                           @RequestParam(required = false, defaultValue = "mes") String periodo,
                           @RequestParam(required = false) String fechaRef,
                           Model m) {
        LocalDate ref = parseFechaRef(fechaRef);
        FiltroReporte filtro = new FiltroReporte(seccion, periodo, ref);
        var rango = reportes.calcularRango(filtro);

        m.addAttribute("filtroSeccion", seccion);
        m.addAttribute("filtroPeriodo", periodo);
        m.addAttribute("filtroFechaRef", ref);
        m.addAttribute("rangoDesde", rango.inicio());
        m.addAttribute("rangoHasta", rango.fin());
        m.addAttribute("secciones", Seccion.values());

        m.addAttribute("resumen",         reportes.resumenEstados(filtro));
        m.addAttribute("horario",         reportes.horarioMasSolicitado(filtro));
        m.addAttribute("dia",             reportes.diaSemanaMasSolicitado(filtro));
        m.addAttribute("mes",             reportes.mesMasSolicitado(filtro));
        m.addAttribute("seccion",         reportes.porSeccion(filtro));
        m.addAttribute("tipoSolicitante", reportes.porTipoSolicitante(filtro));
        m.addAttribute("intExt",          reportes.internosVsExternos(filtro));
        m.addAttribute("ingresos",        reportes.ingresosTotales(filtro));
        m.addAttribute("ingresosSec",     reportes.ingresosPorSeccion(filtro));
        m.addAttribute("historial",       reportes.historial(filtro));
        return "reportes";
    }

    @GetMapping("/historial.csv")
    public void exportar(@RequestParam(required = false) Seccion seccion,
                         @RequestParam(required = false, defaultValue = "mes") String periodo,
                         @RequestParam(required = false) String fechaRef,
                         HttpServletResponse resp) throws Exception {
        FiltroReporte filtro = new FiltroReporte(seccion, periodo, parseFechaRef(fechaRef));

        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition",
                "attachment; filename=reporte-auditorio-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv");

        PrintWriter w = resp.getWriter();
        w.print('\uFEFF');
        w.println("ID,Solicitante,Documento,Correo,Rol,Tipo Solicitante,Seccion," +
                  "Fecha Inicio,Fecha Fin,Duracion (h),Tipo Evento,Estado,Costo (COP),Motivo Rechazo,Motivo Cancelacion,Observaciones");

        for (Reserva r : reportes.historial(filtro)) {
            long mins = ChronoUnit.MINUTES.between(r.getInicio(), r.getFin());
            double horas = Math.round(mins / 60.0 * 100) / 100.0;
            w.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%.2f,%s,%s,%s,%s,%s,%s%n",
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
                q(r.getMotivoRechazo()),
                q(r.getMotivoCancelacion()),
                q(r.getObservaciones()));
        }
        w.flush();
    }

    private LocalDate parseFechaRef(String fechaRef) {
        if (fechaRef == null || fechaRef.isBlank()) return LocalDate.now();
        return LocalDate.parse(fechaRef);
    }

    private String q(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
