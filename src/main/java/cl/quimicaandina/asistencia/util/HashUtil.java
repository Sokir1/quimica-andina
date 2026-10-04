package cl.quimicaandina.asistencia.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Utilidad para calcular el hash SHA-256 en hexadecimal (64 caracteres). */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256(String texto) {
        if (texto == null) {
            texto = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("El algoritmo SHA-256 no esta disponible", e);
        }
    }
}
