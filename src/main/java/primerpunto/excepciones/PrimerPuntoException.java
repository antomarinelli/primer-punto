package primerpunto.excepciones;

/**
 * Excepción base del sistema. Representa una situación del negocio que impide
 * completar una operación (por ejemplo, inscribir fuera de plazo).
 *
 * Todas las excepciones propias heredan de esta clase, de modo que la interfaz
 * puede capturarlas juntas y mostrarle al organizador el mensaje correspondiente.
 */
public class PrimerPuntoException extends Exception {

    public PrimerPuntoException(String mensaje) {
        super(mensaje);
    }

    public PrimerPuntoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
