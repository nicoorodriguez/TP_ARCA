package com.tp.service;

import com.tp.dto.FacturaReporteDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

// PARTE 2: consulta JPQL con proyeccion directa a DTO.
//
// IDEA CENTRAL: en lugar de traer entidades FacturaVenta completas a memoria
// (con todos sus detalles, cliente, punto de venta, etc.) y despues armar el
// reporte en Java, le pedimos a la BD que devuelva YA ARMADO el DTO mediante
// la expresion de constructor:
//
//   SELECT new com.tp.dto.FacturaReporteDTO(f.numero, f.fechaEmision, ...)
//
// Hibernate ejecuta el SQL, toma cada fila y llama a
// new FacturaReporteDTO(...) con esos 7 valores. Por eso el orden y los tipos
// deben coincidir con el @AllArgsConstructor del DTO.
//
// DETALLES RAROS EXPLICADOS:
// - COALESCE(c.denominacion, 'Consumidor Final'): si la factura no tiene
//   cliente (cliente_id null), c.denominacion seria null; COALESCE lo reemplaza
//   por el texto 'Consumidor Final'.
// - LEFT JOIN f.cliente c: trae igual las facturas sin cliente. Con JOIN comun
//   (inner) esas facturas se perderian.
// - LEFT JOIN f.detalles d + COUNT(d): cuenta cuantos items tiene cada factura.
//   El TP original usa "JOIN f.detalles" (inner), pero eso EXCLUYE a las
//   facturas sin detalles. Usamos LEFT JOIN para no perder ninguna factura.
//   COUNT(d) cuenta solo detalles no nulos, asi que una factura sin detalles
//   da cantidadItems = 0 (correcto).
// - GROUP BY: como hay una funcion de agregacion (COUNT), JPQL/SQL exige
//   agrupar por todos los demas campos del SELECT. Se agrupa por f.id ademas
//   de los visibles para no mezclar dos facturas que casualmente tengan el
//   mismo numero, fecha e importes.
public class FacturaReporteService {

    private final EntityManager em;

    public FacturaReporteService(EntityManager em) {
        this.em = em;
    }

    // Devuelve el reporte ejecutivo completo, sin filtros.
    public List<FacturaReporteDTO> generarReporte() {
        String jpql =
                "SELECT new com.tp.dto.FacturaReporteDTO(" +
                "  f.numero, " +
                "  f.fechaEmision, " +
                "  COALESCE(c.denominacion, 'Consumidor Final'), " +
                "  ci.denominacion, " +
                "  pv.descripcion, " +
                "  f.importeTotal, " +
                "  COUNT(d)" +
                ") " +
                "FROM FacturaVenta f " +
                "LEFT JOIN f.cliente c " +
                "JOIN f.condicionIva ci " +
                "JOIN f.puntoVenta pv " +
                "LEFT JOIN f.detalles d " +
                "GROUP BY f.id, f.numero, f.fechaEmision, " +
                "         c.denominacion, ci.denominacion, " +
                "         pv.descripcion, f.importeTotal " +
                "ORDER BY f.fechaEmision, f.numero";

        TypedQuery<FacturaReporteDTO> query =
                em.createQuery(jpql, FacturaReporteDTO.class);

        return query.getResultList();
    }
}
