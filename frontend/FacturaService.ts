const BASE_URL = 'http://localhost:8080/api/facturas';

import { FacturaFiltrosDTO, FacturaPageDTO, FacturaReporteDTO, FacturaRequestDTO } from './models/factura.dto.js';

export class FacturaService {
    static async obtenerFacturas(filtros: FacturaFiltrosDTO): Promise<FacturaPageDTO> {
        const params = new URLSearchParams();
        if (filtros.fechaDesde) params.append('fechaDesde', filtros.fechaDesde);
        if (filtros.fechaHasta) params.append('fechaHasta', filtros.fechaHasta);
        if (filtros.estado) params.append('estado', filtros.estado);
        if (filtros.montoMinimo != null) params.append('montoMinimo', String(filtros.montoMinimo));
        params.append('page', String(filtros.page ?? 0));
        params.append('size', String(filtros.size ?? 10));

        const res = await fetch(`${BASE_URL}?${params.toString()}`);
        if (!res.ok) throw new Error(`Error ${res.status}`);
        return await res.json();
    }

    static async crearFactura(factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
        const res = await fetch(BASE_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(factura)
        });
        if (!res.ok) throw new Error(`Error ${res.status}`);
        return await res.json();
    }

    static async actualizarEstado(id: number, estado: string, observaciones?: string): Promise<void> {
        const res = await fetch(`${BASE_URL}/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ estado, observaciones })
        });
        if (!res.ok) throw new Error(`Error ${res.status}`);
    }

    static async eliminarFactura(id: number): Promise<void> {
        const res = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error(`Error ${res.status}`);
    }
}