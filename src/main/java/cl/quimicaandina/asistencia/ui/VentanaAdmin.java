package cl.quimicaandina.asistencia.ui;

import cl.quimicaandina.asistencia.dao.RegistroDao;
import cl.quimicaandina.asistencia.dao.RegistroDaoJdbc;
import cl.quimicaandina.asistencia.dao.ResultadoTabla;
import cl.quimicaandina.asistencia.modelo.Area;
import cl.quimicaandina.asistencia.modelo.Empleado;
import cl.quimicaandina.asistencia.modelo.Turno;
import cl.quimicaandina.asistencia.servicio.ExcepcionNegocio;
import cl.quimicaandina.asistencia.servicio.ServicioUsuarios;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * RF-4: panel del administrador con seis pestañas:
 * P1 usuarios, P2 atrasos, P3 salidas anticipadas, P4 inasistencias,
 * P5 resumen por trabajador y P6 turnos.
 */
public class VentanaAdmin extends JFrame {

    /** Reportes basados en las vistas ya creadas en la base de datos. */
    private static final String SQL_ATRASOS = "SELECT * FROM vw_reporte_atrasos";
    private static final String SQL_SALIDAS_ANTICIPADAS =
            "SELECT * FROM vw_reporte_salidas_anticipadas";
    private static final String SQL_INASISTENCIAS =
            "SELECT e.run AS RUN, CONCAT(e.nombres, ' ', e.apellidos) AS Empleado, "
            + "d.fecha AS Fecha_faltante "
            + "FROM empleado e "
            + "CROSS JOIN (SELECT DISTINCT fecha FROM registro_asistencia) d "
            + "LEFT JOIN registro_asistencia r "
            + "ON r.id_empleado = e.id_empleado AND r.fecha = d.fecha "
            + "WHERE e.activo = 1 AND r.id_registro IS NULL "
            + "ORDER BY Empleado, d.fecha";
    private static final String SQL_RESUMEN =
            "SELECT v.*, "
            + "ROUND(v.dias_registrados * 100 / "
            + "NULLIF((SELECT COUNT(DISTINCT fecha) FROM registro_asistencia), 0), 1) "
            + "AS porcentaje_asistencia, "
            + "COALESCE((SELECT SUM(TIMESTAMPDIFF(MINUTE, t.hora_entrada, r.hora_entrada)) "
            + "FROM registro_asistencia r "
            + "JOIN empleado e2 ON e2.id_empleado = r.id_empleado "
            + "JOIN turno t ON t.id_turno = e2.id_turno "
            + "WHERE r.id_empleado = v.id_empleado AND r.estado_entrada = 'ATRASADA'), 0) "
            + "AS minutos_atraso_acumulados "
            + "FROM vw_resumen_asistencia v "
            + "ORDER BY porcentaje_asistencia ASC, v.empleado";
    private static final String SQL_TURNOS =
            "SELECT id_turno AS ID, nombre_turno AS Turno, hora_entrada AS Hora_entrada, "
            + "hora_salida AS Hora_salida FROM turno ORDER BY id_turno";

    private final ServicioUsuarios servicioUsuarios = new ServicioUsuarios();
    private final RegistroDao registroDao = new RegistroDaoJdbc();

    private List<Empleado> empleadosCache;
    private Integer idSeleccionado;

    private final JTable tablaEmpleados = new JTable();
    private final JTextField campoRun = new JTextField(18);
    private final JTextField campoNombres = new JTextField(18);
    private final JTextField campoApellidos = new JTextField(18);
    private final JTextField campoCorreo = new JTextField(18);
    private final JTextField campoClave = new JTextField(18);
    private final JTextField campoTelefono = new JTextField(18);
    private final JTextField campoCargo = new JTextField(18);
    private final JTextField campoFechaContratacion = new JTextField(18);
    private final JComboBox<Area> comboArea = new JComboBox<>();
    private final JComboBox<Turno> comboTurno = new JComboBox<>();
    private final JComboBox<String> comboRol =
            new JComboBox<>(new String[]{"EMPLEADO", "ADMINISTRADOR"});
    private final JCheckBox chequeoActivo = new JCheckBox("Activo");
    private final JLabel etiquetaModo = new JLabel("Modo: Crear nuevo trabajador");

    public VentanaAdmin(Empleado administrador) {
        setTitle("Química Andina — Panel de Administración");
        setSize(1150, 720);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(construirCabecera(administrador), BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(Estilo.fuente(Font.PLAIN, 13));
        pestanas.setBackground(Estilo.FONDO);
        pestanas.addTab("Gestión de Usuarios", construirPanelUsuarios());
        pestanas.addTab("Reporte de Atrasos",
                construirPanelReporte(SQL_ATRASOS, "Total de atrasos"));
        pestanas.addTab("Salidas Anticipadas",
                construirPanelReporte(SQL_SALIDAS_ANTICIPADAS, "Total de salidas anticipadas"));
        pestanas.addTab("Inasistencias",
                construirPanelReporte(SQL_INASISTENCIAS, "Total de inasistencias"));
        pestanas.addTab("Resumen por Trabajador",
                construirPanelReporte(SQL_RESUMEN, "Trabajadores listados"));
        pestanas.addTab("Turnos", construirPanelReporte(SQL_TURNOS, "Turnos configurados"));

        add(pestanas, BorderLayout.CENTER);

        recargarEmpleados();
    }

    private JPanel construirCabecera(Empleado administrador) {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(Estilo.PRIMARIO);
        cabecera.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        Box titulos = Box.createVerticalBox();
        JLabel titulo = new JLabel("Química Andina — Panel de Administración");
        titulo.setFont(Estilo.fuenteTitulo());
        titulo.setForeground(Estilo.BLANCO);
        JLabel sesion = new JLabel("Sesión: " + administrador.getCorreo()
                + "  ·  Rol: ADMINISTRADOR");
        sesion.setFont(Estilo.fuenteBase());
        sesion.setForeground(new Color(0xD6E4F0));
        titulos.add(titulo);
        titulos.add(sesion);
        cabecera.add(titulos, BorderLayout.WEST);

        JButton botonCerrarSesion = Estilo.boton("Cerrar sesión", new Color(0x16395C));
        botonCerrarSesion.addActionListener(e -> cerrarSesion());
        cabecera.add(botonCerrarSesion, BorderLayout.EAST);
        return cabecera;
    }

    // ------------------------------------------------------------------
    // P1 Gestión de Usuarios (GU-01, GU-02, GU-03)
    // ------------------------------------------------------------------

    private JPanel construirPanelUsuarios() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBackground(Estilo.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        Estilo.estilizarTabla(tablaEmpleados);
        tablaEmpleados.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaEmpleados.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int vista = tablaEmpleados.getSelectedRow();
                if (vista >= 0 && vista < empleadosCache.size()) {
                    cargarEnFormulario(empleadosCache.get(vista));
                }
            }
        });
        panel.add(new JScrollPane(tablaEmpleados), BorderLayout.CENTER);
        panel.add(construirFormularioUsuario(), BorderLayout.EAST);
        return panel;
    }

    private JPanel construirFormularioUsuario() {
        JPanel formulario = Estilo.tarjeta();
        formulario.setLayout(new GridBagLayout());
        formulario.setPreferredSize(new Dimension(400, 0));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 10, 4, 10);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int fila = 0;
        etiquetaModo.setFont(Estilo.fuente(Font.BOLD, 14));
        etiquetaModo.setForeground(Estilo.PRIMARIO);
        g.gridx = 0;
        g.gridy = fila++;
        g.gridwidth = 2;
        formulario.add(etiquetaModo, g);
        g.gridwidth = 1;

        fila = agregarCampo(formulario, g, fila, "RUN:", campoRun);
        fila = agregarCampo(formulario, g, fila, "Nombres:", campoNombres);
        fila = agregarCampo(formulario, g, fila, "Apellidos:", campoApellidos);
        fila = agregarCampo(formulario, g, fila, "Correo:", campoCorreo);
        campoClave.setToolTipText("Creación: obligatoria (mínimo 6). Modificación: vacío conserva la actual.");
        fila = agregarCampo(formulario, g, fila, "Clave:", campoClave);
        fila = agregarCampo(formulario, g, fila, "Teléfono:", campoTelefono);
        fila = agregarCampo(formulario, g, fila, "Cargo:", campoCargo);
        campoFechaContratacion.setToolTipText("Formato yyyy-MM-dd");
        fila = agregarCampo(formulario, g, fila, "Contratación:", campoFechaContratacion);
        fila = agregarCampo(formulario, g, fila, "Área:", comboArea);
        fila = agregarCampo(formulario, g, fila, "Turno:", comboTurno);
        fila = agregarCampo(formulario, g, fila, "Rol:", comboRol);

        chequeoActivo.setSelected(true);
        chequeoActivo.setFont(Estilo.fuenteBase());
        chequeoActivo.setBackground(Estilo.BLANCO);
        g.gridx = 1;
        g.gridy = fila++;
        formulario.add(chequeoActivo, g);

        JButton botonNuevo = Estilo.botonPrimario("Nuevo");
        botonNuevo.addActionListener(e -> limpiarFormulario());
        JButton botonGuardar = Estilo.botonVerde("Guardar");
        botonGuardar.addActionListener(e -> guardarSeleccion());
        JButton botonEliminar = Estilo.botonRojo("Eliminar");
        botonEliminar.addActionListener(e -> eliminarSeleccion());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        botones.setOpaque(false);
        botones.add(botonNuevo);
        botones.add(botonGuardar);
        botones.add(botonEliminar);
        g.gridx = 0;
        g.gridy = fila++;
        g.gridwidth = 2;
        formulario.add(botones, g);

        cargarCombos();
        return formulario;
    }

    private int agregarCampo(JPanel formulario, GridBagConstraints g, int fila,
            String etiqueta, javax.swing.JComponent campo) {
        g.gridy = fila;
        g.gridx = 0;
        formulario.add(new JLabel(etiqueta), g);
        g.gridx = 1;
        campo.setFont(Estilo.fuenteBase());
        formulario.add(campo, g);
        return fila + 1;
    }

    private void cargarCombos() {
        for (Area area : servicioUsuarios.listarAreas()) {
            comboArea.addItem(area);
        }
        for (Turno turno : servicioUsuarios.listarTurnos()) {
            comboTurno.addItem(turno);
        }
        comboRol.setFont(Estilo.fuenteBase());
        comboArea.setFont(Estilo.fuenteBase());
        comboTurno.setFont(Estilo.fuenteBase());
    }

    private void recargarEmpleados() {
        empleadosCache = servicioUsuarios.listarEmpleados();
        DefaultTableModel modelo = new DefaultTableModel(
                new Object[]{"ID", "RUN", "Nombre Completo", "Correo", "Cargo",
                        "Área", "Turno", "Rol", "Activo"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        for (Empleado empleado : empleadosCache) {
            modelo.addRow(new Object[]{
                    empleado.getIdEmpleado(),
                    empleado.getRun(),
                    empleado.getNombreCompleto(),
                    empleado.getCorreo(),
                    empleado.getCargo(),
                    empleado.getNombreArea(),
                    empleado.getNombreTurno(),
                    empleado.getRol(),
                    empleado.isActivo() ? "Sí" : "No"
            });
        }
        tablaEmpleados.setModel(modelo);
    }

    private void cargarEnFormulario(Empleado empleado) {
        idSeleccionado = empleado.getIdEmpleado();
        campoRun.setText(empleado.getRun());
        campoNombres.setText(empleado.getNombres());
        campoApellidos.setText(empleado.getApellidos());
        campoCorreo.setText(empleado.getCorreo());
        campoClave.setText("");
        campoTelefono.setText(empleado.getTelefono());
        campoCargo.setText(empleado.getCargo());
        campoFechaContratacion.setText(
                empleado.getFechaContratacion() == null ? "" : empleado.getFechaContratacion().toString());
        seleccionarComboPorId(comboArea, empleado.getIdArea());
        seleccionarComboPorId(comboTurno, empleado.getIdTurno());
        comboRol.setSelectedItem(empleado.getRol());
        chequeoActivo.setSelected(empleado.isActivo());
        etiquetaModo.setText("Modo: Modificando a " + empleado.getNombreCompleto());
    }

    private void seleccionarComboPorId(javax.swing.JComboBox<?> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            int idItem = -1;
            if (item instanceof Area) {
                idItem = ((Area) item).getIdArea();
            } else if (item instanceof Turno) {
                idItem = ((Turno) item).getIdTurno();
            }
            if (idItem == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void guardarSeleccion() {
        try {
            Empleado empleado = leerFormulario();
            String clave = campoClave.getText();
            boolean esCreacion = idSeleccionado == null;
            if (esCreacion) {
                servicioUsuarios.crearEmpleado(empleado, clave); // GU-01
                JOptionPane.showMessageDialog(this,
                        "Trabajador creado correctamente.",
                        "Gestión de usuarios", JOptionPane.INFORMATION_MESSAGE);
            } else {
                servicioUsuarios.modificarEmpleado(empleado, clave); // GU-02
                JOptionPane.showMessageDialog(this,
                        "Trabajador modificado correctamente.",
                        "Gestión de usuarios", JOptionPane.INFORMATION_MESSAGE);
            }
            limpiarFormulario();
            recargarEmpleados();
        } catch (ExcepcionNegocio e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al guardar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Empleado leerFormulario() {
        Empleado empleado = new Empleado();
        empleado.setIdEmpleado(idSeleccionado == null ? 0 : idSeleccionado);
        empleado.setRun(campoRun.getText().trim());
        empleado.setNombres(campoNombres.getText().trim());
        empleado.setApellidos(campoApellidos.getText().trim());
        empleado.setCorreo(campoCorreo.getText().trim());
        empleado.setTelefono(campoTelefono.getText().trim());
        empleado.setCargo(campoCargo.getText().trim());
        try {
            String fechaTexto = campoFechaContratacion.getText().trim();
            empleado.setFechaContratacion(fechaTexto.isEmpty()
                    ? LocalDate.now() : LocalDate.parse(fechaTexto));
        } catch (DateTimeParseException e) {
            throw new ExcepcionNegocio(
                    "La fecha de contratación debe tener formato yyyy-MM-dd.");
        }
        Area area = (Area) comboArea.getSelectedItem();
        Turno turno = (Turno) comboTurno.getSelectedItem();
        if (area != null) {
            empleado.setIdArea(area.getIdArea());
        }
        if (turno != null) {
            empleado.setIdTurno(turno.getIdTurno());
        }
        empleado.setRol((String) comboRol.getSelectedItem());
        empleado.setActivo(chequeoActivo.isSelected());
        return empleado;
    }

    private void eliminarSeleccion() {
        if (idSeleccionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Debe seleccionar un trabajador en la tabla para eliminar.",
                    "Gestión de usuarios", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String nombre = campoNombres.getText() + " " + campoApellidos.getText();
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Confirma la eliminación (borrado lógico) de: " + nombre.trim() + "?\n"
                + "El trabajador quedará con activo = 0 y sus registros se conservan.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            try {
                servicioUsuarios.eliminarEmpleado(idSeleccionado); // GU-03
                JOptionPane.showMessageDialog(this,
                        "Trabajador desactivado correctamente (borrado lógico).",
                        "Gestión de usuarios", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario();
                recargarEmpleados();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this,
                        "Error al eliminar: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limpiarFormulario() {
        idSeleccionado = null;
        campoRun.setText("");
        campoNombres.setText("");
        campoApellidos.setText("");
        campoCorreo.setText("");
        campoClave.setText("");
        campoTelefono.setText("");
        campoCargo.setText("");
        campoFechaContratacion.setText("");
        chequeoActivo.setSelected(true);
        comboRol.setSelectedIndex(0);
        tablaEmpleados.clearSelection();
        etiquetaModo.setText("Modo: Crear nuevo trabajador");
    }

    // ------------------------------------------------------------------
    // P2..P6 Reportes basados en vistas y consultas read-only
    // ------------------------------------------------------------------

    private JPanel construirPanelReporte(String sql, String etiquetaContador) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Estilo.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTable tabla = new JTable();
        Estilo.estilizarTabla(tabla);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JLabel contador = new JLabel(etiquetaContador + ": 0");
        contador.setFont(Estilo.fuente(Font.BOLD, 13));
        contador.setForeground(Estilo.TEXTO);

        JButton botonActualizar = Estilo.botonPrimario("Actualizar");
        botonActualizar.addActionListener(e ->
                cargarConsulta(tabla, contador, sql, etiquetaContador));

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Estilo.FONDO);
        barra.add(contador, BorderLayout.WEST);
        barra.add(botonActualizar, BorderLayout.EAST);

        panel.add(barra, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        cargarConsulta(tabla, contador, sql, etiquetaContador);
        return panel;
    }

    /** Ejecuta la consulta y vuelca columnas/filas dinamicamente en la tabla. */
    private void cargarConsulta(JTable tabla, JLabel contador, String sql, String etiquetaContador) {
        try {
            ResultadoTabla resultado = registroDao.consultaTabla(sql);
            DefaultTableModel modelo = new DefaultTableModel(resultado.getColumnas(), 0) {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };
            for (Object[] fila : resultado.getFilas()) {
                modelo.addRow(fila);
            }
            tabla.setModel(modelo);
            contador.setText(etiquetaContador + ": " + resultado.getFilas().size());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al consultar el reporte: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cerrarSesion() {
        dispose();
        new VentanaLogin().setVisible(true);
    }
}
