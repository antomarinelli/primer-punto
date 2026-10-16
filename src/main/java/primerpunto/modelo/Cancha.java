package primerpunto.modelo;

import java.time.LocalTime;

/** Cancha del club, con su horario de funcionamiento. */
public class Cancha {

    private final int idCancha;
    private int numero;
    private boolean disponibilidad;
    private LocalTime horaApertura;
    private LocalTime horaCierre;

    public Cancha(int idCancha, int numero, LocalTime horaApertura, LocalTime horaCierre) {
        this.idCancha = idCancha;
        this.disponibilidad = true;
        setDatos(numero, horaApertura, horaCierre);
    }

    public int getIdCancha() {
        return idCancha;
    }

    public int getNumero() {
        return numero;
    }

    public boolean getDisponibilidad() {
        return disponibilidad;
    }

    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    public LocalTime getHoraCierre() {
        return horaCierre;
    }

    /** Permite inhabilitar una cancha temporalmente (por ejemplo, por lluvia o mantenimiento). */
    public void setDisponibilidad(boolean disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    /** Actualiza el número y el horario de la cancha (CU02, flujo alternativo: modificar cancha). */
    public final void setDatos(int numero, LocalTime horaApertura, LocalTime horaCierre) {
        if (numero <= 0) {
            throw new IllegalArgumentException("El número de cancha debe ser mayor a cero.");
        }
        if (horaApertura == null || horaCierre == null || !horaCierre.isAfter(horaApertura)) {
            throw new IllegalArgumentException("La hora de cierre debe ser posterior a la hora de apertura.");
        }
        this.numero = numero;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
    }

    /**
     * Indica si un partido que empieza a la hora indicada y dura los minutos indicados
     * entra en el horario de funcionamiento de la cancha.
     */
    public boolean estaDisponible(LocalTime horario, int duracionMinutos) {
        return disponibilidad
                && !horario.isBefore(horaApertura)
                && !horario.plusMinutes(duracionMinutos).isAfter(horaCierre)
                && horario.plusMinutes(duracionMinutos).isAfter(horario); // no pasa de medianoche
    }

    @Override
    public String toString() {
        return "Cancha " + numero;
    }
}
