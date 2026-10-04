package cl.quimicaandina.asistencia.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.JButton;
import javax.swing.BorderFactory;

/**
 * Boton con fondo plano de color corporativo. Se pinta manualmente para que
 * el color se respete incluso cuando el LookAndFeel es Nimbus.
 */
public class BotonColor extends JButton {

    private final Color colorFondo;

    public BotonColor(String texto, Color colorFondo) {
        super(texto);
        this.colorFondo = colorFondo;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setBackground(colorFondo);
        setForeground(Estilo.BLANCO);
        setFont(Estilo.fuente(Font.BOLD, 13));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color pintura = getModel().isPressed() ? colorFondo.darker() : colorFondo;
        g.setColor(pintura);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
