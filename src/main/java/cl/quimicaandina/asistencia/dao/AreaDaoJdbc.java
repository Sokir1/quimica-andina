package cl.quimicaandina.asistencia.dao;

import cl.quimicaandina.asistencia.modelo.Area;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Implementacion JDBC de AreaDao. */
public class AreaDaoJdbc implements AreaDao {

    @Override
    public List<Area> listarTodos() {
        String sql = "SELECT id_area, nombre_area FROM area ORDER BY nombre_area";
        List<Area> areas = new ArrayList<>();
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet rs = sentencia.executeQuery()) {

            while (rs.next()) {
                areas.add(new Area(rs.getInt("id_area"), rs.getString("nombre_area")));
            }
            return areas;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar areas: " + e.getMessage(), e);
        }
    }
}
