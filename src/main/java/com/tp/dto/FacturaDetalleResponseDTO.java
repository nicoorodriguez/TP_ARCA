package com.tp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDetalleResponseDTO {
    private Long id;
    private Long listaPrecioArticuloId;
    private Long articuloId;
    private String articuloCodigo;
    private String articuloDenominacion;
    private String descripcion;
    private double cantidad;
    private double precioUnitario;
    private double porcentajeBonificacion;
    private double importeSubtotal;
}
