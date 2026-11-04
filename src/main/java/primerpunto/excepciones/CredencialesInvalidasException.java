package primerpunto.excepciones;

/**
 * El usuario o la contraseña ingresados no son correctos (RNF08). El mensaje no
 * dice cuál de los dos falló, para no dar pistas a quien intente adivinarlos.
 */
public class CredencialesInvalidasException extends PrimerPuntoException {

    public CredencialesInvalidasException() {
        super("Usuario o contraseña incorrectos.");
    }
}
