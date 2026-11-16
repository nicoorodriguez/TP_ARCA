package com.tp.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class LookupController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/clientes")
    public List<Map<String, Object>> buscarClientes(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "20") int limit) {
        String jpql = "SELECT c FROM Cliente c " +
                "WHERE (:s IS NULL OR :s = '' OR LOWER(c.denominacion) LIKE LOWER(CONCAT('%', :s, '%')) " +
                "OR c.cuitCuil LIKE CONCAT('%', :s, '%')) " +
                "ORDER BY c.denominacion";
        TypedQuery<com.tp.Cliente> q = em.createQuery(jpql, com.tp.Cliente.class);
        q.setParameter("s", search == null ? "" : search.trim());
        q.setMaxResults(Math.max(1, Math.min(limit, 50)));
        return q.getResultList().stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("denominacion", c.getDenominacion());
            m.put("cuitCuil", c.getCuitCuil());
            return m;
        }).toList();
    }

    @GetMapping("/puntos-venta")
    public List<Map<String, Object>> listarPuntosVenta() {
        return em.createQuery("SELECT p FROM PuntoVenta p ORDER BY p.numero", com.tp.PuntoVenta.class)
                .getResultList().stream().map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("numero", p.getNumero());
                    m.put("descripcion", p.getDescripcion());
                    m.put("tipoEmision", p.getTipoEmision());
                    return m;
                }).toList();
    }

    @GetMapping("/condiciones-iva")
    public List<Map<String, Object>> listarCondicionesIva() {
        return em.createQuery("SELECT c FROM CondicionIva c ORDER BY c.denominacion", com.tp.CondicionIva.class)
                .getResultList().stream().map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("codigoAfip", c.getCodigoAfip());
                    m.put("denominacion", c.getDenominacion());
                    return m;
                }).toList();
    }

    @GetMapping("/tipos-moneda")
    public List<Map<String, Object>> listarTiposMoneda() {
        return em.createQuery("SELECT t FROM TipoMoneda t ORDER BY t.denominacion", com.tp.TipoMoneda.class)
                .getResultList().stream().map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("codigoAfip", t.getCodigoAfip());
                    m.put("denominacion", t.getDenominacion());
                    m.put("simbolo", t.getSimbolo());
                    return m;
                }).toList();
    }

    @GetMapping("/listas-precio")
    public List<Map<String, Object>> listarListasPrecio() {
        return em.createQuery("SELECT l FROM ListaPrecio l ORDER BY l.denominacion", com.tp.ListaPrecio.class)
                .getResultList().stream().map(l -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", l.getId());
                    m.put("codigo", l.getCodigo());
                    m.put("denominacion", l.getDenominacion());
                    return m;
                }).toList();
    }

    @GetMapping("/listas-precio/{id}/articulos")
    public List<Map<String, Object>> listarArticulosPorLista(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "30") int limit) {
        String jpql = "SELECT lpa FROM ListaPrecioArticulo lpa JOIN lpa.articulo a " +
                "WHERE lpa.listaPrecio.id = :id " +
                "AND (:s IS NULL OR :s = '' OR LOWER(a.denominacion) LIKE LOWER(CONCAT('%', :s, '%')) " +
                "OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', :s, '%'))) " +
                "ORDER BY a.denominacion";
        TypedQuery<com.tp.ListaPrecioArticulo> q = em.createQuery(jpql, com.tp.ListaPrecioArticulo.class);
        q.setParameter("id", id);
        q.setParameter("s", search == null ? "" : search.trim());
        q.setMaxResults(Math.max(1, Math.min(limit, 50)));
        return q.getResultList().stream().map(lpa -> {
            Map<String, Object> m = new HashMap<>();
            m.put("listaPrecioArticuloId", lpa.getId());
            m.put("articuloId", lpa.getArticulo() != null ? lpa.getArticulo().getId() : null);
            m.put("codigo", lpa.getArticulo() != null ? lpa.getArticulo().getCodigo() : null);
            m.put("denominacion", lpa.getArticulo() != null ? lpa.getArticulo().getDenominacion() : null);
            m.put("precioVenta", lpa.getPrecioVenta());
            return m;
        }).toList();
    }
}
