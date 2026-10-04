package cl.quimicaandina.asistencia.ui;

import cl.quimicaandina.asistencia.modelo.Empleado;
import cl.quimicaandina.asistencia.servicio.ExcepcionNegocio;
import cl.quimicaandina.asistencia.servicio.ServicioAsistencia;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

/** RF-2: portal del trabajador con reloj en vivo y marcacion de entrada/salida. */
public class VentanaTrabajador extends JFrame {

    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", Locale.of("es", "CL"));

    private final Empleado empleado;
    private final ServicioAsistencia servicioAsistencia = new ServicioAsistencia();
    private final JLabel etiquetaReloj = new JLabel(" ", JLabel.CENTER);
    private final JLabel etiquetaFecha = new JLabel(" ", JLabel.CENTER);

    public VentanaTrabajador(Empleado empleado) {
        this.empleado = empleado;

        setTitle("Química Andina — Portal del Trabajador: " + empleado.getNombreCompleto());
        setSize(900, 560);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        add(construirCabecera(), BorderLayout.NORTH);
        add(construirCuerpo(), BorderLayout.CENTER);

        // RF-2: reloj visible actualizado cada segundo
        actualizarReloj();
        new Timer(1000, e -> actualizarReloj()).start();
    }

    private JPanel construirCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(Estilo.PRIMARIO);
        cabecera.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel titulo = new JLabel("Química Andina — Portal del Trabajador");
        titulo.setFont(Estilo.fuenteTitulo());
        titulo.setForeground(Estilo.BLANCO);
        cabecera.add(titulo, BorderLayout.WEST);

        JButton botonCerrarSesion = Estilo.boton("Cerrar sesión", new Color(0x16395C));
        botonCerrarSesion.addActionListener(e -> cerrarSesion());
        cabecera.add(botonCerrarSesion, BorderLayout.EAST);
        return cabecera;
    }

    private JPanel construirCuerpo() {
        JPanel cuerpo = new JPanel(new GridBagLayout());
        cuerpo.setBackground(Estilo.FONDO);

        JPanel tarjeta = Estilo.tarjeta();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(28, 48, 32, 48));

        JLabel bienvenida = new JLabel("Bienvenido(a), " + empleado.getNombreCompleto());
        bienvenida.setFont(Estilo.fuente(Font.BOLD, 18));
        bienvenida.setForeground(Estilo.TEXTO);
        bienvenida.setAlignmentX(CENTER_ALIGNMENT);

        etiquetaReloj.setFont(new Font("Segoe UI", Font.BOLD, 34));
        etiquetaReloj.setForeground(Estilo.PRIMARIO);
        etiquetaReloj.setAlignmentX(CENTER_ALIGNMENT);

        etiquetaFecha.setFont(Estilo.fuente(Font.PLAIN, 15));
        etiquetaFecha.setForeground(Estilo.TEXTO);
        etiquetaFecha.setAlignmentX(CENTER_ALIGNMENT);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        panelBotones.setOpaque(false);

        JButton botonEntrada = Estilo.botonVerde("MARCAR ENTRADA");
        botonEntrada.setFont(Estilo.fuente(Font.BOLD, 15));
        botonEntrada.addActionListener(e -> marcar(true));
        JButton botonSalida = Estilo.botonRojo("MARCAR SALIDA");
        botonSalida.setFont(Estilo.fuente(Font.BOLD, 15));
        botonSalida.addActionListener(e -> marcar(false));

        panelBotones.add(botonEntrada);
        panelBotones.add(Box.createHorizontalStrut(10));
        panelBotones.add(botonSalida);

        tarjeta.add(bienvenida);
        tarjeta.add(javax.swing.Box.createVerticalStrut(18));
        tarjeta.add(etiquetaReloj);
        tarjeta.add(javax.swing.Box.createVerticalStrut(6));
        tarjeta.add(etiquetaFecha);
        tarjeta.add(javax.swing.Box.createVerticalStrut(26));
        tarjeta.add(panelBotones);

        cuerpo.add(tarjeta, new GridBagConstraints());
        return cuerpo;
    }

    private void actualizarReloj() {
        LocalDateTime ahora = LocalDateTime.now();
        etiquetaReloj.setText(ahora.format(FORMATO_HORA));
        String fecha = ahora.format(FORMATO_FECHA);
        etiquetaFecha.setText(fecha.substring(0, 1).toUpperCase()
                + fecha.substring(1) + "  —  Hora del sistema (en vivo)");
    }

    /** V1..V7 se validan en ServicioAsistencia; aqui solo informamos el resultado. */
    private void marcar(boolean esEntrada) {
        try {
            String mensaje = esEntrada
                    ? servicioAsistencia.marcarEntrada(empleado.getIdEmpleado())
                    : servicioAsistencia.marcarSalida(empleado.getIdEmpleado());
            JOptionPane.showMessageDialog(this, mensaje,
                    "Marca registrada", JOptionPane.INFORMATION_MESSAGE);
        } catch (ExcepcionNegocio e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Marcación no permitida", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al comunicarse con la base de datos: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cerrarSesion() {
        dispose();
        new VentanaLogin().setVisible(true);
    }
}
