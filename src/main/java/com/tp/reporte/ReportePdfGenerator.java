package com.tp.reporte;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.tp.dto.FacturaReporteDTO;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

// Genera el archivo PDF del reporte ejecutivo usando iText 5.
//
// ESTRUCTURA DEL PDF: titulo + fecha de generacion + tabla con una fila por
// DTO + linea de totales. Cada columna de la tabla corresponde a un campo del
// FacturaReporteDTO.
public class ReportePdfGenerator {

    // Formato de fecha legible para el PDF (distinto del Date crudo de la BD).
    private static final SimpleDateFormat FORMATO_FECHA =
            new SimpleDateFormat("dd/MM/yyyy");

    public void generar(List<FacturaReporteDTO> datos, String rutaSalida)
            throws Exception {

        // Document define pagina A4 horizontal (apaisada): la tabla tiene 7
        // columnas y en vertical quedaria muy apretada.
        Document document = new Document(PageSize.A4.rotate());

        // PdfWriter conecta el Document con el archivo fisico. El try garantiza
        // que el archivo se cierre aunque falle algo a mitad de camino.
        try (FileOutputStream fos = new FileOutputStream(rutaSalida)) {
            PdfWriter.getInstance(document, fos);
            document.open();

            // Titulo centrado con fuente mas grande y negrita.
            Font tituloFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 16);
            Paragraph titulo = new Paragraph(
                    "Reporte Ejecutivo de Ventas", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            // Fecha de generacion del reporte (no es fechaEmision de facturas).
            Font fechaFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 10);
            Paragraph fecha = new Paragraph(
                    "Generado el: " + FORMATO_FECHA.format(new Date()),
                    fechaFont);
            fecha.setAlignment(Element.ALIGN_CENTER);
            document.add(fecha);
            document.add(new Paragraph(" ")); // linea en blanco separadora

            // Tabla de 7 columnas (una por campo del DTO).
            PdfPTable tabla = new PdfPTable(7);
            tabla.setWidthPercentage(100); // ocupa todo el ancho de la pagina

            // Encabezados con fondo gris para distinguirlos de los datos.
            String[] encabezados = {
                    "Nro Factura", "Fecha Emision", "Cliente",
                    "Cond. IVA", "Punto Venta", "Importe Total",
                    "Cant. Items"
            };
            for (String h : encabezados) {
                PdfPCell celda = new PdfPCell(new Phrase(
                        h, FontFactory.getFont(
                                FontFactory.HELVETICA_BOLD, 10)));
                celda.setBackgroundColor(BaseColor.LIGHT_GRAY);
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(celda);
            }

            // Una fila por cada DTO. Si fechaEmision es null se muestra "-"
            // para no romper el SimpleDateFormat (format(null) lanza error).
            double totalGeneral = 0;
            for (FacturaReporteDTO dto : datos) {
                tabla.addCell(String.valueOf(dto.getNumeroFactura()));
                tabla.addCell(dto.getFechaEmision() != null
                        ? FORMATO_FECHA.format(dto.getFechaEmision())
                        : "-");
                tabla.addCell(dto.getClienteDenominacion());
                tabla.addCell(dto.getCondicionIva());
                tabla.addCell(dto.getPuntoVentaDescripcion());
                tabla.addCell(
                        String.format("%.2f", dto.getImporteTotal()));
                tabla.addCell(String.valueOf(dto.getCantidadItems()));
                totalGeneral += dto.getImporteTotal();
            }

            document.add(tabla);
            document.add(new Paragraph(" "));

            // Pie con el total acumulado (suma de importeTotal de las filas).
            Paragraph total = new Paragraph(
                    "Total general: " + String.format("%.2f", totalGeneral),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.close();
        }
    }
}
