package primerpunto.excepciones;

/**
 * No se pudo escribir un archivo exportado: la carpeta no existe, no hay permiso
 * o el archivo está abierto en otro programa (por ejemplo, en Excel).
 * Guarda como causa la IOException original.
 */
public class ArchivoException extends PrimerPuntoException {

    public ArchivoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
