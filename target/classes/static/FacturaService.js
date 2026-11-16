const BASE_URL = 'http://localhost:8080/api';

async function parseResponse(res) {
    if (res.ok) {
        const ct = res.headers.get('content-type') || '';
        if (res.status === 204) return null;
        if (ct.includes('application/json')) return await res.json();
        return await res.text();
    }
    let msg = `Error ${res.status}`;
    try {
        const data = await res.json();
        if (data && data.mensaje) msg = data.mensaje;
        else if (data && data.message) msg = data.message;
    } catch (e) {
        try { msg = await res.text() || msg; } catch (_) {}
    }
    throw new Error(msg);
}

export class FacturaService {
    static async obtenerFacturas(filtros) {
        const params = new URLSearchParams();
        if (filtros.fechaDesde) params.append('fechaDesde', filtros.fechaDesde);
        if (filtros.fechaHasta) params.append('fechaHasta', filtros.fechaHasta);
        if (filtros.estado) params.append('estado', filtros.estado);
        if (filtros.montoMinimo != null) params.append('montoMinimo', String(filtros.montoMinimo));
        params.append('page', String(filtros.page ?? 0));
        params.append('size', String(filtros.size ?? 10));
        const res = await fetch(`${BASE_URL}/facturas?${params.toString()}`);
        return await parseResponse(res);
    }

    static async obtenerFactura(id) {
        const res = await fetch(`${BASE_URL}/facturas/${id}`);
        return await parseResponse(res);
    }

    static async crearFactura(factura) {
        const res = await fetch(`${BASE_URL}/facturas`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(factura)
        });
        return await parseResponse(res);
    }

    static async actualizarFactura(id, factura) {
        const res = await fetch(`${BASE_URL}/facturas/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(factura)
        });
        return await parseResponse(res);
    }

    static async eliminarFactura(id) {
        const res = await fetch(`${BASE_URL}/facturas/${id}`, { method: 'DELETE' });
        return await parseResponse(res);
    }

    static async facturar(id) {
        const res = await fetch(`${BASE_URL}/facturas/${id}/autorizar-afip`, { method: 'POST' });
        return await parseResponse(res);
    }

    // Lookups
    static async listarPuntosVenta() {
        const res = await fetch(`${BASE_URL}/puntos-venta`);
        return await parseResponse(res);
    }
    static async listarCondicionesIva() {
        const res = await fetch(`${BASE_URL}/condiciones-iva`);
        return await parseResponse(res);
    }
    static async listarTiposMoneda() {
        const res = await fetch(`${BASE_URL}/tipos-moneda`);
        return await parseResponse(res);
    }
    static async listarListasPrecio() {
        const res = await fetch(`${BASE_URL}/listas-precio`);
        return await parseResponse(res);
    }
    static async buscarClientes(search) {
        const params = new URLSearchParams();
        if (search) params.append('search', search);
        params.append('limit', '20');
        const res = await fetch(`${BASE_URL}/clientes?${params.toString()}`);
        return await parseResponse(res);
    }
    static async buscarArticulosPorLista(listaId, search) {
        const params = new URLSearchParams();
        if (search) params.append('search', search);
        params.append('limit', '30');
        const res = await fetch(`${BASE_URL}/listas-precio/${listaId}/articulos?${params.toString()}`);
        return await parseResponse(res);
    }
}
