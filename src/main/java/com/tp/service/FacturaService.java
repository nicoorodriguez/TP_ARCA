package com.tp.service;

import com.tp.dto.FacturaDetalleRequestDTO;
import com.tp.dto.FacturaReporteDTO;
import com.tp.dto.FacturaRequestDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FacturaService {

    @PersistenceContext
    private EntityManager em;

    // ---------- helpers ----------

    private com.tp.Usuario obtenerUsuarioAuditoria() {
        com.tp.Usuario u = em.find(com.tp.Usuario.class, 1L);
        if (u == null) {
            u = new com.tp.Usuario("admin", "admin", "Admin", "Sistema");
            em.persist(u);
            em.flush();
        }
        return u;
    }

    private com.tp.PuntoVenta buscarPuntoVenta(Long id) {
        com.tp.PuntoVenta pv = em.find(com.tp.PuntoVenta.class, id);
        if (pv == null) throw new IllegalArgumentException("No existe PuntoVenta con id " + id);
        return pv;
    }

    private com.tp.CondicionIva buscarCondicionIva(Long id) {
        com.tp.CondicionIva ci = em.find(com.tp.CondicionIva.class, id);
        if (ci == null) throw new IllegalArgumentException("No existe CondicionIva con id " + id);
        return ci;
    }

    private com.tp.TipoMoneda buscarTipoMoneda(Long id) {
        com.tp.TipoMoneda tm = em.find(com.tp.TipoMoneda.class, id);
        if (tm == null) throw new IllegalArgumentException("No existe TipoMoneda con id " + id);
        return tm;
    }

    private com.tp.Cliente buscarCliente(Long id) {
        if (id == null) return null;
        com.tp.Cliente c = em.find(com.tp.Cliente.class, id);
        if (c == null) throw new IllegalArgumentException("No existe Cliente con id " + id);
        return c;
    }

    private com.tp.ListaPrecioArticulo buscarListaPrecioArticulo(Long id) {
        com.tp.ListaPrecioArticulo lpa = em.find(com.tp.ListaPrecioArticulo.class, id);
        if (lpa == null) throw new IllegalArgumentException("No existe ListaPrecioArticulo con id " + id);
        return lpa;
    }

    private double calcularImportes(com.tp.FacturaVenta factura) {
        double total = 0;
        for (com.tp.FacturaVentaDetalle d : factura.getDetalles()) {
            double neto = d.getCantidad() * d.getPrecioUnitario() * (1 - d.getPorcentajeBonificacion() / 100.0);
            d.setImporteNeto(neto);
            double iva = neto * 0.21; // IVA 21% por defecto
            d.setImporteIva(iva);
            d.setImporteSubtotal(neto + iva);
            total += d.getImporteSubtotal();
        }
        factura.setImporteTotal(total);
        return total;
    }

    // ---------- read (paginated) ----------

    public Page<FacturaReporteDTO> buscarFacturasFiltradas(
            Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo,
            int page, int size) {

        StringBuilder jpql = new StringBuilder(
                "SELECT new com.tp.dto.FacturaReporteDTO(" +
                        "  f.id, " +
                        "  f.numero, " +
                        "  f.fechaEmision, " +
                        "  COALESCE(c.denominacion, 'Consumidor Final'), " +
                        "  ci.denominacion, " +
                        "  pv.descripcion, " +
                        "  f.importeTotal, " +
                        "  f.estado, " +
                        "  COUNT(d)" +
                        ") " +
                        "FROM FacturaVenta f " +
                        "LEFT JOIN f.cliente c " +
                        "JOIN f.condicionIva ci " +
                        "JOIN f.puntoVenta pv " +
                        "LEFT JOIN f.detalles d " +
                        "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            jpql.append("AND f.fechaEmision <= :fechaHasta ");
            params.put("fechaHasta", fechaHasta);
        }
        if (estado != null && !estado.trim().isEmpty()) {
            jpql.append("AND f.estado = :estado ");
            params.put("estado", estado);
        }
        if (montoMinimo != null) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            params.put("montoMinimo", montoMinimo);
        }

        jpql.append("GROUP BY f.id, f.numero, f.fechaEmision, " +
                "c.denominacion, ci.denominacion, pv.descripcion, f.importeTotal, f.estado " +
                "ORDER BY f.fechaEmision DESC, f.id DESC");

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        params.forEach(query::setParameter);

        // count total (separate simple count over facturas matching filters, approx)
        StringBuilder countJpql = new StringBuilder("SELECT COUNT(f) FROM FacturaVenta f WHERE 1=1 ");
        Map<String, Object> cparams = new HashMap<>();
        if (fechaDesde != null) { countJpql.append("AND f.fechaEmision >= :fechaDesde "); cparams.put("fechaDesde", fechaDesde); }
        if (fechaHasta != null) { countJpql.append("AND f.fechaEmision <= :fechaHasta "); cparams.put("fechaHasta", fechaHasta); }
        if (estado != null && !estado.trim().isEmpty()) { countJpql.append("AND f.estado = :estado "); cparams.put("estado", estado); }
        if (montoMinimo != null) { countJpql.append("AND f.importeTotal >= :montoMinimo "); cparams.put("montoMinimo", montoMinimo); }
        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);
        cparams.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        query.setFirstResult(page * size);
        query.setMaxResults(size);
        List<FacturaReporteDTO> content = query.getResultList();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    // ---------- create ----------

    @Transactional
    public com.tp.FacturaVenta crear(FacturaRequestDTO dto) {
        com.tp.PuntoVenta pv = buscarPuntoVenta(dto.getPuntoVentaId());
        com.tp.CondicionIva ci = buscarCondicionIva(dto.getCondicionIvaId());
        // Moneda: si no viene, usar la primera disponible (o la configurada por defecto)
        com.tp.TipoMoneda tm;
        if (dto.getTipoMonedaId() != null) {
            tm = buscarTipoMoneda(dto.getTipoMonedaId());
        } else {
            java.util.List<com.tp.TipoMoneda> monedas = em.createQuery(
                    "SELECT t FROM TipoMoneda t ORDER BY t.id", com.tp.TipoMoneda.class)
                    .setMaxResults(1).getResultList();
            if (monedas.isEmpty()) throw new IllegalArgumentException("No hay TipoMoneda configurado");
            tm = monedas.get(0);
        }
        com.tp.Cliente cli = buscarCliente(dto.getClienteId());

        com.tp.Usuario auditor = obtenerUsuarioAuditoria();

        com.tp.FacturaVenta factura = new com.tp.FacturaVenta(
                null, // borrador: sin numero de comprobante hasta facturar en ARCA
                dto.getFechaEmision() != null ? dto.getFechaEmision() : new Date(),
                pv, cli, ci, tm,
                dto.getImporteCobrado(), dto.getImporteSaldo(), 0,
                "BORRADOR",
                auditor);
        if (dto.getObservaciones() != null) factura.setObservaciones(dto.getObservaciones());

        if (dto.getDetalles() != null) {
            for (FacturaDetalleRequestDTO d : dto.getDetalles()) {
                com.tp.ListaPrecioArticulo lpa = buscarListaPrecioArticulo(d.getArticuloId());
                com.tp.FacturaVentaDetalle det = new com.tp.FacturaVentaDetalle(
                        lpa, d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario(),
                        d.getPorcentajeBonificacion(), 0, 0, 0);
                factura.agregarDetalle(det);
            }
        }
        calcularImportes(factura);

        em.persist(factura);
        em.flush();
        return factura;
    }

    // ---------- update ----------

    @Transactional
    public com.tp.FacturaVenta actualizar(Long id, FacturaRequestDTO dto) {
        com.tp.FacturaVenta factura = em.find(com.tp.FacturaVenta.class, id);
        if (factura == null) throw new jakarta.persistence.EntityNotFoundException("No existe Factura con id " + id);
        if ("AUTORIZADA".equalsIgnoreCase(factura.getEstado())) {
            throw new IllegalStateException("No se puede editar una factura ya autorizada en ARCA");
        }

        if (dto.getPuntoVentaId() != null) factura.setPuntoVenta(buscarPuntoVenta(dto.getPuntoVentaId()));
        if (dto.getCondicionIvaId() != null) factura.setCondicionIva(buscarCondicionIva(dto.getCondicionIvaId()));
        if (dto.getTipoMonedaId() != null) factura.setTipoMoneda(buscarTipoMoneda(dto.getTipoMonedaId()));
        // Permitir cambiar/limpiar cliente: si viene en el request, actualizar (null => consumidor final)
        factura.setCliente(buscarCliente(dto.getClienteId()));
        if (dto.getFechaEmision() != null) factura.setFechaEmision(dto.getFechaEmision());
        if (dto.getObservaciones() != null) factura.setObservaciones(dto.getObservaciones());
        factura.setImporteCobrado(dto.getImporteCobrado());
        factura.setImporteSaldo(dto.getImporteSaldo());

        // reemplazar detalles
        if (dto.getDetalles() != null) {
            List<com.tp.FacturaVentaDetalle> existentes = new ArrayList<>(factura.getDetalles());
            for (com.tp.FacturaVentaDetalle d : existentes) {
                em.remove(d);
            }
            factura.limpiarDetalles();
            for (FacturaDetalleRequestDTO d : dto.getDetalles()) {
                com.tp.ListaPrecioArticulo lpa = buscarListaPrecioArticulo(d.getArticuloId());
                com.tp.FacturaVentaDetalle det = new com.tp.FacturaVentaDetalle(
                        lpa, d.getDescripcion(), d.getCantidad(), d.getPrecioUnitario(),
                        d.getPorcentajeBonificacion(), 0, 0, 0);
                factura.agregarDetalle(det);
            }
        }
        calcularImportes(factura);

        em.merge(factura);
        em.flush();
        return factura;
    }

    // ---------- delete (logical annulment) ----------

    @Transactional
    public void eliminar(Long id) {
        com.tp.FacturaVenta factura = em.find(com.tp.FacturaVenta.class, id);
        if (factura == null) throw new jakarta.persistence.EntityNotFoundException("No existe Factura con id " + id);
        factura.setEstado("ANULADA");
        factura.setFechaAnulacion(new Date());
        em.merge(factura);
        em.flush();
    }

    // ---------- find by id ----------

    public com.tp.FacturaVenta buscarPorId(Long id) {
        com.tp.FacturaVenta f = em.find(com.tp.FacturaVenta.class, id);
        if (f == null) throw new jakarta.persistence.EntityNotFoundException("No existe Factura con id " + id);
        return f;
    }

    public com.tp.dto.FacturaResponseDTO obtenerPorId(Long id) {
        com.tp.FacturaVenta f = buscarPorId(id);
        // forzar carga de detalles
        f.getDetalles().size();
        java.util.List<com.tp.dto.FacturaDetalleResponseDTO> dets = new java.util.ArrayList<>();
        for (com.tp.FacturaVentaDetalle d : f.getDetalles()) {
            com.tp.ListaPrecioArticulo lpa = d.getListaPrecioArticulo();
            com.tp.Articulo art = lpa != null ? lpa.getArticulo() : null;
            dets.add(new com.tp.dto.FacturaDetalleResponseDTO(
                    d.getId(),
                    lpa != null ? lpa.getId() : null,
                    art != null ? art.getId() : null,
                    art != null ? art.getCodigo() : null,
                    art != null ? art.getDenominacion() : null,
                    d.getDescripcion(),
                    d.getCantidad(),
                    d.getPrecioUnitario(),
                    d.getPorcentajeBonificacion(),
                    d.getImporteSubtotal()
            ));
        }
        return new com.tp.dto.FacturaResponseDTO(
                f.getId(),
                f.getNumero(),
                f.getFechaEmision(),
                f.getPuntoVenta() != null ? f.getPuntoVenta().getId() : null,
                f.getPuntoVenta() != null ? f.getPuntoVenta().getDescripcion() : null,
                f.getCliente() != null ? f.getCliente().getId() : null,
                f.getCliente() != null ? f.getCliente().getDenominacion() : "Consumidor Final",
                f.getCondicionIva() != null ? f.getCondicionIva().getId() : null,
                f.getCondicionIva() != null ? f.getCondicionIva().getDenominacion() : null,
                f.getTipoMoneda() != null ? f.getTipoMoneda().getId() : null,
                f.getTipoMoneda() != null ? f.getTipoMoneda().getDenominacion() : null,
                f.getImporteTotal(),
                f.getEstado(),
                f.getObservaciones(),
                dets
        );
    }

    @Transactional
    public com.tp.FacturaVenta autorizarConAfip(Long id) throws Exception {
        com.tp.FacturaVenta factura = buscarPorId(id);
        if ("AUTORIZADA".equalsIgnoreCase(factura.getEstado())) {
            throw new IllegalStateException("La factura ya está autorizada");
        }
        AfipService afip = new AfipService();
        AfipService.ResultadoAutorizacion res = afip.autorizar(factura);
        factura.setResultadoAfip(res.resultado);
        if (res.cae != null) factura.setCae(res.cae);
        if (res.caeVto != null) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
                factura.setCaeFechaVencimiento(sdf.parse(res.caeVto));
            } catch (Exception ignored) {}
        }
        if (res.motivo != null) factura.setMotivoRechazo(res.motivo);
        if ("A".equals(res.resultado)) {
            factura.setEstado("AUTORIZADA");
            // guardar el número de comprobante que devolvió/asignó ARCA
            if (res.numeroComprobante > 0) {
                factura.setNumero((long) res.numeroComprobante);
            }
        } else {
            factura.setEstado("RECHAZADA");
        }
        em.merge(factura);
        em.flush();
        return factura;
    }
}
