package com.tp.dto;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// PARTE 2 - DTO de reporte ejecutivo de ventas.
//
// QUE ES UN DTO: es un objeto simple que NO es entidad (no tiene @Entity ni se
// guarda en la BD). Sirve para transportar solo los datos resumidos que necesita
// el reporte, sin exponer toda la entidad FacturaVenta ni traer objetos pesados
// a memoria. Asi protegemos la capa de dominio y la consulta es mas rapida.
//
// COMO SE LLENA: no lo creamos a mano con setters, sino que JPA lo instancia
// directo desde la consulta con "SELECT new com.tp.dto.FacturaReporteDTO(...)".
// Por eso el ORDEN y los TIPOS del constructor deben coincidir EXACTAMENTE
// con los del SELECT del JPQL (ver FacturaReporteService).
//
// LOMBOK: @Getter/@Setter generan los accesores, @NoArgsConstructor es util
// para librerias como Jackson (Parte 3), y @AllArgsConstructor es OBLIGATORIO
// porque es el constructor que invoca la expresion "SELECT new" de JPQL.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacturaReporteDTO {

    // ID interno de la factura (clave primaria). Necesario para acciones de la grilla.
    private Long id;

    // Numero de comprobante (puede ser null en borradores no informados a ARCA).
    private Long numeroFactura;

    private Date fechaEmision;

    // Denominacion del cliente, o "Consumidor Final" si la factura no tiene
    // cliente asociado (se resuelve con COALESCE en el JPQL).
    private String clienteDenominacion;

    // Ej: "IVA Responsable Inscripto", "Monotributo", etc.
    private String condicionIva;

    // Ej: "Sucursal principal".
    private String puntoVentaDescripcion;

    private double importeTotal;

    // Estado de la factura (ej: "BORRADOR", "ACTIVA", "AUTORIZADA", "ANULADA").
    private String estado;

    // Cantidad de renglones/detalles de la factura (COUNT(d) en el JPQL).
    // Es long (minuscula) porque COUNT siempre devuelve un numero, nunca null.
    private long cantidadItems;
}
