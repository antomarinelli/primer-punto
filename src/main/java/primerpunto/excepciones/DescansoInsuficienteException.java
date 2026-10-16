package primerpunto.excepciones;

/**
 * El jugador no tendría el descanso mínimo entre dos partidos. El organizador puede
 * programarlo igual si lo confirma (por ejemplo, si el jugador pidió jugar seguido).
 */
public class DescansoInsuficienteException extends PrimerPuntoException {

    public DescansoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
