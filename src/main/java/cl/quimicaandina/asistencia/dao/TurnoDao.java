package cl.quimicaandina.asistencia.dao;

import cl.quimicaandina.asistencia.modelo.Turno;
import java.util.List;

/** Acceso a datos de turnos laborales (parametrizacion de horarios). */
public interface TurnoDao {

    List<Turno> listarTodos();
}
