package cl.quimicaandina.asistencia.servicio;

/** Excepcion con mensaje legible para el usuario cuando una regla de negocio no se cumple. */
public class ExcepcionNegocio extends RuntimeException {

    public ExcepcionNegocio(String mensaje) {
        super(mensaje);
    }
}
