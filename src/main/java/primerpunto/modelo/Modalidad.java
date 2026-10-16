package primerpunto.modelo;

/** Formato de participación de un torneo. */
public enum Modalidad {

    SINGLE("Single", false),
    DOBLES("Dobles", true),
    MIXTO("Mixto", true);

    private final String descripcion;
    private final boolean requierePareja;

    Modalidad(String descripcion, boolean requierePareja) {
        this.descripcion = descripcion;
        this.requierePareja = requierePareja;
    }

    /** Dobles y Mixto se juegan en pareja; Single, de forma individual. */
    public boolean requierePareja() {
        return requierePareja;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
