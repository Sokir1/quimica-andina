package cl.quimicaandina.asistencia.modelo;

import java.time.LocalTime;

/** Turno laboral parametrizado en base de datos (hora de entrada y salida). */
public class Turno {

    private int idTurno;
    private String nombreTurno;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;

    public Turno() {
    }

    public Turno(int idTurno, String nombreTurno, LocalTime horaEntrada, LocalTime horaSalida) {
        this.idTurno = idTurno;
        this.nombreTurno = nombreTurno;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
    }

    public int getIdTurno() {
        return idTurno;
    }

    public void setIdTurno(int idTurno) {
        this.idTurno = idTurno;
    }

    public String getNombreTurno() {
        return nombreTurno;
    }

    public void setNombreTurno(String nombreTurno) {
        this.nombreTurno = nombreTurno;
    }

    public LocalTime getHoraEntrada() {
        return horaEntrada;
    }

    public void setHoraEntrada(LocalTime horaEntrada) {
        this.horaEntrada = horaEntrada;
    }

    public LocalTime getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(LocalTime horaSalida) {
        this.horaSalida = horaSalida;
    }

    @Override
    public String toString() {
        return nombreTurno + " (" + horaEntrada + " a " + horaSalida + ")";
    }
}
