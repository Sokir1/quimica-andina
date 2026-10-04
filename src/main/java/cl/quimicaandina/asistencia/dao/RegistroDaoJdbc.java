package cl.quimicaandina.asistencia.dao;

import cl.quimicaandina.asistencia.modelo.RegistroAsistencia;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Implementacion JDBC de RegistroDao. */
public class RegistroDaoJdbc implements RegistroDao {

    private static final String SQL_BUSCAR =
            "SELECT id_registro, id_empleado, fecha, hora_entrada, hora_salida, estado_entrada, estado_salida "
            + "FROM registro_asistencia WHERE id_empleado = ? AND fecha = ?";

    @Override
    public RegistroAsistencia buscarPorEmpleadoYFecha(int idEmpleado, LocalDate fecha) {
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR)) {
            sentencia.setInt(1, idEmpleado);
            sentencia.setDate(2, java.sql.Date.valueOf(fecha));
            try (ResultSet rs = sentencia.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                RegistroAsistencia registro = new RegistroAsistencia();
                registro.setIdRegistro(rs.getInt("id_registro"));
                registro.setIdEmpleado(rs.getInt("id_empleado"));
                registro.setFecha(rs.getDate("fecha").toLocalDate());
                registro.setHoraEntrada(rs.getObject("hora_entrada", LocalTime.class));
                registro.setHoraSalida(rs.getObject("hora_salida", LocalTime.class));
                registro.setEstadoEntrada(rs.getString("estado_entrada"));
                registro.setEstadoSalida(rs.getString("estado_salida"));
                return registro;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar registro de asistencia: " + e.getMessage(), e);
        }
    }

    @Override
    public void insertarEntrada(int idEmpleado, LocalDate fecha, LocalTime horaEntrada, String estadoEntrada) {
        String sql = "INSERT INTO registro_asistencia (id_empleado, fecha, hora_entrada, estado_entrada) "
                + "VALUES (?,?,?,?)";
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            sentencia.setInt(1, idEmpleado);
            sentencia.setDate(2, java.sql.Date.valueOf(fecha));
            sentencia.setTime(3, java.sql.Time.valueOf(horaEntrada));
            sentencia.setString(4, estadoEntrada);
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar la entrada: " + e.getMessage(), e);
        }
    }

    @Override
    public void registrarSalida(int idEmpleado, LocalDate fecha, LocalTime horaSalida, String estadoSalida) {
        String sql = "UPDATE registro_asistencia SET hora_salida = ?, estado_salida = ? "
                + "WHERE id_empleado = ? AND fecha = ? AND hora_salida IS NULL AND hora_entrada < ?";
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setTime(1, java.sql.Time.valueOf(horaSalida));
            sentencia.setString(2, estadoSalida);
            sentencia.setInt(3, idEmpleado);
            sentencia.setDate(4, java.sql.Date.valueOf(fecha));
            sentencia.setTime(5, java.sql.Time.valueOf(horaSalida));
            if (sentencia.executeUpdate() != 1) {
                throw new cl.quimicaandina.asistencia.servicio.ExcepcionNegocio(
                    "La salida no se registró: el registro cambió o la hora no es válida. Actualice e intente nuevamente.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar la salida: " + e.getMessage(), e);
        }
    }

    @Override
    public ResultadoTabla consultaTabla(String sql) {
        try (Connection conexion = Conexion.getConnection();
             Statement sentencia = conexion.createStatement();
             ResultSet rs = sentencia.executeQuery(sql)) {

            int cantidad = rs.getMetaData().getColumnCount();
            String[] columnas = new String[cantidad];
            for (int i = 0; i < cantidad; i++) {
                columnas[i] = rs.getMetaData().getColumnLabel(i + 1);
            }
            List<Object[]> filas = new ArrayList<>();
            while (rs.next()) {
                Object[] fila = new Object[cantidad];
                for (int i = 0; i < cantidad; i++) {
                    fila[i] = rs.getObject(i + 1);
                }
                filas.add(fila);
            }
            return new ResultadoTabla(columnas, filas);
        } catch (SQLException e) {
            throw new RuntimeException("Error al ejecutar consulta de reporte: " + e.getMessage(), e);
        }
    }

    @Override
    public int eliminarPorEmpleadoYFecha(int idEmpleado, LocalDate fecha) {
        String sql = "DELETE FROM registro_asistencia WHERE id_empleado = ? AND fecha = ?";
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setInt(1, idEmpleado);
            sentencia.setDate(2, java.sql.Date.valueOf(fecha));
            return sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al limpiar registro de prueba: " + e.getMessage(), e);
        }
    }
}
