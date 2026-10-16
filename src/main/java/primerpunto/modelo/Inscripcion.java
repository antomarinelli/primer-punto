package primerpunto.modelo;

import java.time.LocalDate;

/**
 * Inscripción de un participante (jugador o pareja) a un torneo, con su estado
 * de pago. La crea y la modifica el controlador GestionParticipantes.
 */
public class Inscripcion {

    private final int idInscripcion;
    private final Participante participante;
    private final LocalDate fecha;
    private boolean estadoPago;
    private EstadoInscripcion estadoInscripcion;
    private String motivoBaja;

    public Inscripcion(int idInscripcion, Participante participante, LocalDate fecha, boolean pagado) {
        if (participante == null || fecha == null) {
            throw new IllegalArgumentException("La inscripción requiere un participante y una fecha.");
        }
        this.idInscripcion = idInscripcion;
        this.participante = participante;
        this.fecha = fecha;
        setEstadoPago(pagado);
    }

    public int getIdInscripcion() {
        return idInscripcion;
    }

    public Participante getParticipante() {
        return participante;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public boolean isEstadoPago() {
        return estadoPago;
    }

    public EstadoInscripcion getEstadoInscripcion() {
        return estadoInscripcion;
    }

    public String getMotivoBaja() {
        return motivoBaja;
    }

    /** Al registrar el pago, una inscripción activa pasa de "Pago pendiente" a "Confirmada". */
    public void setEstadoPago(boolean estadoPago) {
        this.estadoPago = estadoPago;
        if (estadoInscripcion != EstadoInscripcion.BAJA) {
            estadoInscripcion = estadoPago ? EstadoInscripcion.CONFIRMADA : EstadoInscripcion.PENDIENTE;
        }
    }

    public void setEstadoInscripcion(EstadoInscripcion estadoInscripcion) {
        this.estadoInscripcion = estadoInscripcion;
    }

    public void setMotivoBaja(String motivoBaja) {
        this.motivoBaja = motivoBaja;
    }

    public boolean estaActiva() {
        return estadoInscripcion != EstadoInscripcion.BAJA;
    }

    /** Solo participa del torneo quien está activo y pagó la inscripción (RF08). */
    public boolean estaHabilitado() {
        return estaActiva() && estadoPago;
    }

    /** Hay deuda cuando la inscripción sigue activa y el pago no se registró (RF18). */
    public boolean tieneDeuda() {
        return estaActiva() && !estadoPago;
    }
}
