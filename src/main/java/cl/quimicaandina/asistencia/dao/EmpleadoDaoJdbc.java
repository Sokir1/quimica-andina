package cl.quimicaandina.asistencia.dao;

import cl.quimicaandina.asistencia.modelo.Empleado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Implementacion JDBC de EmpleadoDao. La clave viaja como hash; la verificación corresponde al servicio. */
public class EmpleadoDaoJdbc implements EmpleadoDao {

    private static final String SQL_BASE =
            "SELECT id_empleado, run, nombres, apellidos, correo, clave, telefono, cargo, "
            + "fecha_contratacion, id_turno, id_area, rol, activo FROM empleado";

    private static final String SQL_LISTAR =
            "SELECT e.id_empleado, e.run, e.nombres, e.apellidos, e.correo, e.clave, e.telefono, "
            + "e.cargo, e.fecha_contratacion, e.id_turno, e.id_area, e.rol, e.activo, "
            + "a.nombre_area AS nombre_area, t.nombre_turno AS nombre_turno "
            + "FROM empleado e "
            + "JOIN area a ON a.id_area = e.id_area "
            + "JOIN turno t ON t.id_turno = e.id_turno "
            + "ORDER BY e.apellidos, e.nombres";

    @Override
    public Empleado buscarPorCorreo(String correo) {
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BASE + " WHERE correo = ?")) {
            sentencia.setString(1, correo);
            try (ResultSet rs = sentencia.executeQuery()) {
                return rs.next() ? mapearBase(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar empleado por correo: " + e.getMessage(), e);
        }
    }

    @Override
    public Empleado autenticar(String correo, String claveHash) {
        String sql = SQL_BASE + " WHERE correo = ? AND clave = ?";
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, correo);
            sentencia.setString(2, claveHash);
            try (ResultSet rs = sentencia.executeQuery()) {
                return rs.next() ? mapearBase(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al autenticar: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existeCorreo(String correo, int idExcluir) {
        return existe("correo", correo, idExcluir);
    }

    @Override
    public boolean existeRun(String run, int idExcluir) {
        return existe("run", run, idExcluir);
    }

    private boolean existe(String columna, String valor, int idExcluir) {
        String sql = "SELECT COUNT(*) FROM empleado WHERE " + columna + " = ? AND id_empleado <> ?";
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, valor);
            sentencia.setInt(2, idExcluir);
            try (ResultSet rs = sentencia.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar unicidad de " + columna + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void crear(Empleado empleado) {
        String sql = "INSERT INTO empleado (run, nombres, apellidos, correo, clave, telefono, cargo, "
                + "fecha_contratacion, id_turno, id_area, rol, activo) VALUES (?,?,?,?,?,?,?,?,?,?,?,1)";
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignarParametros(sentencia, empleado);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al crear el trabajador: " + e.getMessage(), e);
        }
    }

    @Override
    public void modificar(Empleado empleado) {
        String sql = "UPDATE empleado SET run=?, nombres=?, apellidos=?, correo=?, telefono=?, cargo=?, "
                + "fecha_contratacion=?, id_turno=?, id_area=?, rol=? WHERE id_empleado=?";
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            asignarParametros(sentencia, empleado);
            sentencia.setInt(11, empleado.getIdEmpleado());
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al modificar el trabajador: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarClave(int idEmpleado, String claveHash) {
        String sql = "UPDATE empleado SET clave=? WHERE id_empleado=?";
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, claveHash);
            sentencia.setInt(2, idEmpleado);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar la clave: " + e.getMessage(), e);
        }
    }

    @Override
    public void cambiarActivo(int idEmpleado, boolean activo) {
        String sql = "UPDATE empleado SET activo=? WHERE id_empleado=?";
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, activo ? 1 : 0);
            sentencia.setInt(2, idEmpleado);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al cambiar el estado del trabajador: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Empleado> listarTodos() {
        List<Empleado> empleados = new ArrayList<>();
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet rs = sentencia.executeQuery()) {

            while (rs.next()) {
                Empleado empleado = mapearBase(rs);
                empleado.setNombreArea(rs.getString("nombre_area"));
                empleado.setNombreTurno(rs.getString("nombre_turno"));
                empleados.add(empleado);
            }
            return empleados;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar trabajadores: " + e.getMessage(), e);
        }
    }

    private void asignarParametros(PreparedStatement sentencia, Empleado empleado) throws SQLException {
        sentencia.setString(1, empleado.getRun());
        sentencia.setString(2, empleado.getNombres());
        sentencia.setString(3, empleado.getApellidos());
        sentencia.setString(4, empleado.getCorreo());
        sentencia.setString(5, empleado.getClave());
        sentencia.setString(6, valorVacioACero(empleado.getTelefono()));
        sentencia.setString(7, valorVacioACero(empleado.getCargo()));
        sentencia.setDate(8, java.sql.Date.valueOf(
                empleado.getFechaContratacion() != null
                        ? empleado.getFechaContratacion()
                        : LocalDate.now()));
        sentencia.setInt(9, empleado.getIdTurno());
        sentencia.setInt(10, empleado.getIdArea());
        sentencia.setString(11, empleado.getRol());
    }

    private String valorVacioACero(String texto) {
        return (texto == null || texto.isBlank()) ? "" : texto.trim();
    }

    private Empleado mapearBase(ResultSet rs) throws SQLException {
        Empleado empleado = new Empleado();
        empleado.setIdEmpleado(rs.getInt("id_empleado"));
        empleado.setRun(rs.getString("run"));
        empleado.setNombres(rs.getString("nombres"));
        empleado.setApellidos(rs.getString("apellidos"));
        empleado.setCorreo(rs.getString("correo"));
        empleado.setClave(rs.getString("clave"));
        empleado.setTelefono(rs.getString("telefono"));
        empleado.setCargo(rs.getString("cargo"));
        java.sql.Date fecha = rs.getDate("fecha_contratacion");
        empleado.setFechaContratacion(fecha != null ? fecha.toLocalDate() : null);
        empleado.setIdTurno(rs.getInt("id_turno"));
        empleado.setIdArea(rs.getInt("id_area"));
        empleado.setRol(rs.getString("rol"));
        empleado.setActivo(rs.getBoolean("activo"));
        return empleado;
    }
}
