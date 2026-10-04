package cl.quimicaandina.asistencia.dao;

import cl.quimicaandina.asistencia.modelo.Turno;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Implementacion JDBC de TurnoDao: evidencia la parametrizacion de horarios en BD. */
public class TurnoDaoJdbc implements TurnoDao {

    private static final String SQL_LISTAR =
            "SELECT id_turno, nombre_turno, hora_entrada, hora_salida FROM turno ORDER BY id_turno";

    @Override
    public List<Turno> listarTodos() {
        List<Turno> turnos = new ArrayList<>();
        try (Connection conexion = Conexion.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet rs = sentencia.executeQuery()) {

            while (rs.next()) {
                Turno turno = new Turno();
                turno.setIdTurno(rs.getInt("id_turno"));
                turno.setNombreTurno(rs.getString("nombre_turno"));
                turno.setHoraEntrada(rs.getObject("hora_entrada", LocalTime.class));
                turno.setHoraSalida(rs.getObject("hora_salida", LocalTime.class));
                turnos.add(turno);
            }
            return turnos;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar turnos: " + e.getMessage(), e);
        }
    }
}
