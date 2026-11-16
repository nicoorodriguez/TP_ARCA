import { FacturaService } from './FacturaService.js';
import { FacturaFiltrosDTO, FacturaPageDTO, FacturaReporteDTO, FacturaRequestDTO, FacturaDetalleRequestDTO } from './models/factura.dto.js';

let paginaActual = 0;
const tamanoPagina = 10;

interface ItemForm {
    articuloId: number;
    descripcion: string;
    cantidad: number;
    precioUnitario: number;
    porcentajeBonificacion: number;
}

const items: ItemForm[] = [];

function $(id: string): HTMLElement { return document.getElementById(id)!; }

function renderItems(): void {
    const tbody = $('items-tbody') as HTMLTableSectionElement;
    tbody.innerHTML = '';
    items.forEach((it, idx) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${it.articuloId}</td>
            <td>${it.descripcion}</td>
            <td>${it.cantidad}</td>
            <td>${it.precioUnitario}</td>
            <td>${it.porcentajeBonificacion}</td>
            <td><button class="btn btn-sm btn-danger" data-idx="${idx}">X</button></td>
        `;
        tbody.appendChild(tr);
    });
    tbody.querySelectorAll('button').forEach(b => {
        b.addEventListener('click', (e) => {
            const idx = Number((e.target as HTMLElement).getAttribute('data-idx'));
            items.splice(idx, 1);
            renderItems();
        });
    });
}

async function cargarFacturas(): Promise<void> {
    const filtros: FacturaFiltrosDTO = {
        fechaDesde: ($('filtro-fecha-desde') as HTMLInputElement).value || undefined,
        fechaHasta: ($('filtro-fecha-hasta') as HTMLInputElement).value || undefined,
        estado: ($('filtro-estado') as HTMLInputElement).value || undefined,
        page: paginaActual,
        size: tamanoPagina
    };
    try {
        const page: FacturaPageDTO = await FacturaService.obtenerFacturas(filtros);
        const tbody = $('facturas-tbody') as HTMLTableSectionElement;
        tbody.innerHTML = '';
        if (page.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">Sin resultados</td></tr>';
        } else {
            page.content.forEach(f => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${f.numeroFactura}</td>
                    <td>${f.fechaEmision}</td>
                    <td>${f.clienteDenominacion}</td>
                    <td>${f.condicionIva}</td>
                    <td>${f.puntoVentaDescripcion}</td>
                    <td>${f.importeTotal.toFixed(2)}</td>
                    <td>${f.estado}</td>
                    <td></td>
                `;
                const cell = tr.cells[tr.cells.length - 1];
                const anular = document.createElement('button');
                anular.textContent = 'Anular';
                anular.className = 'btn btn-sm btn-warning me-1';
                anular.addEventListener('click', async () => {
                    if (!confirm('¿Anular factura?')) return;
                    await FacturaService.actualizarEstado(f.numeroFactura, 'ANULADA');
                    await cargarFacturas();
                });
                const eliminar = document.createElement('button');
                eliminar.textContent = 'Eliminar';
                eliminar.className = 'btn btn-sm btn-danger';
                eliminar.addEventListener('click', async () => {
                    if (!confirm('¿Eliminar (anular) factura?')) return;
                    await FacturaService.eliminarFactura(f.numeroFactura);
                    await cargarFacturas();
                });
                cell.appendChild(anular);
                cell.appendChild(eliminar);
                tbody.appendChild(tr);
            });
        }
        // paginacion
        const info = $('pag-info');
        info.textContent = `Página ${page.number + 1} de ${page.totalPages} (${page.totalElements} total)`;
        ($('btn-prev') as HTMLButtonElement).disabled = page.number <= 0;
        ($('btn-next') as HTMLButtonElement).disabled = page.number >= page.totalPages - 1;
    } catch (e) {
        alert('Error al cargar: ' + (e as Error).message);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    $('btn-agregar-item').addEventListener('click', () => {
        const item: ItemForm = {
            articuloId: Number(($('item-articulo') as HTMLInputElement).value),
            descripcion: ($('item-desc') as HTMLInputElement).value,
            cantidad: Number(($('item-cant') as HTMLInputElement).value),
            precioUnitario: Number(($('item-precio') as HTMLInputElement).value),
            porcentajeBonificacion: Number(($('item-bonif') as HTMLInputElement).value || 0)
        };
        items.push(item);
        renderItems();
    });

    $('form-alta').addEventListener('submit', async (e) => {
        e.preventDefault();
        const dto: FacturaRequestDTO = {
            puntoVentaId: Number(($('alta-pv') as HTMLInputElement).value),
            clienteId: ($('alta-cliente') as HTMLInputElement).value ? Number(($('alta-cliente') as HTMLInputElement).value) : undefined,
            condicionIvaId: Number(($('alta-ci') as HTMLInputElement).value),
            tipoMonedaId: Number(($('alta-tm') as HTMLInputElement).value),
            importeCobrado: 0,
            importeSaldo: 0,
            estado: 'ACTIVA',
            detalles: items.map(i => ({
                articuloId: i.articuloId,
                descripcion: i.descripcion,
                cantidad: i.cantidad,
                precioUnitario: i.precioUnitario,
                porcentajeBonificacion: i.porcentajeBonificacion
            } as FacturaDetalleRequestDTO))
        };
        try {
            await FacturaService.crearFactura(dto);
            items.length = 0;
            renderItems();
            ($('form-alta') as HTMLFormElement).reset();
            await cargarFacturas();
        } catch (e) {
            alert('Error al crear: ' + (e as Error).message);
        }
    });

    $('filtros-form').addEventListener('submit', (e) => {
        e.preventDefault();
        paginaActual = 0;
        cargarFacturas();
    });

    $('btn-prev').addEventListener('click', () => { if (paginaActual > 0) { paginaActual--; cargarFacturas(); } });
    $('btn-next').addEventListener('click', () => { paginaActual++; cargarFacturas(); });

    cargarFacturas();
});