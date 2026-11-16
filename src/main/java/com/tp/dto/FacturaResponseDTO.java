package com.tp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaResponseDTO {
    private Long id;
    private Long numero;
    private Date fechaEmision;
    private Long puntoVentaId;
    private String puntoVentaDescripcion;
    private Long clienteId;
    private String clienteDenominacion;
    private Long condicionIvaId;
    private String condicionIvaDenominacion;
    private Long tipoMonedaId;
    private String tipoMonedaDenominacion;
    private double importeTotal;
    private String estado;
    private String observaciones;
    private List<FacturaDetalleResponseDTO> detalles;
}
