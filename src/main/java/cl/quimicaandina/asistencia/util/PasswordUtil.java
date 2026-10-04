package cl.quimicaandina.asistencia.util;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;

/** Contraseñas nuevas con sal aleatoria; compatibilidad de lectura con SHA-256 histórico. */
public final class PasswordUtil {
    private static final int ITERACIONES = 600_000;
    private PasswordUtil() {}
    public static String hash(String clave) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return "pbkdf2$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
            + "$" + Base64.getEncoder().encodeToString(derivar(clave, sal, ITERACIONES));
    }
    public static boolean verificar(String clave, String guardada) {
        if (clave == null || guardada == null) return false;
        if (guardada.matches("[a-fA-F0-9]{64}")) {
            return MessageDigest.isEqual(HashUtil.sha256(clave).getBytes(StandardCharsets.US_ASCII),
                guardada.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        }
        try {
            String[] partes = guardada.split("\\$");
            if (partes.length != 4 || !partes[0].equals("pbkdf2")) return false;
            int iteraciones = Integer.parseInt(partes[1]);
            if (iteraciones < 100_000 || iteraciones > 1_000_000) return false;
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] hash = Base64.getDecoder().decode(partes[3]);
            if (sal.length != 16 || hash.length != 32) return false;
            return MessageDigest.isEqual(hash, derivar(clave, sal, iteraciones));
        } catch (IllegalArgumentException e) { return false; }
    }
    private static byte[] derivar(String clave, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), sal, iteraciones, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
}
