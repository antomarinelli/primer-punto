package primerpunto.modelo;

/** Situación de una inscripción según el pago y las bajas. */
public enum EstadoInscripcion {

    CONFIRMADA("Confirmada"),
    PENDIENTE("Pago pendiente"),
    BAJA("Baja");

    private final String descripcion;

    EstadoInscripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
