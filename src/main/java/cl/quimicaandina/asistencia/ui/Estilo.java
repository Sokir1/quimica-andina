package cl.quimicaandina.asistencia.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;

/** Paleta y tipografia corporativa de Quimica Andina, mas helpers visuales. */
public final class Estilo {

    public static final Color FONDO = new Color(0xF4F6F8);
    public static final Color PRIMARIO = new Color(0x1F4E79);
    public static final Color VERDE = new Color(0x2E7D32);
    public static final Color ROJO = new Color(0xC62828);
    public static final Color TEXTO = new Color(0x2B2B2B);
    public static final Color FILA_ALTERNA = new Color(0xEDF2F7);
    public static final Color BLANCO = Color.WHITE;
    public static final Color BORDE = new Color(0xD9E0E7);
    public static final Color SELECCION = new Color(0xCFE0F1);

    private Estilo() {
    }

    public static Font fuente(int estilo, int tamano) {
        return new Font("Segoe UI", estilo, tamano);
    }

    public static Font fuenteBase() {
        return fuente(Font.PLAIN, 13);
    }

    public static Font fuenteTitulo() {
        return fuente(Font.BOLD, 20);
    }

    public static JButton boton(String texto, Color fondo) {
        return new BotonColor(texto, fondo);
    }

    public static JButton botonPrimario(String texto) {
        return boton(texto, PRIMARIO);
    }

    public static JButton botonVerde(String texto) {
        return boton(texto, VERDE);
    }

    public static JButton botonRojo(String texto) {
        return boton(texto, ROJO);
    }

    /** Panel blanco con borde suave para agrupar contenido. */
    public static JPanel tarjeta() {
        JPanel tarjeta = new JPanel();
        tarjeta.setBackground(BLANCO);
        tarjeta.setBorder(new LineBorder(BORDE, 1, true));
        return tarjeta;
    }

    /** Aplica tipografia, cabecera azul corporativo y filas alternadas a una tabla. */
    public static void estilizarTabla(JTable tabla) {
        tabla.setFont(fuenteBase());
        tabla.setRowHeight(26);
        tabla.setForeground(TEXTO);
        tabla.setBackground(BLANCO);
        tabla.setGridColor(new Color(0xE2E8F0));
        tabla.setSelectionBackground(SELECCION);
        tabla.setSelectionForeground(TEXTO);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getTableHeader().setPreferredSize(new Dimension(0, 36));
        tabla.getTableHeader().setDefaultRenderer(new CabeceraRenderer());
        tabla.setDefaultRenderer(Object.class, new FilasAlternadasRenderer());
    }

    /** Cabecera de tabla: fondo azul corporativo con texto blanco centrado. */
    private static class CabeceraRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccionado, boolean enfocado, int fila, int columna) {
            JLabel etiqueta = (JLabel) super.getTableCellRendererComponent(
                    tabla, valor, seleccionado, enfocado, fila, columna);
            etiqueta.setOpaque(true);
            etiqueta.setBackground(PRIMARIO);
            etiqueta.setForeground(BLANCO);
            etiqueta.setFont(fuente(Font.BOLD, 13));
            etiqueta.setHorizontalAlignment(CENTER);
            etiqueta.setBorder(new EmptyBorder(8, 8, 8, 8));
            return etiqueta;
        }
    }

    /** Filas alternadas blanco / #EDF2F7. */
    private static class FilasAlternadasRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor,
                boolean seleccionado, boolean enfocado, int fila, int columna) {
            Component componente = super.getTableCellRendererComponent(
                    tabla, valor, seleccionado, enfocado, fila, columna);
            if (seleccionado) {
                componente.setBackground(tabla.getSelectionBackground());
                componente.setForeground(tabla.getSelectionForeground());
            } else {
                componente.setBackground(fila % 2 == 0 ? BLANCO : FILA_ALTERNA);
                componente.setForeground(TEXTO);
            }
            return componente;
        }
    }
}
