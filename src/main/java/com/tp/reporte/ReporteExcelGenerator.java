package com.tp.reporte;

import com.tp.dto.FacturaReporteDTO;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Genera el archivo Excel (.xlsx) del reporte ejecutivo usando Apache POI.
//
// Cada fila del Excel = un FacturaReporteDTO. La primera fila es el encabezado
// en negrita, luego una fila por factura, y al final una fila de totales.
// POI trabaja con el modelo Workbook -> Sheet -> Row -> Cell.
public class ReporteExcelGenerator {

    public void generar(List<FacturaReporteDTO> datos, String rutaSalida)
            throws Exception {

        // XSSFWorkbook = libro Excel moderno (.xlsx). El try-with-resources lo
        // cierra solo al terminar, liberando memoria y cerrando el archivo.
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(rutaSalida)) {

            Sheet hoja = workbook.createSheet("Reporte Ventas");

            // Estilo negrita para la fila de encabezados.
            Font negrita = workbook.createFont();
            negrita.setBold(true);
            CellStyle estiloEncabezado = workbook.createCellStyle();
            estiloEncabezado.setFont(negrita);

            // Estilo de fecha dd/MM/yyyy: Excel guarda las fechas como numeros,
            // este formato le dice como mostrarlas. Sin esto se veria un numero
            // serial raro (ej: 45789) en vez de la fecha.
            CreationHelper helper = workbook.getCreationHelper();
            CellStyle estiloFecha = workbook.createCellStyle();
            estiloFecha.setDataFormat(
                    helper.createDataFormat().getFormat("dd/MM/yyyy"));

            // Estilo numerico con 2 decimales para los importes.
            CellStyle estiloMoneda = workbook.createCellStyle();
            estiloMoneda.setDataFormat(
                    helper.createDataFormat().getFormat("#,##0.00"));

            // Fila 0: encabezados.
            String[] encabezados = {
                    "Nro Factura", "Fecha Emision", "Cliente",
                    "Cond. IVA", "Punto Venta", "Importe Total",
                    "Cant. Items"
            };
            Row filaEncabezado = hoja.createRow(0);
            for (int i = 0; i < encabezados.length; i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(encabezados[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            // Filas 1..N: un DTO por fila. Se aplican los estilos de fecha y
            // moneda para que Excel los trate como valores (permiten ordenar,
            // filtrar y sumar dentro del Excel) y no como texto plano.
            int filaActual = 1;
            double totalGeneral = 0;
            for (FacturaReporteDTO dto : datos) {
                Row fila = hoja.createRow(filaActual++);

                // numeroFactura es Long (objeto, puede ser null): se protege
                // con el ternario para no lanzar NullPointerException.
                fila.createCell(0).setCellValue(
                        dto.getNumeroFactura() != null
                                ? dto.getNumeroFactura() : 0);

                Cell celdaFecha = fila.createCell(1);
                Date fecha = dto.getFechaEmision();
                if (fecha != null) {
                    celdaFecha.setCellValue(fecha);
                    celdaFecha.setCellStyle(estiloFecha);
                } else {
                    celdaFecha.setCellValue("-");
                }

                fila.createCell(2).setCellValue(
                        dto.getClienteDenominacion() != null
                                ? dto.getClienteDenominacion() : "");
                fila.createCell(3).setCellValue(
                        dto.getCondicionIva() != null
                                ? dto.getCondicionIva() : "");
                fila.createCell(4).setCellValue(
                        dto.getPuntoVentaDescripcion() != null
                                ? dto.getPuntoVentaDescripcion() : "");

                Cell celdaTotal = fila.createCell(5);
                celdaTotal.setCellValue(dto.getImporteTotal());
                celdaTotal.setCellStyle(estiloMoneda);

                fila.createCell(6).setCellValue(dto.getCantidadItems());

                totalGeneral += dto.getImporteTotal();
            }

            // Ultima fila: total general acumulado.
            Row filaTotal = hoja.createRow(filaActual);
            filaTotal.createCell(4).setCellValue("TOTAL:");
            filaTotal.getCell(4).setCellStyle(estiloEncabezado);
            Cell celdaTotalGeneral = filaTotal.createCell(5);
            celdaTotalGeneral.setCellValue(totalGeneral);
            celdaTotalGeneral.setCellStyle(estiloMoneda);

            // Ajusta el ancho de cada columna al contenido mas largo.
            // Se hace AL FINAL, cuando ya estan todas las filas cargadas.
            for (int i = 0; i < encabezados.length; i++) {
                hoja.autoSizeColumn(i);
            }

            // Vuelca el libro en memoria al archivo fisico.
            workbook.write(fos);
        }
    }
}
