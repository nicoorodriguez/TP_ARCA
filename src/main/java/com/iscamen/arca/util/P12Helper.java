package com.iscamen.arca.util;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class P12Helper {

    public static Object[] obtenerClaveYCertificado(String p12Path, String p12Password) throws Exception {
        try (InputStream fis = new java.io.FileInputStream(p12Path)) {
            return obtenerClaveYCertificado(fis, p12Password);
        }
    }

    public static Object[] obtenerClaveYCertificado(InputStream p12Stream, String p12Password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream fis = p12Stream) {
            ks.load(fis, p12Password.toCharArray());
        }

        String alias = ks.aliases().nextElement();

        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, p12Password.toCharArray());
        X509Certificate certificate = (X509Certificate) ks.getCertificate(alias);

        return new Object[] { privateKey, certificate };
    }
}