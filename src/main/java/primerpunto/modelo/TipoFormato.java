package primerpunto.modelo;

/** Opciones de formato que el organizador puede elegir al generar la competencia (CU10). */
public enum TipoFormato {

    CUADRO("Cuadro de eliminación directa"),
    ZONAS("Zonas de todos contra todos"),
    ZONAS_Y_CUADRO("Zonas y luego cuadro");

    private final String descripcion;

    TipoFormato(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
