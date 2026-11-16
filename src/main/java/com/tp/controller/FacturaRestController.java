package com.tp.controller;

import com.tp.dto.FacturaReporteDTO;
import com.tp.dto.FacturaRequestDTO;
import com.tp.service.FacturaService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/facturas")
public class FacturaRestController {

    private final FacturaService facturaService;

    public FacturaRestController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping
    public Page<FacturaReporteDTO> obtenerFacturasFiltradas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.tp.dto.FacturaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<FacturaReporteDTO> crear(@RequestBody FacturaRequestDTO dto) {
        com.tp.FacturaVenta fv = facturaService.crear(dto);
        FacturaReporteDTO reporte = new FacturaReporteDTO(
                fv.getId(),
                fv.getNumero(),
                fv.getFechaEmision(),
                fv.getCliente() != null ? fv.getCliente().getDenominacion() : "Consumidor Final",
                fv.getCondicionIva() != null ? fv.getCondicionIva().getDenominacion() : null,
                fv.getPuntoVenta() != null ? fv.getPuntoVenta().getDescripcion() : null,
                fv.getImporteTotal(),
                fv.getEstado(),
                fv.getDetalles().size());
        return ResponseEntity.status(HttpStatus.CREATED).body(reporte);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizar(@PathVariable Long id, @RequestBody FacturaRequestDTO dto) {
        facturaService.actualizar(id, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/autorizar-afip")
    public ResponseEntity<FacturaReporteDTO> autorizarAfip(@PathVariable Long id) throws Exception {
        com.tp.FacturaVenta fv = facturaService.autorizarConAfip(id);
        FacturaReporteDTO reporte = new FacturaReporteDTO(
                fv.getId(),
                fv.getNumero(),
                fv.getFechaEmision(),
                fv.getCliente() != null ? fv.getCliente().getDenominacion() : "Consumidor Final",
                fv.getCondicionIva() != null ? fv.getCondicionIva().getDenominacion() : null,
                fv.getPuntoVenta() != null ? fv.getPuntoVenta().getDescripcion() : null,
                fv.getImporteTotal(),
                fv.getEstado(),
                fv.getDetalles().size());
        return ResponseEntity.ok(reporte);
    }
}
