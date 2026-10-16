package primerpunto.modelo;

/** Etapas por las que pasa un torneo, desde su creación hasta su finalización. */
public enum EstadoTorneo {

    INSCRIPCION_ABIERTA("Inscripción abierta"),
    INSCRIPCION_CERRADA("Inscripción cerrada"),
    EN_CURSO("En curso"),
    FINALIZADO("Finalizado");

    private final String descripcion;

    EstadoTorneo(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
