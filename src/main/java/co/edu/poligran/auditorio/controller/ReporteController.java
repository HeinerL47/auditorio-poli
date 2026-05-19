package co.edu.poligran.auditorio.controller;

import co.edu.poligran.auditorio.model.Reserva;
import co.edu.poligran.auditorio.model.Seccion;
import co.edu.poligran.auditorio.service.ReporteService;
import co.edu.poligran.auditorio.service.ReporteService.FiltroReporte;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private static final DateTimeFormatter FDT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FD  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
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

        m.addAttribute("filtroSeccion",   seccion);
        m.addAttribute("filtroPeriodo",   periodo);
        m.addAttribute("filtroFechaRef",  ref);
        m.addAttribute("rangoDesde",      rango.inicio());
        m.addAttribute("rangoHasta",      rango.fin());
        m.addAttribute("secciones",       Seccion.values());
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

    /** Exporta el historial como archivo Excel (.xlsx) con columnas bien definidas. */
    @GetMapping("/historial.xlsx")
    public void exportarExcel(@RequestParam(required = false) Seccion seccion,
                              @RequestParam(required = false, defaultValue = "mes") String periodo,
                              @RequestParam(required = false) String fechaRef,
                              HttpServletResponse resp) throws Exception {

        FiltroReporte filtro = new FiltroReporte(seccion, periodo, parseFechaRef(fechaRef));

        String filename = "reporte-auditorio-" + LocalDateTime.now().format(FD) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment; filename=" + filename);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Reservas");

            // ── Estilos ───────────────────────────────────────────────────
            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setWrapText(true);

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            altStyle.setBorderBottom(BorderStyle.THIN);
            altStyle.setBorderTop(BorderStyle.THIN);
            altStyle.setBorderLeft(BorderStyle.THIN);
            altStyle.setBorderRight(BorderStyle.THIN);

            CellStyle normalStyle = wb.createCellStyle();
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.cloneStyleFrom(normalStyle);
            DataFormat fmt = wb.createDataFormat();
            moneyStyle.setDataFormat(fmt.getFormat("$#,##0"));

            CellStyle moneyAltStyle = wb.createCellStyle();
            moneyAltStyle.cloneStyleFrom(altStyle);
            moneyAltStyle.setDataFormat(fmt.getFormat("$#,##0"));

            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.cloneStyleFrom(normalStyle);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle centerAltStyle = wb.createCellStyle();
            centerAltStyle.cloneStyleFrom(altStyle);
            centerAltStyle.setAlignment(HorizontalAlignment.CENTER);

            // ── Fila 0: Título ────────────────────────────────────────────
            int NUM_COLS = 14;
            Row titleRow = sheet.createRow(0);
            titleRow.setHeight((short) 700);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE RESERVAS — AUDITORIO INSTITUCIONAL POLITÉCNICO GRANCOLOMBIANO");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, NUM_COLS - 1));

            // ── Fila 1: Sub-título (período) ──────────────────────────────
            var rango = reportes.calcularRango(filtro);
            Row subRow = sheet.createRow(1);
            subRow.setHeight((short) 400);
            Cell subCell = subRow.createCell(0);
            subCell.setCellValue("Período: " + rango.inicio().format(FD)
                    + " al " + rango.fin().format(FD)
                    + (seccion != null ? "  |  Sección: " + seccion : "  |  Todas las secciones")
                    + "  |  Generado: " + LocalDateTime.now().format(FDT));
            CellStyle subStyle = wb.createCellStyle();
            Font subFont = wb.createFont();
            subFont.setItalic(true);
            subFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            subStyle.setFont(subFont);
            subStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            subStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            subStyle.setAlignment(HorizontalAlignment.CENTER);
            subCell.setCellStyle(subStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, NUM_COLS - 1));

            // ── Fila 2: vacía ─────────────────────────────────────────────
            sheet.createRow(2);

            // ── Fila 3: Encabezados ───────────────────────────────────────
            String[] headers = {
                "ID", "Nombre Solicitante", "Documento", "Correo",
                "Rol", "Tipo Solicitante", "Sección",
                "Fecha", "Hora Inicio", "Hora Fin", "Duración (h)",
                "Tipo de Evento", "Estado", "Costo (COP)"
            };
            Row headerRow = sheet.createRow(3);
            headerRow.setHeight((short) 600);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // ── Filas de datos ────────────────────────────────────────────
            int rowNum = 4;
            for (Reserva r : reportes.historial(filtro)) {
                Row row = sheet.createRow(rowNum);
                boolean alt = (rowNum % 2 == 0);
                CellStyle base   = alt ? altStyle    : normalStyle;
                CellStyle money  = alt ? moneyAltStyle : moneyStyle;
                CellStyle center = alt ? centerAltStyle : centerStyle;

                long mins  = ChronoUnit.MINUTES.between(r.getInicio(), r.getFin());
                double hrs = Math.round(mins / 60.0 * 100) / 100.0;

                setCell(row, 0,  String.valueOf(r.getId()), center);
                setCell(row, 1,  r.getSolicitante().getNombre(), base);
                setCell(row, 2,  r.getSolicitante().getDocumento(), center);
                setCell(row, 3,  r.getSolicitante().getCorreo(), base);
                setCell(row, 4,  r.getSolicitante().getRol().name(), center);
                setCell(row, 5,  r.getSolicitante().getTipoSolicitante() != null
                                    ? r.getSolicitante().getTipoSolicitante().name() : "—", center);
                setCell(row, 6,  r.getSeccion().name(), center);
                setCell(row, 7,  r.getInicio().format(FD), center);
                setCell(row, 8,  r.getInicio().format(DateTimeFormatter.ofPattern("HH:mm")), center);
                setCell(row, 9,  r.getFin().format(DateTimeFormatter.ofPattern("HH:mm")), center);
                setNumCell(row, 10, hrs, center);
                setCell(row, 11, r.getTipoEvento(), base);
                setCell(row, 12, r.getEstado().name(), center);

                Cell costoCell = row.createCell(13);
                if (r.getCosto() != null) {
                    costoCell.setCellValue(r.getCosto().doubleValue());
                } else {
                    costoCell.setCellValue(0);
                }
                costoCell.setCellStyle(money);

                rowNum++;
            }

            // ── Fila de totales ───────────────────────────────────────────
            if (rowNum > 4) {
                Row totalRow = sheet.createRow(rowNum);
                CellStyle totalStyle = wb.createCellStyle();
                Font totalFont = wb.createFont();
                totalFont.setBold(true);
                totalStyle.setFont(totalFont);
                totalStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
                totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                totalStyle.setBorderTop(BorderStyle.MEDIUM);

                CellStyle totalMoney = wb.createCellStyle();
                totalMoney.cloneStyleFrom(totalStyle);
                DataFormat df2 = wb.createDataFormat();
                totalMoney.setDataFormat(df2.getFormat("$#,##0"));

                Cell lbl = totalRow.createCell(12);
                lbl.setCellValue("TOTAL:");
                lbl.setCellStyle(totalStyle);

                Cell tot = totalRow.createCell(13);
                tot.setCellFormula("SUM(N5:N" + rowNum + ")");
                tot.setCellStyle(totalMoney);

                // Total registros
                for (int i = 0; i < 12; i++) {
                    Cell tc = totalRow.createCell(i);
                    if (i == 0) {
                        tc.setCellValue((rowNum - 4) + " registro(s)");
                    }
                    tc.setCellStyle(totalStyle);
                }
            }

            // ── Ajustar ancho de columnas ─────────────────────────────────
            int[] colWidths = {
                2000,  // ID
                7000,  // Nombre
                4000,  // Documento
                8000,  // Correo
                4500,  // Rol
                4500,  // Tipo Solicitante
                3500,  // Sección
                3500,  // Fecha
                3200,  // Hora Inicio
                3200,  // Hora Fin
                3500,  // Duración
                7000,  // Tipo Evento
                3500,  // Estado
                4000   // Costo
            };
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i]);
            }

            // ── Auto filtro en encabezados ────────────────────────────────
            sheet.setAutoFilter(new CellRangeAddress(3, 3, 0, NUM_COLS - 1));

            // ── Fijar la fila de encabezados ──────────────────────────────
            sheet.createFreezePane(0, 4);

            wb.write(resp.getOutputStream());
        }
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private void setNumCell(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private LocalDate parseFechaRef(String fechaRef) {
        if (fechaRef == null || fechaRef.isBlank()) return LocalDate.now();
        return LocalDate.parse(fechaRef);
    }
}
