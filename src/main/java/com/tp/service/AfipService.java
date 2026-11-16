package com.tp.service;

import com.iscamen.arca.model.dto.FacturaRequest;
import com.iscamen.arca.service.AfipFacturaService;
import com.iscamen.arca.util.P12Helper;
import com.iscamen.arca.wsaa.AfipWsaaClient;
import com.iscamen.arca.wsfe.AfipWsfeClient;
import com.tp.FacturaVenta;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;

@Service
public class AfipService {

    private static final boolean IS_PRODUCCION = false;

    public ResultadoAutorizacion autorizar(FacturaVenta fv) throws Exception {
        // 1. Cargar certificado desde classpath
        InputStream p12Stream = getClass().getClassLoader()
                .getResourceAsStream("certificados/factelectronica.p12");
        if (p12Stream == null) {
            throw new RuntimeException("No se encontro certificados/factelectronica.p12 en classpath");
        }
        Object[] kc = P12Helper.obtenerClaveYCertificado(p12Stream, com.iscamen.arca.config.ArcaConfig.getPassword());
        java.security.PrivateKey pk = (java.security.PrivateKey) kc[0];
        java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) kc[1];

        long cuit = Long.parseLong(com.iscamen.arca.config.ArcaConfig.getCuit());
        String servicio = com.iscamen.arca.config.ArcaConfig.getService();
        int ptoVta = 1;
        if (fv.getPuntoVenta() != null) {
            // usar el número de punto de venta configurado (para ARCA)
            ptoVta = fv.getPuntoVenta().getNumero();
            if (ptoVta <= 0 && fv.getPuntoVenta().getId() != null) {
                ptoVta = fv.getPuntoVenta().getId().intValue();
            }
        }

        // 2. Obtener Token y Sign
        String[] auth = AfipWsaaClient.obtenerTokenYSign(pk, cert, servicio, IS_PRODUCCION, String.valueOf(cuit));
        String token = auth[0];
        String sign = auth[1];

        // 3. Consultar ultimo comprobante autorizado
        int cbteTipo = 11; // Factura C
        int ultimoNro = AfipWsfeClient.obtenerUltimoComprobante(token, sign, cuit, ptoVta, cbteTipo, IS_PRODUCCION);
        int nuevoNro = ultimoNro + 1;

        // 4. Armar request
        FacturaRequest req = new FacturaRequest();
        req.setPtoVta(ptoVta);
        req.setCbteTipo(cbteTipo);
        req.setConcepto(1); // Productos
        req.setDocTipo(99); // Consumidor final
        req.setDocNro(0L);
        req.setCondicionIvaReceptor(5); // Consumidor final
        req.setImporteTotal(fv.getImporteTotal());

        // 5. Emitir
        ar.gov.afip.wsfe.FECAEResponse resp = AfipFacturaService.emitirComprobante(token, sign, cuit, nuevoNro, req);

        String resultado = "R";
        String cae = null;
        String vto = null;
        String motivo = null;
        if (resp.getFeDetResp() != null && !resp.getFeDetResp().getFECAEDetResponse().isEmpty()) {
            ar.gov.afip.wsfe.FECAEDetResponse det = resp.getFeDetResp().getFECAEDetResponse().get(0);
            resultado = det.getResultado();
            cae = det.getCAE();
            vto = det.getCAEFchVto();
            if (det.getObservaciones() != null && det.getObservaciones().getObs() != null
                    && !det.getObservaciones().getObs().isEmpty()) {
                motivo = det.getObservaciones().getObs().get(0).getMsg();
            }
        }
        if (resp.getFeCabResp() != null) {
            resultado = resp.getFeCabResp().getResultado();
        }

        return new ResultadoAutorizacion(resultado, cae, vto, motivo, nuevoNro);
    }

    public static class ResultadoAutorizacion {
        public final String resultado;
        public final String cae;
        public final String caeVto;
        public final String motivo;
        public final int numeroComprobante;

        public ResultadoAutorizacion(String resultado, String cae, String vto, String motivo, int nro) {
            this.resultado = resultado;
            this.cae = cae;
            this.caeVto = vto;
            this.motivo = motivo;
            this.numeroComprobante = nro;
        }
    }
}