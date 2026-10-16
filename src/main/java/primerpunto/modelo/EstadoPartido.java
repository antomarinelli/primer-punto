package primerpunto.modelo;

/** Situación de un partido dentro del torneo. */
public enum EstadoPartido {

    PENDIENTE("Sin programar"),
    PROGRAMADO("Programado"),
    FINALIZADO("Finalizado");

    private final String descripcion;

    EstadoPartido(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
