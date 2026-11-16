package com.tp.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaDetalleRequestDTO {
    private Long articuloId; // id de ListaPrecioArticulo (o Articulo segun modelo)
    private String descripcion;
    private double cantidad;
    private double precioUnitario;
    private double porcentajeBonificacion;
}