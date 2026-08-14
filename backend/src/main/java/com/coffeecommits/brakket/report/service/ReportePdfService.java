package com.coffeecommits.brakket.report.service;

import com.coffeecommits.brakket.report.dto.ReporteResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ReportePdfService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** RF-50: arma el mismo reporte que ya se muestra en pantalla, como PDF descargable. */
    public byte[] generarPdf(ReporteResponse reporte) {
        Document documento = new Document(PageSize.A4, 36, 36, 54, 36);
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font metaFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font encabezadoFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font celdaFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            documento.add(new Paragraph(reporte.titulo(), tituloFont));
            documento.add(new Paragraph("Generado: " + reporte.fechaGeneracion().format(FORMATO_FECHA)
                    + "   ·   Solicitado por: " + reporte.usuarioSolicitante(), metaFont));
            documento.add(new Paragraph("Filtros: " + reporte.filtrosDescripcion(), metaFont));
            documento.add(Chunk.NEWLINE);

            if (reporte.filas().isEmpty()) {
                documento.add(new Paragraph("No hay datos para los filtros seleccionados.", metaFont));
            } else {
                PdfPTable tabla = new PdfPTable(reporte.columnas().size());
                tabla.setWidthPercentage(100);
                for (String columna : reporte.columnas()) {
                    PdfPCell celda = new PdfPCell(new Phrase(columna, encabezadoFont));
                    celda.setBackgroundColor(new Color(30, 41, 59));
                    celda.setPadding(6);
                    tabla.addCell(celda);
                }
                for (var fila : reporte.filas()) {
                    for (String valor : fila) {
                        PdfPCell celda = new PdfPCell(new Phrase(valor, celdaFont));
                        celda.setPadding(5);
                        tabla.addCell(celda);
                    }
                }
                documento.add(tabla);
            }

            documento.close();
            return salida.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte.", ex);
        }
    }
}