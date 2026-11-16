package com.tp.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaRequestDTO {
    private Long numero;
    private Date fechaEmision;
    private Long puntoVentaId;
    private Long clienteId; // opcional
    private Long condicionIvaId;
    private Long tipoMonedaId;
    private double importeCobrado;
    private double importeSaldo;
    private String estado;
    private String observaciones;
    private List<FacturaDetalleRequestDTO> detalles;
}