package cl.quimicaandina.asistencia;

import cl.quimicaandina.asistencia.ui.VentanaLogin;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Punto de entrada de la aplicacion: abre la ventana de login. */
public class Principal {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignorada) {
            // Si Nimbus no esta disponible se usa el LookAndFeel por defecto
        }
        SwingUtilities.invokeLater(() -> new VentanaLogin().setVisible(true));
    }
}
