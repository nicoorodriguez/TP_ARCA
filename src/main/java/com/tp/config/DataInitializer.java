package com.tp.config;

import com.tp.dto.FacturaDetalleRequestDTO;
import com.tp.dto.FacturaRequestDTO;
import com.tp.service.FacturaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final FacturaService facturaService;

    @PersistenceContext
    private EntityManager em;

    public DataInitializer(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Long count = em.createQuery("SELECT COUNT(f) FROM FacturaVenta f", Long.class).getSingleResult();
        if (count != null && count > 0) return;

        com.tp.Usuario u = new com.tp.Usuario("admin", "admin", "Admin", "Sistema");
        em.persist(u);

        com.tp.PuntoVenta pv = new com.tp.PuntoVenta(1, "Sucursal Principal", "CAE", "Calle 1", u);
        em.persist(pv);
        com.tp.CondicionIva ci = new com.tp.CondicionIva(1, "IVA Responsable Inscripto", u);
        em.persist(ci);
        com.tp.TipoMoneda tm = new com.tp.TipoMoneda("PES", "Pesos", "$", u);
        em.persist(tm);

        com.tp.ListaPrecio lp = new com.tp.ListaPrecio("L1", "Lista General", u);
        em.persist(lp);
        com.tp.Articulo art = new com.tp.Articulo("A1", "Articulo Ejemplo", null, null, u);
        em.persist(art);
        com.tp.ListaPrecioArticulo lpa = new com.tp.ListaPrecioArticulo(lp, 100.0, art, u);
        em.persist(lpa);
        em.flush();

        for (int i = 1; i <= 20; i++) {
            FacturaRequestDTO dto = new FacturaRequestDTO();
            dto.setNumero((long) (1000 + i));
            dto.setFechaEmision(new Date());
            dto.setPuntoVentaId(pv.getId());
            dto.setCondicionIvaId(ci.getId());
            dto.setTipoMonedaId(tm.getId());
            dto.setImporteCobrado(0);
            dto.setImporteSaldo(0);
            dto.setEstado("ACTIVA");
            dto.setDetalles(List.of(new FacturaDetalleRequestDTO(lpa.getId(), "Item " + i, 2, 50.0, 0.0)));
            facturaService.crear(dto);
        }
    }
}
