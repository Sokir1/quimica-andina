package cl.quimicaandina.asistencia.dao;

import cl.quimicaandina.asistencia.modelo.Empleado;
import java.util.List;

/** Acceso a datos de empleados. */
public interface EmpleadoDao {

    /** Busca por correo; devuelve null si no existe. */
    Empleado buscarPorCorreo(String correo);

    /** Valida correo y clave (hash SHA-256). Devuelve el empleado o null si no coincide. */
    Empleado autenticar(String correo, String claveHash);

    boolean existeCorreo(String correo, int idExcluir);

    boolean existeRun(String run, int idExcluir);

    void crear(Empleado empleado);

    void modificar(Empleado empleado);

    void actualizarClave(int idEmpleado, String claveHash);

    /** Borrado logico GU-03: cambia la marca activo sin eliminar filas. */
    void cambiarActivo(int idEmpleado, boolean activo);

    List<Empleado> listarTodos();
}
