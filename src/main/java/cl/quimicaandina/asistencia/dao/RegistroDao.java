package cl.quimicaandina.asistencia.dao;

import cl.quimicaandina.asistencia.modelo.RegistroAsistencia;
import java.time.LocalDate;
import java.time.LocalTime;

/** Acceso a datos de registros de asistencia y reportes. */
public interface RegistroDao {

    /** Registro del empleado para la fecha indicada, o null si no existe. */
    RegistroAsistencia buscarPorEmpleadoYFecha(int idEmpleado, LocalDate fecha);

    /** Inserta la marcacion de entrada (hora_salida queda NULL). */
    void insertarEntrada(int idEmpleado, LocalDate fecha, LocalTime horaEntrada, String estadoEntrada);

    /** Actualiza hora y estado de salida del registro del dia. */
    void registrarSalida(int idEmpleado, LocalDate fecha, LocalTime horaSalida, String estadoSalida);

    /** Ejecuta una consulta SELECT de reportes y devuelve columnas + filas. */
    ResultadoTabla consultaTabla(String sql);

    /** Eliminacion fisica restringida, usada solo por PruebaHumo para limpiar sus datos. */
    int eliminarPorEmpleadoYFecha(int idEmpleado, LocalDate fecha);
}
