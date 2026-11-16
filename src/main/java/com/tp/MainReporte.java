package com.tp;

import com.tp.dto.FacturaReporteDTO;
import com.tp.reporte.ReporteExcelGenerator;
import com.tp.reporte.ReportePdfGenerator;
import com.tp.service.FacturaReporteService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// PARTE 2: programa que ejecuta el reporte y genera los 2 archivos.
//
// FLUJO:
//  1. Si la tabla facturas_venta esta vacia, crea datos de ejemplo (2 facturas:
//     una con cliente y otra sin cliente para probar el "Consumidor Final").
//  2. Ejecuta la consulta JPQL con SELECT new -> List<FacturaReporteDTO>.
//  3. Muestra el resultado por consola y genera reporte-ventas.pdf y
//     reporte-ventas.xlsx en la carpeta del proyecto.
//
// PARA EJECUTAR: mvn compile exec:java -Dexec.mainClass="com.tp.MainReporte"
// o desde el IDE con Run sobre esta clase. Requiere PostgreSQL levantado
// (ver persistence.xml: jdbc:postgresql://127.0.0.1:5432/facturacion).
public class MainReporte {

    public static void main(String[] args) {
        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("FacturacionPU");
        try {
            // Paso 1: sembrar datos solo si no hay facturas (idempotente:
            // si lo corres dos veces, no duplica los datos).
            sembrarDatosSiVacio(emf);

            EntityManager em = emf.createEntityManager();
            try {
                // Paso 2: la consulta JPQL con proyeccion a DTO.
                FacturaReporteService servicio =
                        new FacturaReporteService(em);
                List<FacturaReporteDTO> reporte =
                        servicio.generarReporte();

                // Paso 3a: mostrar por consola para verificar sin abrir archivos.
                System.out.println("=== REPORTE EJECUTIVO (" +
                        reporte.size() + " facturas) ===");
                for (FacturaReporteDTO dto : reporte) {
                    System.out.println(
                            "Nro=" + dto.getNumeroFactura() +
                            " | Fecha=" + dto.getFechaEmision() +
                            " | Cliente=" + dto.getClienteDenominacion() +
                            " | IVA=" + dto.getCondicionIva() +
                            " | PV=" + dto.getPuntoVentaDescripcion() +
                            " | Total=" + dto.getImporteTotal() +
                            " | Items=" + dto.getCantidadItems());
                }

                // Paso 3b: generar los 2 archivos que pide el TP.
                // Se guardan en la carpeta desde donde se ejecuta (user.dir).
                String base = System.getProperty("user.dir") + "/";
                String rutaPdf = base + "reporte-ventas.pdf";
                String rutaExcel = base + "reporte-ventas.xlsx";

                new ReportePdfGenerator().generar(reporte, rutaPdf);
                System.out.println("PDF generado en: " + rutaPdf);

                new ReporteExcelGenerator().generar(reporte, rutaExcel);
                System.out.println("Excel generado en: " + rutaExcel);

            } finally {
                em.close();
            }
        } catch (Exception e) {
            // Se imprime el stack completo: si falla el JPQL (ej: nombre de
            // campo mal escrito) o la conexion a la BD, aca se ve la causa.
            e.printStackTrace();
        } finally {
            emf.close();
        }
    }

    // Crea 2 facturas de ejemplo SOLO si no hay ninguna en la BD.
    // Una factura lleva cliente (para ver su denominacion) y la otra lleva
    // cliente = null (para ver "Consumidor Final" del COALESCE).
    private static void sembrarDatosSiVacio(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            Long cantidad = em.createQuery(
                    "SELECT COUNT(f) FROM FacturaVenta f", Long.class)
                    .getSingleResult();
            if (cantidad > 0) {
                System.out.println(
                        "Ya existen " + cantidad +
                        " facturas, no se crean datos de ejemplo.");
                return;
            }

            em.getTransaction().begin();

            Usuario usuario = new Usuario(
                    "reporte", "clave", "Repo", "Rtero");

            PuntoVenta pv = new PuntoVenta(
                    1, "Sucursal principal", "Electronica",
                    "San Martin 123", usuario);

            // Entidades que la Parte 1 no usaba pero el reporte necesita.
            CondicionIva ivaInscripto = new CondicionIva(
                    1, "IVA Responsable Inscripto", usuario);
            TipoMoneda pesos = new TipoMoneda(
                    "PES", "Pesos Argentinos", "$", usuario);

            // Cliente de ejemplo con su contacto y domicilio (ambos
            // obligatorios por el modelo: @JoinColumn nullable = false).
            Contacto contacto = new Contacto(
                    "tech@empresa.com", "43123456", "2615551234");
            Domicilio domicilio = new Domicilio("Mitre", "1000");
            Cliente cliente = new Cliente(
                    "30-12345678-9", "Empresa Tech S.A.",
                    contacto, domicilio, usuario);

            Rubro rubro = new Rubro("Libreria", 1, usuario);
            Marca marca = new Marca("Marca de prueba", 1, usuario);
            Articulo cuaderno = new Articulo(
                    "ART-001", "Cuaderno", rubro, marca, usuario);
            Articulo carpeta = new Articulo(
                    "ART-002", "Carpeta", rubro, marca, usuario);
            ListaPrecio lista = new ListaPrecio(
                    "LP-001", "Minorista", usuario);
            ListaPrecioArticulo precioCuaderno = new ListaPrecioArticulo(
                    lista, 1000.0, cuaderno, usuario);
            ListaPrecioArticulo precioCarpeta = new ListaPrecioArticulo(
                    lista, 2000.0, carpeta, usuario);

            // Factura 1001: CON cliente, 2 detalles.
            FacturaVenta f1 = new FacturaVenta(
                    1001L, new Date(), pv,
                    cliente, ivaInscripto, pesos,
                    0.0, 4000.0, 4000.0, "EMITIDA", usuario);
            f1.agregarDetalle(new FacturaVentaDetalle(
                    precioCuaderno, "Dos cuadernos", 2.0, 1000.0,
                    0.0, 2000.0, 0.0, 2000.0));
            f1.agregarDetalle(new FacturaVentaDetalle(
                    precioCarpeta, "Una carpeta", 1.0, 2000.0,
                    0.0, 2000.0, 0.0, 2000.0));

            // Factura 1002: SIN cliente (null) -> el reporte debe mostrar
            // "Consumidor Final". Fecha de ayer para tener variedad.
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date ayer = cal.getTime();
            FacturaVenta f2 = new FacturaVenta(
                    1002L, ayer, pv,
                    null, ivaInscripto, pesos,
                    1500.0, 0.0, 1500.0, "EMITIDA", usuario);
            f2.agregarDetalle(new FacturaVentaDetalle(
                    precioCuaderno, "Un cuaderno", 1.0, 1000.0,
                    0.0, 1000.0, 0.0, 1000.0));
            // OJO: este detalle suma 1000 pero el total dice 1500 a proposito?
            // No: mantenemos coherencia -> ajustamos el total a 1000.
            f2.setImporteTotal(1000.0);
            f2.setImporteCobrado(1000.0);
            f2.setImporteSaldo(0.0);

            // Un solo persist por factura alcanza: las cascadas
            // (CascadeType.PERSIST / ALL) guardan cliente, punto de venta,
            // condicion, moneda, articulos y detalles relacionados.
            em.persist(f1);
            em.persist(f2);

            em.getTransaction().commit();
            System.out.println("Datos de ejemplo creados: 2 facturas.");
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
