package cl.quimicaandina.asistencia.dao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Configuración externa: propiedades JVM o variables de entorno. */
public final class Conexion {
    public static final String URL = configurar("db.url", "DB_URL", "jdbc:mysql://localhost:3306/sistema_asistencia");
    public static final String USUARIO = configurar("db.user", "DB_USER", "asistencia");
    private Conexion() {}
    private static String configurar(String propiedad, String variable, String defecto) {
        String valor = System.getProperty(propiedad);
        if (valor == null) valor = System.getenv(variable);
        return valor == null ? defecto : valor;
    }
    public static Connection getConnection() throws SQLException {
        String clave = configurar("db.password", "DB_PASSWORD", null);
        if (clave == null) throw new SQLException("Configure DB_PASSWORD antes de iniciar el sistema.");
        DriverManager.setLoginTimeout(10);
        return DriverManager.getConnection(URL, USUARIO, clave);
    }
}
