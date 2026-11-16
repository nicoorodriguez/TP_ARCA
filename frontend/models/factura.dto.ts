export interface FacturaReporteDTO {
    numeroFactura: number;
    fechaEmision: string;
    clienteDenominacion: string;
    condicionIva: string;
    puntoVentaDescripcion: string;
    importeTotal: number;
    estado: string;
    cantidadItems: number;
}

export interface FacturaDetalleRequestDTO {
    articuloId: number;
    descripcion?: string;
    cantidad: number;
    precioUnitario: number;
    porcentajeBonificacion: number;
}

export interface FacturaRequestDTO {
    numero?: number;
    fechaEmision?: string;
    puntoVentaId: number;
    clienteId?: number;
    condicionIvaId: number;
    tipoMonedaId: number;
    importeCobrado: number;
    importeSaldo: number;
    estado?: string;
    observaciones?: string;
    detalles: FacturaDetalleRequestDTO[];
}

export interface FacturaFiltrosDTO {
    fechaDesde?: string;
    fechaHasta?: string;
    estado?: string;
    montoMinimo?: number;
    page?: number;
    size?: number;
}

export interface FacturaPageDTO {
    content: FacturaReporteDTO[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}