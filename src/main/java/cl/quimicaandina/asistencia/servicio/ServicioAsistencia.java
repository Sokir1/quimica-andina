package cl.quimicaandina.asistencia.servicio;

import cl.quimicaandina.asistencia.dao.RegistroDao;
import cl.quimicaandina.asistencia.dao.RegistroDaoJdbc;
import cl.quimicaandina.asistencia.modelo.RegistroAsistencia;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Reglas de marcacion de asistencia del caso Quimica Andina:
 * V1 no doble entrada; V2 salida sin entrada; V3 no doble salida;
 * V4 sin marcaciones en fin de semana; V5 atraso si entra despues de 09:30:00;
 * V6 salida anticipada si sale antes de 17:30:00; V7 confirmacion clara.
 */
public class ServicioAsistencia {

    /** Regla fija del caso: limite para considerar una entrada atrasada. */
    public static final LocalTime HORA_LIMITE_ENTRADA = LocalTime.of(9, 30, 0);
    /** Regla fija del caso: limite para considerar una salida anticipada. */
    public static final LocalTime HORA_LIMITE_SALIDA = LocalTime.of(17, 30, 0);

    private final RegistroDao registroDao;
    public ServicioAsistencia() { this(new RegistroDaoJdbc()); }
    public ServicioAsistencia(RegistroDao registroDao) {
        this.registroDao = java.util.Objects.requireNonNull(registroDao);
    }

    /** Marcacion de entrada con fecha y hora actuales del sistema. */
    public String marcarEntrada(int idEmpleado) {
        return marcarEntrada(idEmpleado, LocalDateTime.now());
    }

    /** Marcacion de entrada en un momento dado (usado por PruebaHumo para ser repetible). */
    public String marcarEntrada(int idEmpleado, LocalDateTime momento) {
        LocalDate fecha = momento.toLocalDate();
        LocalTime hora = momento.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        validarDiaHabil(fecha, "la entrada"); // V4

        if (registroDao.buscarPorEmpleadoYFecha(idEmpleado, fecha) != null) {
            throw new ExcepcionNegocio("V1: No se puede marcar la entrada porque ya existe un registro "
                    + "de asistencia hoy (" + fecha + ").");
        }

        String estadoEntrada = hora.isAfter(HORA_LIMITE_ENTRADA) ? "ATRASADA" : "PUNTUAL"; // V5
        registroDao.insertarEntrada(idEmpleado, fecha, hora, estadoEntrada);

        return "Entrada registrada el " + fecha + " a las " + hora + ". Estado de entrada: " + estadoEntrada + ".";
    }

    /** Marcacion de salida con fecha y hora actuales del sistema. */
    public String marcarSalida(int idEmpleado) {
        return marcarSalida(idEmpleado, LocalDateTime.now());
    }

    /** Marcacion de salida en un momento dado (usado por PruebaHumo para ser repetible). */
    public String marcarSalida(int idEmpleado, LocalDateTime momento) {
        LocalDate fecha = momento.toLocalDate();
        LocalTime hora = momento.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        validarDiaHabil(fecha, "la salida"); // V4

        RegistroAsistencia registro = registroDao.buscarPorEmpleadoYFecha(idEmpleado, fecha);
        if (registro == null) {
            throw new ExcepcionNegocio("V2: No se puede marcar la salida porque no existe una entrada "
                    + "registrada hoy (" + fecha + ").");
        }
        if (registro.getHoraSalida() != null) {
            throw new ExcepcionNegocio("V3: No se puede marcar la salida porque ya fue registrada hoy ("
                    + fecha + ") a las " + registro.getHoraSalida() + ".");
        }

        if (!hora.isAfter(registro.getHoraEntrada())) {
            throw new ExcepcionNegocio("La salida debe ser posterior a la entrada registrada.");
        }
        String estadoSalida = hora.isBefore(HORA_LIMITE_SALIDA) ? "ANTICIPADA" : "NORMAL"; // V6
        registroDao.registrarSalida(idEmpleado, fecha, hora, estadoSalida);

        return "Salida registrada el " + fecha + " a las " + hora + ". Estado de salida: " + estadoSalida + ".";
    }

    private void validarDiaHabil(LocalDate fecha, String tipoMarca) {
        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            throw new ExcepcionNegocio("V4: No se puede marcar " + tipoMarca + " un fin de semana. "
                    + "La jornada es de lunes a viernes.");
        }
    }
}
