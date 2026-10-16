package primerpunto.excepciones;

/** Los datos ingresados están incompletos, tienen un formato incorrecto o no respetan una regla del negocio. */
public class DatoInvalidoException extends PrimerPuntoException {

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public DatoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
