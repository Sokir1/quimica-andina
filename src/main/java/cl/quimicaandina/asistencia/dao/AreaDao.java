package cl.quimicaandina.asistencia.dao;

import cl.quimicaandina.asistencia.modelo.Area;
import java.util.List;

/** Acceso a datos de areas de la empresa. */
public interface AreaDao {

    List<Area> listarTodos();
}
