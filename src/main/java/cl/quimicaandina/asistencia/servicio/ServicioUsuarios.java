package cl.quimicaandina.asistencia.servicio;

import cl.quimicaandina.asistencia.dao.AreaDao;
import cl.quimicaandina.asistencia.dao.AreaDaoJdbc;
import cl.quimicaandina.asistencia.dao.EmpleadoDao;
import cl.quimicaandina.asistencia.dao.EmpleadoDaoJdbc;
import cl.quimicaandina.asistencia.dao.TurnoDao;
import cl.quimicaandina.asistencia.dao.TurnoDaoJdbc;
import cl.quimicaandina.asistencia.modelo.Area;
import cl.quimicaandina.asistencia.modelo.Empleado;
import cl.quimicaandina.asistencia.modelo.Turno;
import cl.quimicaandina.asistencia.util.PasswordUtil;
import java.util.List;
import java.util.regex.Pattern;

/** Reglas de gestion de usuarios (GU-01 crear, GU-02 modificar, GU-03 eliminar logico) y login. */
public class ServicioUsuarios {

    private static final Pattern PATRON_CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int LARGO_MINIMO_CLAVE = 6;

    private final EmpleadoDao empleadoDao = new EmpleadoDaoJdbc();
    private final AreaDao areaDao = new AreaDaoJdbc();
    private final TurnoDao turnoDao = new TurnoDaoJdbc();

    /** RF-1: autenticacion con mensajes de error especificos por caso. */
    public Empleado autenticar(String correo, String clave) {
        Empleado empleado = empleadoDao.buscarPorCorreo(correo == null ? "" : correo.trim());
        if (empleado == null) {
            throw new ExcepcionNegocio("El correo ingresado no esta registrado en el sistema.");
        }
        if (!empleado.isActivo()) {
            throw new ExcepcionNegocio("El usuario esta desactivado. Contacte al administrador.");
        }
        if (!PasswordUtil.verificar(clave, empleado.getClave())) {
            throw new ExcepcionNegocio("La clave es incorrecta.");
        }
        if (empleado.getClave().matches("[a-fA-F0-9]{64}")) {
            String nuevoHash = PasswordUtil.hash(clave);
            empleadoDao.actualizarClave(empleado.getIdEmpleado(), nuevoHash);
            empleado.setClave(nuevoHash);
        }
        return empleado;
    }

    /** GU-01: crea un trabajador validando formato, unicidad y largo de clave. */
    public void crearEmpleado(Empleado empleado, String clavePlano) {
        validarDatos(empleado, clavePlano, true);
        empleado.setClave(PasswordUtil.hash(clavePlano));
        empleado.setActivo(true);
        empleadoDao.crear(empleado);
    }

    /** GU-02: modifica un trabajador existente; clave opcional (vacia = conservar). */
    public void modificarEmpleado(Empleado empleado, String claveNuevaOpcional) {
        if (empleado.getIdEmpleado() <= 0) {
            throw new ExcepcionNegocio("Debe seleccionar un trabajador de la tabla para modificar.");
        }
        validarDatos(empleado, claveNuevaOpcional, false);
        empleadoDao.modificar(empleado);
        if (claveNuevaOpcional != null && !claveNuevaOpcional.isBlank()) {
            empleadoDao.actualizarClave(empleado.getIdEmpleado(), PasswordUtil.hash(claveNuevaOpcional));
        }
    }

    /** GU-03: borrado logico (activo=0); jamas elimina filas fisicamente. */
    public void eliminarEmpleado(int idEmpleado) {
        empleadoDao.cambiarActivo(idEmpleado, false);
    }

    private void validarDatos(Empleado empleado, String clavePlano, boolean esCreacion) {
        if (textoVacio(empleado.getRun())) {
            throw new ExcepcionNegocio("El RUN es obligatorio.");
        }
        if (textoVacio(empleado.getNombres())) {
            throw new ExcepcionNegocio("Los nombres son obligatorios.");
        }
        if (textoVacio(empleado.getApellidos())) {
            throw new ExcepcionNegocio("Los apellidos son obligatorios.");
        }
        String correo = textoPlano(empleado.getCorreo());
        empleado.setCorreo(correo);
        if (!PATRON_CORREO.matcher(correo).matches()) {
            throw new ExcepcionNegocio("El formato del correo no es valido (ejemplo: nombre@empresa.cl).");
        }
        if (empleadoDao.existeCorreo(correo, empleado.getIdEmpleado())) {
            throw new ExcepcionNegocio("El correo ya esta registrado por otro trabajador.");
        }
        if (empleadoDao.existeRun(textoPlano(empleado.getRun()), empleado.getIdEmpleado())) {
            throw new ExcepcionNegocio("El RUN ya esta registrado por otro trabajador.");
        }
        if (esCreacion) {
            if (clavePlano == null || clavePlano.length() < LARGO_MINIMO_CLAVE) {
                throw new ExcepcionNegocio("La clave debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres.");
            }
        } else if (clavePlano != null && !clavePlano.isBlank()
                && clavePlano.length() < LARGO_MINIMO_CLAVE) {
            throw new ExcepcionNegocio("La nueva clave debe tener al menos "
                    + LARGO_MINIMO_CLAVE + " caracteres.");
        }
        if (empleado.getIdArea() <= 0) {
            throw new ExcepcionNegocio("Debe seleccionar un area.");
        }
        if (empleado.getIdTurno() <= 0) {
            throw new ExcepcionNegocio("Debe seleccionar un turno.");
        }
    }

    public List<Empleado> listarEmpleados() {
        return empleadoDao.listarTodos();
    }

    public List<Area> listarAreas() {
        return areaDao.listarTodos();
    }

    public List<Turno> listarTurnos() {
        return turnoDao.listarTodos();
    }

    private boolean textoVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private String textoPlano(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
