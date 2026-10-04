import cl.quimicaandina.asistencia.dao.*;
import cl.quimicaandina.asistencia.modelo.*;
import cl.quimicaandina.asistencia.servicio.*;
import cl.quimicaandina.asistencia.util.*;
import java.time.*;

/** Regresión independiente de MySQL: límites horarios, estados y claves. */
public class Pruebas {
    static int total;
    static void comprobar(boolean condicion) {
        if (!condicion) throw new AssertionError("Falló prueba " + (total + 1));
        total++;
    }
    static void rechazar(Runnable accion) {
        try { accion.run(); } catch (ExcepcionNegocio e) { total++; return; }
        throw new AssertionError("Se esperaba una excepción de negocio");
    }
    static class Memoria implements RegistroDao {
        RegistroAsistencia registro;
        public RegistroAsistencia buscarPorEmpleadoYFecha(int id, LocalDate fecha) { return registro; }
        public void insertarEntrada(int id, LocalDate fecha, LocalTime hora, String estado) {
            registro = new RegistroAsistencia(); registro.setHoraEntrada(hora); registro.setEstadoEntrada(estado);
        }
        public void registrarSalida(int id, LocalDate fecha, LocalTime hora, String estado) {
            registro.setHoraSalida(hora); registro.setEstadoSalida(estado);
        }
        public ResultadoTabla consultaTabla(String sql) { throw new UnsupportedOperationException(); }
        public int eliminarPorEmpleadoYFecha(int id, LocalDate fecha) { registro=null; return 1; }
    }
    public static void main(String[] args) {
        Memoria dao = new Memoria(); ServicioAsistencia servicio = new ServicioAsistencia(dao);
        LocalDate lunes = LocalDate.of(2026, 10, 5);
        rechazar(() -> servicio.marcarEntrada(2, LocalDateTime.of(2026,10,4,9,0)));
        rechazar(() -> servicio.marcarSalida(2, lunes.atTime(17,30)));
        servicio.marcarEntrada(2, lunes.atTime(9,30));
        comprobar(dao.registro.getEstadoEntrada().equals("PUNTUAL"));
        rechazar(() -> servicio.marcarEntrada(2, lunes.atTime(9,31)));
        rechazar(() -> servicio.marcarSalida(2, lunes.atTime(9,0)));
        rechazar(() -> servicio.marcarSalida(2, lunes.atTime(9,30)));
        comprobar(dao.registro.getHoraSalida() == null);
        servicio.marcarSalida(2, lunes.atTime(17,30));
        comprobar(dao.registro.getEstadoSalida().equals("NORMAL"));
        rechazar(() -> servicio.marcarSalida(2, lunes.atTime(18,0)));
        dao.registro=null; servicio.marcarEntrada(2, lunes.atTime(9,30,1));
        comprobar(dao.registro.getEstadoEntrada().equals("ATRASADA"));
        servicio.marcarSalida(2, lunes.atTime(17,29,59));
        comprobar(dao.registro.getEstadoSalida().equals("ANTICIPADA"));
        String hash = PasswordUtil.hash("clave-de-prueba");
        comprobar(PasswordUtil.verificar("clave-de-prueba", hash));
        comprobar(!PasswordUtil.verificar("incorrecta", hash));
        comprobar(!hash.equals(PasswordUtil.hash("clave-de-prueba")));
        comprobar(PasswordUtil.verificar("demo", HashUtil.sha256("demo")));
        comprobar(!PasswordUtil.verificar(null, hash));
        comprobar(!PasswordUtil.verificar("demo", "pbkdf2$abc$bad$bad"));
        System.out.println(total + " verificaciones correctas; sin acceso a base de datos.");
    }
}
