package cl.quimicaandina.asistencia.dao;

import java.util.List;

/** Resultado generico de una consulta de reportes: nombres de columnas y filas. */
public final class ResultadoTabla {

    private final String[] columnas;
    private final List<Object[]> filas;

    public ResultadoTabla(String[] columnas, List<Object[]> filas) {
        this.columnas = columnas;
        this.filas = filas;
    }

    public String[] getColumnas() {
        return columnas;
    }

    public List<Object[]> getFilas() {
        return filas;
    }
}
