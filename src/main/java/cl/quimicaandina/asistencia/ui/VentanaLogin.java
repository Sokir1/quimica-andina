package cl.quimicaandina.asistencia.ui;

import cl.quimicaandina.asistencia.modelo.Empleado;
import cl.quimicaandina.asistencia.servicio.ExcepcionNegocio;
import cl.quimicaandina.asistencia.servicio.ServicioUsuarios;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** RF-1: ventana de inicio de sesion con validacion contra la base de datos. */
public class VentanaLogin extends JFrame {

    private final ServicioUsuarios servicioUsuarios = new ServicioUsuarios();
    private final JTextField campoCorreo = new JTextField(20);
    private final JPasswordField campoClave = new JPasswordField(20);
    private final JButton botonEntrar = Estilo.botonPrimario("Entrar");

    public VentanaLogin() {
        setTitle("Química Andina — Inicio de Sesión");
        setSize(420, 320);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel contenido = new JPanel(new GridBagLayout());
        contenido.setBackground(Estilo.FONDO);
        setContentPane(contenido);

        JPanel tarjeta = Estilo.tarjeta();
        tarjeta.setLayout(new GridBagLayout());
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.insets = new Insets(6, 14, 6, 14);
        restricciones.anchor = GridBagConstraints.WEST;

        JLabel etiquetaTitulo = new JLabel("Control de Asistencia");
        etiquetaTitulo.setFont(Estilo.fuenteTitulo());
        etiquetaTitulo.setForeground(Estilo.PRIMARIO);
        JLabel etiquetaSubtitulo = new JLabel("Química Andina Ltda.");
        etiquetaSubtitulo.setFont(Estilo.fuente(Font.PLAIN, 13));
        etiquetaSubtitulo.setForeground(Estilo.TEXTO);

        int fila = 0;
        restricciones.gridx = 0;
        restricciones.gridy = fila++;
        restricciones.gridwidth = 2;
        tarjeta.add(etiquetaTitulo, restricciones);
        restricciones.gridy = fila++;
        tarjeta.add(etiquetaSubtitulo, restricciones);

        restricciones.gridwidth = 1;
        restricciones.gridy = fila++;
        restricciones.gridx = 0;
        tarjeta.add(new JLabel("Correo:"), restricciones);
        restricciones.gridx = 1;
        campoCorreo.setFont(Estilo.fuenteBase());
        tarjeta.add(campoCorreo, restricciones);

        restricciones.gridy = fila++;
        restricciones.gridx = 0;
        tarjeta.add(new JLabel("Clave:"), restricciones);
        restricciones.gridx = 1;
        campoClave.setFont(Estilo.fuenteBase());
        tarjeta.add(campoClave, restricciones);

        restricciones.gridy = fila++;
        restricciones.gridx = 0;
        restricciones.gridwidth = 2;
        restricciones.fill = GridBagConstraints.HORIZONTAL;
        botonEntrar.addActionListener(e -> intentarIniciarSesion());
        tarjeta.add(botonEntrar, restricciones);

        GridBagConstraints externas = new GridBagConstraints();
        externas.insets = new Insets(10, 10, 10, 10);
        tarjeta.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 8));
        contenido.add(tarjeta, externas);

        getRootPane().setDefaultButton(botonEntrar);
    }

    /** Autentica y abre la ventana que corresponda segun el rol del usuario. */
    private void intentarIniciarSesion() {
        try {
            Empleado empleado = servicioUsuarios.autenticar(
                    campoCorreo.getText(), new String(campoClave.getPassword()));
            dispose();
            SwingUtilities.invokeLater(() -> {
                if ("ADMINISTRADOR".equals(empleado.getRol())) {
                    new VentanaAdmin(empleado).setVisible(true);
                } else {
                    new VentanaTrabajador(empleado).setVisible(true);
                }
            });
        } catch (ExcepcionNegocio e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Inicio de sesión", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    "No fue posible conectar con la base de datos: " + e.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }
}
