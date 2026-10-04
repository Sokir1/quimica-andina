package cl.quimicaandina.asistencia.util;

import cl.quimicaandina.asistencia.dao.Conexion;
import cl.quimicaandina.asistencia.dao.EmpleadoDao;
import cl.quimicaandina.asistencia.dao.EmpleadoDaoJdbc;
import cl.quimicaandina.asistencia.dao.RegistroDao;
import cl.quimicaandina.asistencia.dao.RegistroDaoJdbc;
import cl.quimicaandina.asistencia.modelo.Empleado;
import cl.quimicaandina.asistencia.modelo.RegistroAsistencia;
import cl.quimicaandina.asistencia.servicio.ExcepcionNegocio;
import cl.quimicaandina.asistencia.servicio.ServicioAsistencia;
import cl.quimicaandina.asistencia.servicio.ServicioUsuarios;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Q2 - Prueba de humo sin interfaz grafica. Ejecuta 8 verificaciones contra la
 * base de datos real y deja la base como estaba (limpieza al final).
 *
 * Nota sobre el dia de prueba: la regla V4 prohibe marcar sabado y domingo,
 * por lo que si hoy es fin de semana (o el empleado ya tiene registro) se usa
 * automaticamente el proximo dia habil sin registro para el empleado de prueba.
 */
public class PruebaHumo {

    private static final int ID_EMPLEADO_PRUEBA = 2;
    private static final LocalTime HORA_ENTRADA_PRUEBA = LocalTime.of(9, 5, 0);
    private static final LocalTime HORA_SALIDA_PRUEBA = LocalTime.of(17, 45, 0);

    private static int pruebasPasadas = 0;
    private static int pruebasEjecutadas = 0;

    public static void main(String[] args) {
        if (!"true".equals(System.getenv("ALLOW_DEMO_DB_TEST"))) {
            throw new IllegalStateException("Esta prueba modifica datos. Use solo la base demo y ALLOW_DEMO_DB_TEST=true.");
        }
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out),
                    true, "UTF-8"));
        } catch (Exception ignorada) {
            // Se conserva la salida estandar por defecto
        }

        EmpleadoDao empleadoDao = new EmpleadoDaoJdbc();
        RegistroDao registroDao = new RegistroDaoJdbc();
        ServicioUsuarios servicioUsuarios = new ServicioUsuarios();
        ServicioAsistencia servicioAsistencia = new ServicioAsistencia();

        System.out.println("=== PRUEBA DE HUMO — Sistema de Control de Asistencia Quimica Andina ===");
        System.out.println("Empleado de prueba: id " + ID_EMPLEADO_PRUEBA);
        System.out.println();

        // (1) Conexion a la BD -------------------------------------------------
        try (Connection conexion = Conexion.getConnection()) {
            registrar(conexion != null && !conexion.isClosed(),
                    "Conexion establecida con " + Conexion.URL + " (usuario " + Conexion.USUARIO + ")",
                    "No se pudo establecer la conexion con la base de datos");
        } catch (Exception e) {
            registrar(false, "", "Excepcion al conectar: " + e.getMessage());
        }

        // (2) Login del administrador correcto via DAO -------------------------
        try {
            Empleado admin = servicioUsuarios.autenticar("admin@example.com", "demo12345");
            registrar(admin != null && "ADMINISTRADOR".equals(admin.getRol()),
                    "Login correcto via DAO: " + admin.getCorreo() + " (" + admin.getNombreCompleto()
                            + ", rol " + admin.getRol() + ")",
                    "El DAO no autentico al administrador o su rol no es ADMINISTRADOR");
        } catch (Exception e) {
            registrar(false, "", "Excepcion en login admin: " + e.getMessage());
        }

        // (3) Login con clave incorrecta rechazado -----------------------------
        try {
            try {
                servicioUsuarios.autenticar("admin@example.com", "clave_equivocada_99");
                registrar(false, "", "FALLO GRAVE: se permitio el acceso con clave incorrecta");
            } catch (ExcepcionNegocio ex) {
                boolean mensajeClave = ex.getMessage() != null
                        && ex.getMessage().toLowerCase().contains("clave");
                registrar(mensajeClave,
                        "Acceso rechazado correctamente -> \"" + ex.getMessage() + "\"",
                        "Se rechazo el acceso pero sin mensaje especifico de clave: " + ex.getMessage());
            }
        } catch (Exception e) {
            registrar(false, "", "Excepcion en rechazo de clave: " + e.getMessage());
        }

        // Preparacion: dia habil de prueba y limpieza previa --------------------
        LocalDate fechaPrueba = null;
        try {
            fechaPrueba = calcularDiaDePrueba(registroDao);
            String diaSemana = traducirDia(fechaPrueba.getDayOfWeek());
            System.out.println("Día de prueba seleccionado: " + fechaPrueba + " (" + diaSemana + ").");
            System.out.println("Hoy es " + LocalDate.now()
                    + "; la regla V4 prohibe marcar sábados y domingos, por lo que la prueba "
                    + "usa el próximo día hábil sin registro del empleado.");
            int residuos = registroDao.eliminarPorEmpleadoYFecha(ID_EMPLEADO_PRUEBA, fechaPrueba);
            if (residuos > 0) {
                System.out.println("Nota: se elimino un registro residual previo del empleado "
                        + ID_EMPLEADO_PRUEBA + " para esa fecha.");
            }
            System.out.println();
        } catch (Exception e) {
            fechaPrueba = null;
            System.out.println("No se pudo preparar el día de prueba: " + e.getMessage());
        }

        // (4) Marcar entrada funciona y el estado cuadra con V5 -----------------
        try {
            if (fechaPrueba == null) {
                registrar(false, "", "Sin dia de prueba disponible");
            } else {
                String mensaje = servicioAsistencia.marcarEntrada(ID_EMPLEADO_PRUEBA,
                        LocalDateTime.of(fechaPrueba, HORA_ENTRADA_PRUEBA));
                RegistroAsistencia registro =
                        registroDao.buscarPorEmpleadoYFecha(ID_EMPLEADO_PRUEBA, fechaPrueba);
                boolean ok = registro != null && registro.getHoraSalida() == null
                        && "PUNTUAL".equals(registro.getEstadoEntrada())
                        && !registro.getHoraEntrada().isAfter(ServicioAsistencia.HORA_LIMITE_ENTRADA);
                registrar(ok,
                        mensaje + " | V5 verificada: 09:05 no es posterior a "
                                + ServicioAsistencia.HORA_LIMITE_ENTRADA + " -> PUNTUAL",
                        "Estado esperado PUNTUAL; obtenido: "
                                + (registro == null ? "sin registro" : registro.getEstadoEntrada()));
            }
        } catch (Exception e) {
            registrar(false, "", "Excepcion al marcar entrada: " + e.getMessage());
        }

        // (5) Segunda entrada hoy RECHAZADA por V1 ------------------------------
        try {
            try {
                servicioAsistencia.marcarEntrada(ID_EMPLEADO_PRUEBA,
                        LocalDateTime.of(fechaPrueba, java.time.LocalTime.of(10, 0, 0)));
                registrar(false, "", "FALLO GRAVE: se acepto una segunda entrada el mismo dia");
            } catch (ExcepcionNegocio ex) {
                registrar(ex.getMessage() != null && ex.getMessage().startsWith("V1"),
                        "Segunda entrada rechazada por V1 -> \"" + ex.getMessage() + "\"",
                        "Mensaje inesperado al duplicar entrada: " + ex.getMessage());
            }
        } catch (Exception e) {
            registrar(false, "", "Excepcion en doble entrada: " + e.getMessage());
        }

        // (6) Marcar salida funciona y el estado cuadra con V6 -------------------
        try {
            String mensaje = servicioAsistencia.marcarSalida(ID_EMPLEADO_PRUEBA,
                    LocalDateTime.of(fechaPrueba, HORA_SALIDA_PRUEBA));
            RegistroAsistencia registro =
                    registroDao.buscarPorEmpleadoYFecha(ID_EMPLEADO_PRUEBA, fechaPrueba);
            boolean ok = registro != null && registro.getHoraSalida() != null
                    && "NORMAL".equals(registro.getEstadoSalida())
                    && !registro.getHoraSalida().isBefore(ServicioAsistencia.HORA_LIMITE_SALIDA);
            registrar(ok,
                    mensaje + " | V6 verificada: 17:45 no es anterior a "
                            + ServicioAsistencia.HORA_LIMITE_SALIDA + " -> NORMAL",
                    "Estado esperado NORMAL; obtenido: "
                            + (registro == null ? "sin registro" : registro.getEstadoSalida()));
        } catch (Exception e) {
            registrar(false, "", "Excepcion al marcar salida: " + e.getMessage());
        }

        // (7) Segunda salida hoy RECHAZADA por V3 --------------------------------
        try {
            try {
                servicioAsistencia.marcarSalida(ID_EMPLEADO_PRUEBA,
                        LocalDateTime.of(fechaPrueba, java.time.LocalTime.of(18, 30, 0)));
                registrar(false, "", "FALLO GRAVE: se acepto una segunda salida el mismo dia");
            } catch (ExcepcionNegocio ex) {
                registrar(ex.getMessage() != null && ex.getMessage().startsWith("V3"),
                        "Segunda salida rechazada por V3 -> \"" + ex.getMessage() + "\"",
                        "Mensaje inesperado al duplicar salida: " + ex.getMessage());
            }
        } catch (Exception e) {
            registrar(false, "", "Excepcion en doble salida: " + e.getMessage());
        }

        // (8) Limpieza: DELETE del registro de prueba -----------------------------
        try {
            int eliminados = registroDao.eliminarPorEmpleadoYFecha(ID_EMPLEADO_PRUEBA, fechaPrueba);
            RegistroAsistencia restante =
                    registroDao.buscarPorEmpleadoYFecha(ID_EMPLEADO_PRUEBA, fechaPrueba);
            registrar(eliminados >= 1 && restante == null,
                    "Limpieza completada: se elimino el registro de prueba del empleado "
                            + ID_EMPLEADO_PRUEBA + " del " + fechaPrueba + ". La base quedo como estaba.",
                    "Limpieza incompleta (eliminados=" + eliminados
                            + ", restante=" + (restante != null) + ")");
        } catch (Exception e) {
            registrar(false, "", "Excepcion en limpieza: " + e.getMessage());
        }

        System.out.println();
        System.out.println("RESUMEN FINAL: " + pruebasPasadas + "/8 pruebas pasadas"
                + (pruebasPasadas == 8 ? " — TODO OK" : " — REVISE LOS FALLOS ARRIBA"));
        System.exit(pruebasPasadas == 8 ? 0 : 1);
    }

    /** Registra el resultado de una prueba imprimiendo [OK] o [FALLO] con detalle. */
    private static void registrar(boolean exitosa, String detalleOk, String detalleFallo) {
        pruebasEjecutadas++;
        if (exitosa) {
            pruebasPasadas++;
            System.out.println("(" + pruebasEjecutadas + ") [OK] " + detalleOk);
        } else {
            System.out.println("(" + pruebasEjecutadas + ") [FALLO] " + detalleFallo);
        }
    }

    /**
     * Devuelve el proximo dia habil a partir de hoy que aun no tenga registro
     * del empleado de prueba, para que la prueba sea repetible sin tocar datos semilla.
     */
    private static LocalDate calcularDiaDePrueba(RegistroDao registroDao) {
        LocalDate dia = LocalDate.now();
        if (dia.getDayOfWeek() == DayOfWeek.SATURDAY) {
            dia = dia.plusDays(2);
        } else if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dia = dia.plusDays(1);
        }
        int intentos = 0;
        while (intentos++ < 30 && registroDao.buscarPorEmpleadoYFecha(ID_EMPLEADO_PRUEBA, dia) != null) {
            dia = dia.plusDays(1);
            if (dia.getDayOfWeek() == DayOfWeek.SATURDAY) {
                dia = dia.plusDays(2);
            } else if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
                dia = dia.plusDays(1);
            }
        }
        return dia;
    }

    private static String traducirDia(DayOfWeek dia) {
        switch (dia) {
            case MONDAY: return "lunes";
            case TUESDAY: return "martes";
            case WEDNESDAY: return "miércoles";
            case THURSDAY: return "jueves";
            case FRIDAY: return "viernes";
            case SATURDAY: return "sábado";
            default: return "domingo";
        }
    }
}
