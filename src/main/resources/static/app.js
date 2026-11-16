import { FacturaService } from './FacturaService.js';

let paginaActual = 0;
const tamanoPagina = 10;
let items = [];
let modoEdicion = false;
let facturaEditId = null;
let modalFactura, modalCliente;

function $(id) { return document.getElementById(id); }

function fmtDate(d) {
    if (!d) return '';
    const s = String(d);
    return s.length >= 10 ? s.substring(0, 10) : s;
}
function fmtMoney(n) { return Number(n || 0).toFixed(2); }

function calcItemSubtotal(it) {
    const base = Number(it.cantidad) * Number(it.precioUnitario);
    const desc = base * (Number(it.porcentajeBonificacion || 0) / 100);
    return base - desc;
}
function actualizarTotal() {
    let total = 0;
    items.forEach(it => { total += calcItemSubtotal(it) * 1.21; }); // IVA 21% estimado
    $('factura-total').textContent = fmtMoney(total);
}

function renderItems() {
    const tbody = $('items-tbody');
    tbody.innerHTML = '';
    items.forEach((it, idx) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${it.articuloCodigo || it.articuloId || ''}</td>
            <td>${it.descripcion || ''}</td>
            <td>${it.cantidad}</td>
            <td>${fmtMoney(it.precioUnitario)}</td>
            <td>${it.porcentajeBonificacion || 0}</td>
            <td>${fmtMoney(calcItemSubtotal(it))}</td>
            <td><button type="button" class="btn btn-sm btn-danger" data-idx="${idx}">X</button></td>
        `;
        tbody.appendChild(tr);
    });
    tbody.querySelectorAll('button[data-idx]').forEach(b => {
        b.addEventListener('click', (e) => {
            const idx = Number(e.currentTarget.getAttribute('data-idx'));
            items.splice(idx, 1);
            renderItems();
            actualizarTotal();
        });
    });
    actualizarTotal();
}

async function cargarCombos() {
    try {
        const [pvs, cis, listas] = await Promise.all([
            FacturaService.listarPuntosVenta(),
            FacturaService.listarCondicionesIva(),
            FacturaService.listarListasPrecio()
        ]);
        const pvSel = $('factura-pv');
        pvSel.innerHTML = '<option value="">Seleccione...</option>' +
            pvs.map(p => `<option value="${p.id}">${p.numero ?? ''} - ${p.descripcion ?? ''}</option>`).join('');
        const ciSel = $('factura-ci');
        ciSel.innerHTML = '<option value="">Seleccione...</option>' +
            cis.map(c => `<option value="${c.id}">${c.denominacion ?? ''}</option>`).join('');
        const lpSel = $('item-lista-precio');
        lpSel.innerHTML = '<option value="">Seleccione lista...</option>' +
            listas.map(l => `<option value="${l.id}">${l.codigo ?? ''} - ${l.denominacion ?? ''}</option>`).join('');
    } catch (e) {
        console.error(e);
        alert('Error cargando datos iniciales: ' + e.message);
    }
}

function resetFormulario() {
    modoEdicion = false;
    facturaEditId = null;
    items = [];
    $('factura-id').value = '';
    $('form-factura').reset();
    $('factura-cliente-id').value = '';
    $('factura-cliente-nombre').value = 'Consumidor Final';
    $('factura-fecha').value = new Date().toISOString().substring(0,10);
    $('item-resultados').innerHTML = '';
    $('modalFacturaTitulo').textContent = 'Nueva Factura (borrador)';
    $('btn-guardar').textContent = 'Guardar borrador';
    renderItems();
}

async function abrirModalNueva() {
    resetFormulario();
    modalFactura.show();
}

async function abrirModalEditar(id) {
    try {
        const f = await FacturaService.obtenerFactura(id);
        if (String(f.estado).toUpperCase() === 'AUTORIZADA') {
            alert('No se puede editar una factura ya autorizada en ARCA.');
            return;
        }
        resetFormulario();
        modoEdicion = true;
        facturaEditId = f.id;
        $('modalFacturaTitulo').textContent = `Editar Factura #${f.id}${f.numero ? ' (Nro ' + f.numero + ')' : ''}`;
        $('btn-guardar').textContent = 'Guardar cambios';
        $('factura-id').value = f.id;
        $('factura-pv').value = f.puntoVentaId || '';
        $('factura-ci').value = f.condicionIvaId || '';
        $('factura-fecha').value = fmtDate(f.fechaEmision) || new Date().toISOString().substring(0,10);
        $('factura-cliente-id').value = f.clienteId || '';
        $('factura-cliente-nombre').value = f.clienteDenominacion || 'Consumidor Final';
        $('factura-obs').value = f.observaciones || '';
        items = (f.detalles || []).map(d => ({
            listaPrecioArticuloId: d.listaPrecioArticuloId || d.articuloId, // fallback
            articuloId: d.listaPrecioArticuloId || d.articuloId,
            articuloCodigo: d.articuloCodigo,
            descripcion: d.descripcion || d.articuloDenominacion,
            cantidad: Math.max(1, parseInt(d.cantidad, 10) || 1),
            precioUnitario: d.precioUnitario,
            porcentajeBonificacion: d.porcentajeBonificacion || 0
        }));
        renderItems();
        modalFactura.show();
    } catch (e) {
        alert('Error al cargar factura: ' + e.message);
    }
}

async function cargarFacturas() {
    const filtros = {
        fechaDesde: $('filtro-fecha-desde').value || undefined,
        fechaHasta: $('filtro-fecha-hasta').value || undefined,
        estado: $('filtro-estado').value || undefined,
        page: paginaActual,
        size: tamanoPagina
    };
    try {
        const page = await FacturaService.obtenerFacturas(filtros);
        const tbody = $('facturas-tbody');
        tbody.innerHTML = '';
        const rows = page.content || [];
        if (rows.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center">Sin resultados</td></tr>';
        } else {
            rows.forEach(f => {
                const tr = document.createElement('tr');
                const estado = (f.estado || '').toUpperCase();
                const puedeEditar = estado !== 'AUTORIZADA' && estado !== 'ANULADA';
                tr.innerHTML = `
                    <td>${f.id ?? ''}</td>
                    <td>${f.numeroFactura ?? '-'}</td>
                    <td>${fmtDate(f.fechaEmision)}</td>
                    <td>${f.clienteDenominacion ?? ''}</td>
                    <td>${f.condicionIva ?? ''}</td>
                    <td>${f.puntoVentaDescripcion ?? ''}</td>
                    <td>${fmtMoney(f.importeTotal)}</td>
                    <td>${f.estado ?? ''}</td>
                    <td></td>
                `;
                const cell = tr.cells[tr.cells.length - 1];
                const mkBtn = (text, cls, handler, disabled=false) => {
                    const b = document.createElement('button');
                    b.textContent = text;
                    b.className = 'btn btn-sm ' + cls + ' me-1 mb-1';
                    b.disabled = disabled;
                    b.addEventListener('click', handler);
                    return b;
                };
                cell.appendChild(mkBtn('Editar', 'btn-primary', () => abrirModalEditar(f.id), !puedeEditar));
                cell.appendChild(mkBtn('Facturar', 'btn-success', async () => {
                    if (!confirm('¿Facturar esta factura en ARCA?')) return;
                    try {
                        const res = await FacturaService.facturar(f.id);
                        alert(`Factura procesada. Estado: ${res.estado}${res.numeroFactura ? ', Nro: ' + res.numeroFactura : ''}`);
                        await cargarFacturas();
                    } catch (e) { alert('Error al facturar: ' + e.message); }
                }, estado === 'AUTORIZADA' || estado === 'ANULADA'));
                cell.appendChild(mkBtn('Anular', 'btn-warning', async () => {
                    if (!confirm('¿Anular factura?')) return;
                    try { await FacturaService.eliminarFactura(f.id); await cargarFacturas(); }
                    catch (e) { alert('Error: ' + e.message); }
                }, estado === 'ANULADA'));
                tbody.appendChild(tr);
            });
        }
        $('pag-info').textContent = `Página ${(page.number ?? 0) + 1} de ${page.totalPages ?? 1} (${page.totalElements ?? 0} total)`;
        $('btn-prev').disabled = (page.number ?? 0) <= 0;
        $('btn-next').disabled = (page.number ?? 0) >= (page.totalPages ?? 1) - 1;
    } catch (e) {
        alert('Error al cargar facturas: ' + e.message);
    }
}

async function buscarClientes() {
    const term = $('cliente-buscar-input').value || '';
    const cont = $('cliente-resultados');
    cont.innerHTML = '<div class="list-group-item">Buscando...</div>';
    try {
        const resultados = await FacturaService.buscarClientes(term);
        cont.innerHTML = '';
        if (!resultados.length) {
            cont.innerHTML = '<div class="list-group-item">Sin resultados</div>';
            return;
        }
        resultados.forEach(c => {
            const a = document.createElement('button');
            a.type = 'button';
            a.className = 'list-group-item list-group-item-action';
            a.textContent = `${c.denominacion || ''} - CUIT: ${c.cuitCuil || ''} (ID ${c.id})`;
            a.addEventListener('click', () => {
                $('factura-cliente-id').value = c.id;
                $('factura-cliente-nombre').value = `${c.denominacion} (${c.cuitCuil || ''})`;
                modalCliente.hide();
            });
            cont.appendChild(a);
        });
    } catch (e) {
        cont.innerHTML = `<div class="list-group-item text-danger">Error: ${e.message}</div>`;
    }
}

async function buscarArticulos() {
    const listaId = $('item-lista-precio').value;
    const term = $('item-buscar').value || '';
    const cont = $('item-resultados');
    cont.innerHTML = '';
    if (!listaId) { alert('Seleccione una lista de precios primero.'); return; }
    try {
        const resultados = await FacturaService.buscarArticulosPorLista(listaId, term);
        if (!resultados.length) {
            cont.innerHTML = '<div class="list-group-item">Sin resultados</div>';
            return;
        }
        resultados.forEach(a => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'list-group-item list-group-item-action';
            btn.textContent = `${a.codigo || ''} - ${a.denominacion || ''} | Precio: $${fmtMoney(a.precioVenta)}`;
            btn.addEventListener('click', () => {
                const cant = Math.max(1, parseInt($('item-cant').value || '1', 10));
                const bonif = Number($('item-bonif').value || 0);
                items.push({
                    articuloId: a.listaPrecioArticuloId, // ID de ListaPrecioArticulo para el backend
                    listaPrecioArticuloId: a.listaPrecioArticuloId,
                    articuloCodigo: a.codigo,
                    descripcion: a.denominacion,
                    cantidad: cant,
                    precioUnitario: Number(a.precioVenta),
                    porcentajeBonificacion: bonif
                });
                renderItems();
                $('item-buscar').value = '';
                cont.innerHTML = '';
            });
            cont.appendChild(btn);
        });
    } catch (e) {
        cont.innerHTML = `<div class="list-group-item text-danger">Error: ${e.message}</div>`;
    }
}

async function guardarFactura(e) {
    e.preventDefault();
    const pv = $('factura-pv').value;
    const ci = $('factura-ci').value;
    if (!pv || !ci) { alert('Seleccione punto de venta y condición de IVA.'); return; }
    if (items.length === 0) { alert('Agregue al menos un ítem.'); return; }
    const dto = {
        puntoVentaId: Number(pv),
        clienteId: $('factura-cliente-id').value ? Number($('factura-cliente-id').value) : null,
        condicionIvaId: Number(ci),
        fechaEmision: $('factura-fecha').value ? $('factura-fecha').value + 'T00:00:00' : null,
        observaciones: $('factura-obs').value || null,
        importeCobrado: 0,
        importeSaldo: 0,
        detalles: items.map(it => ({
            articuloId: Number(it.listaPrecioArticuloId || it.articuloId),
            descripcion: it.descripcion,
            cantidad: Number(it.cantidad),
            precioUnitario: Number(it.precioUnitario),
            porcentajeBonificacion: Number(it.porcentajeBonificacion || 0)
        }))
    };
    try {
        if (modoEdicion && facturaEditId) {
            await FacturaService.actualizarFactura(facturaEditId, dto);
            alert('Factura actualizada.');
        } else {
            await FacturaService.crearFactura(dto);
            alert('Borrador guardado.');
        }
        modalFactura.hide();
        await cargarFacturas();
    } catch (e) {
        alert('Error al guardar: ' + e.message);
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    modalFactura = new bootstrap.Modal($('modalFactura'));
    modalCliente = new bootstrap.Modal($('modalCliente'));

    await cargarCombos();
    $('btn-agregar').addEventListener('click', abrirModalNueva);
    $('form-factura').addEventListener('submit', guardarFactura);

    $('btn-buscar-cliente').addEventListener('click', () => { $('cliente-buscar-input').value = ''; $('cliente-resultados').innerHTML = ''; modalCliente.show(); buscarClientes(); });
    $('btn-cliente-buscar').addEventListener('click', buscarClientes);
    $('cliente-buscar-input').addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); buscarClientes(); } });
    $('btn-limpiar-cliente').addEventListener('click', () => { $('factura-cliente-id').value = ''; $('factura-cliente-nombre').value = 'Consumidor Final'; });

    $('btn-buscar-item').addEventListener('click', buscarArticulos);
    $('item-buscar').addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); buscarArticulos(); } });
    $('item-lista-precio').addEventListener('change', () => { $('item-resultados').innerHTML = ''; });

    $('filtros-form').addEventListener('submit', (e) => { e.preventDefault(); paginaActual = 0; cargarFacturas(); });
    $('btn-prev').addEventListener('click', () => { if (paginaActual > 0) { paginaActual--; cargarFacturas(); } });
    $('btn-next').addEventListener('click', () => { paginaActual++; cargarFacturas(); });

    cargarFacturas();
});
